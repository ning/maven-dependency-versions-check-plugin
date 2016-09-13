/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.maven.plugins.dependencyversionscheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.maven.plugins.dependencyversionscheck.strategy.Strategy;
import com.ning.maven.plugins.dependencyversionscheck.strategy.StrategyProvider;
import com.ning.maven.plugins.dependencyversionscheck.util.ArtifactOptionalFilter;
import com.ning.maven.plugins.dependencyversionscheck.util.ArtifactScopeFilter;
import com.ning.maven.plugins.dependencyversionscheck.version.Version;
import com.ning.maven.plugins.dependencyversionscheck.version.VersionResolution;
import com.pyx4j.log4j.MavenLogAppender;

/**
 * Base code for all the mojos. Contains the dependency resolvers and the common options.
 */
public abstract class AbstractDependencyVersionsMojo extends AbstractMojo
{
    /**
     * The maven project (effective pom).
     * @parameter expression="${project}"
     * @required
     * @readonly
    */
    protected MavenProject project;

    /**
     * For creating MavenProject objects.
     * @component
     */
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
     * The artifact factory.
     * @component
     */
    protected ArtifactFactory artifactFactory;

    /**
     * Resolves artifacts.
     * @component
     */
    protected ArtifactResolver artifactResolver;

    /**
     * The strategy provider. This can be requested by other pieces to add
     * additional strategies.
     *
     * @component
     */
    protected StrategyProvider strategyProvider;

    /**
     * For resolving versions.
     * @component
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * The local repo for the project if defined;
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * @component
     * @required
     * @readonly
     */
    protected DependencyTreeBuilder treeBuilder;

    /**
     * @component
     * @required
     * @readonly
     */
    protected ArtifactCollector artifactCollector;

    /**
     * Remote repositories.
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    protected List remoteRepositories;

    /**
     * A set of artifacts with expected and resolved versions that are to be except from the check.
     * @parameter alias="exceptions"
     */
    protected VersionCheckExcludes[] exceptions;

    /**
     * Skip the plugin execution.
     *
     * <pre>
     *   <configuration>
     *     <skip>true</skip>
     *   </configuration>
     * </pre>
     *
     * @parameter default-value="false"
     */
    protected boolean skip = false;

    /**
     * Whether to warn if the resolved major version is higher then the expected one of the project or one of the depedencies.
     * @parameter default-value="false"
     */
    protected boolean warnIfMajorVersionIsHigher;

    /**
     * Resolvers to resolve versions and compare existing things.
     *
     * <pre>
     *   <resolvers>
     *     <resolver>
     *       <id>apache-stuff</id>
     *       <strategy>APR</strategy>
     *       <includes>
     *         <include>commons-configuration:commons-configuration</include>
     *         <include>commons-lang:commons-lang</include>
     *       </includes>
     *     </resolver>
     *   </resolver>
     * </pre>
     *
     * @parameter alias="resolvers"
     */
    protected ResolverDefinition [] resolvers;

    /**
     * Sets the default strategy.
     *
     * @parameter alias="defaultStrategy" default-value="default"
     *
     */
    protected String defaultStrategy = "default";

    /** Lists all available scopes for transitive dependency resolution. */
    protected static final Map TRANSITIVE_SCOPES;

    /** Lists all visible scopes when doing dependency resolution. */
    protected static final Map VISIBLE_SCOPES;

    static {
        final Map transitiveScopes = new HashMap();
        // Map from the scope to test to the scopes that show up in this scope from a transitive dep. null value is for "all available scopes".
        transitiveScopes.put(Artifact.SCOPE_COMPILE, new String [] { Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM });
        transitiveScopes.put(Artifact.SCOPE_TEST,    new String [] { Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME });
        transitiveScopes.put(Artifact.SCOPE_RUNTIME, new String [] { Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME });
        transitiveScopes.put(null ,                  new String [] { Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME });
        TRANSITIVE_SCOPES = Collections.unmodifiableMap(transitiveScopes);

        // Map from the scope to the scopes that are visible on the classpath for that scope. null value is for "all available scopes".
        final Map visibleScopes = new HashMap();
        visibleScopes.put(Artifact.SCOPE_COMPILE, new String [] { Artifact.SCOPE_COMPILE,  Artifact.SCOPE_PROVIDED, Artifact.SCOPE_SYSTEM });
        visibleScopes.put(Artifact.SCOPE_TEST,    new String [] { Artifact.SCOPE_COMPILE,  Artifact.SCOPE_PROVIDED, Artifact.SCOPE_SYSTEM,                         Artifact.SCOPE_TEST });
        visibleScopes.put(Artifact.SCOPE_RUNTIME, new String [] { Artifact.SCOPE_COMPILE,                           Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME });
        visibleScopes.put(null,                   new String [] { Artifact.SCOPE_COMPILE,  Artifact.SCOPE_PROVIDED, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME, Artifact.SCOPE_TEST });
        VISIBLE_SCOPES = Collections.unmodifiableMap(visibleScopes);
    }

    protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

    /** ArtifactName to VersionStrategy. Filled in loadResolvers(). */
    protected final Map resolverMap = new HashMap();

    /** Artifact pattern to VersionStrategy. Filled in loadResolvers(). */
    protected final Map resolverPatternMap = new HashMap();

    /** Qualified artifact name to artifact. */
    protected final Map resolvedDependenciesByName = new HashMap();

    protected Strategy defaultStrategyType;

    /** Keeps track of the longest name for an artifact for printing out nicely. */
    protected int maxLen = -1;

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 5, new ThreadFactory() {
        final AtomicInteger threadCount = new AtomicInteger();

        public Thread newThread(Runnable r) {
            Thread th = new Thread(r, "dependency-version-check worker #" + threadCount.getAndIncrement());
            th.setDaemon(true);
            return th;
        }

        ;
    });

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        MavenLogAppender.startPluginLog(this);

        try {
            if (skip) {
                LOG.debug("Skipping execution!");
            }
            else {
                checkExceptions();

                final DependencyNode node = treeBuilder.buildDependencyTree(project, localRepository, artifactFactory, artifactMetadataSource, null, artifactCollector);

                for (final Iterator dependencyIt = node.iterator(); dependencyIt.hasNext(); ) {
                    final DependencyNode dependency = (DependencyNode) dependencyIt.next();
                    if (dependency.getState() == DependencyNode.INCLUDED) {
                        final Artifact artifact = dependency.getArtifact();
                        resolvedDependenciesByName.put(getQualifiedName(artifact), artifact);
                    }
                }

                loadResolvers(resolvers);

                defaultStrategyType = strategyProvider.forName(defaultStrategy);
                if (defaultStrategyType == null) {
                    throw new MojoExecutionException("Could not locate default strategy '" + defaultStrategy + "'!");
                }

                LOG.debug("Starting {} mojo run!", this.getClass().getSimpleName());
                doExecute();
            }
        }
        catch (MojoExecutionException me) {
            throw me;
        }
        catch (MojoFailureException mfe) {
            throw mfe;
        }
        catch (Exception e) {
            throw new MojoExecutionException("While running mojo: ", e);
        }
        finally {
            LOG.debug("Ended {} mojo run!", this.getClass().getSimpleName());
            MavenLogAppender.endPluginLog(this);
        }
    }

    /**
     * Subclasses need to implement this method.
     */
    protected abstract void doExecute() throws Exception;

    /**
     * Loads all resolver definitions and turns them into either direct resolved strategies or patterns to check against.
     */
    private void loadResolvers(final ResolverDefinition [] resolvers)
    {
        if (!ArrayUtils.isEmpty(resolvers)) {
            for (int i = 0; i < resolvers.length; i++) {
                ResolverDefinition r = resolvers[i];

                final Strategy strategy = strategyProvider.forName(r.getStrategyName());
                if (strategy == null) {
                    LOG.warn("Could not locate Strategy {}! Check for typos!", r.getStrategyName());
                }
                else {
                    final String [] includes = r.getIncludes();
                    if (!ArrayUtils.isEmpty(includes)) {
                        for (int j = 0; j < includes.length; j++) {
                            final Strategy oldStrategy = (Strategy) resolverMap.get(includes[j]);
                            if (oldStrategy != null) {
                                LOG.warn("A strategy for {} was already defined: {}", includes[j], oldStrategy.getName());
                            }
                            if (includes[j].contains("*")) {
                                // Poor mans regexp escape. Escapes all "." and turns "*" into ".*". Should be good enough
                                // for most use cases. Pattern.quote() only adds \\Q and \\E to the string and does not metachar
                                // escaping, so it is useless.
                                final String pattern = includes[j].replace(".", "\\.").replace("*", ".*");
                                resolverPatternMap.put(pattern, strategy);
                            }
                            else {
                                resolverMap.put(includes[j], strategy);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Locate the version strategy for a given version resolution. This will try
     * to locate a direct match and also do wildcard match on "group only" and "group and artifact".
     */
    private Strategy findStrategy(final VersionResolution resolution)
    {
        final String dependencyName = resolution.getDependencyName();
        Strategy strategy = (Strategy) resolverMap.get(dependencyName);
        if (strategy != null) {
            LOG.debug("Found direct match: {}", strategy.getName());
            return strategy;
        }

        // No direct hit. Try just the group
        final String [] elements = StringUtils.split(dependencyName, ":");

        if (elements.length == 2) {
            strategy = (Strategy) resolverMap.get(elements[0]);

            if (strategy != null) {
                LOG.debug("Found group ({}) match: {}", elements[0], strategy.getName());

                resolverMap.put(dependencyName, strategy);
                return strategy;
            }

            // Try the wildcards
            for (Iterator it = resolverPatternMap.entrySet().iterator(); it.hasNext(); ) {
                final Map.Entry entry = (Map.Entry) it.next();
                final String pattern = (String) entry.getKey();
                final String patternElements [] = StringUtils.split(pattern, ":");

                if (Pattern.matches(patternElements[0], elements[0])) {
                    // group wildcard match.
                    if (patternElements.length == 1) {
                        strategy = (Strategy) entry.getValue();
                        LOG.debug("Found pattern match ({}) on group ({}) match: {}", new Object [] {patternElements[0], elements[0], strategy.getName() });

                        resolverMap.put(dependencyName, strategy);
                        return strategy;
                    }
                    // group and artifact wildcard match.
                    else if (Pattern.matches(patternElements[1], elements[1])) {
                        strategy = (Strategy) entry.getValue();
                        LOG.debug("Found regexp match ({}) on ({}) match: {}", new Object [] {pattern, dependencyName, strategy.getName() });

                        resolverMap.put(dependencyName, strategy);
                        return strategy;
                    }
                }
            }
        }

        strategy = defaultStrategyType;
        resolverMap.put(dependencyName, strategy);
        LOG.debug("Using default strategy for {} match: {}", dependencyName, strategy.getName());
        return strategy;
    }

    /**
     * Creates a map of all version resolutions used in this project in a given scope. The result is a map from artifactName to a list of version numbers used in the project, based on the element requesting
     * the version.
     *
     * If the special scope "null" is used, a superset of all scopes is used (this is used by the check mojo).
     */
    protected Map buildResolutionMap(final String scope) throws MojoExecutionException, InvalidDependencyVersionException, ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException
    {
        final String [] visibleScopes = (String []) VISIBLE_SCOPES.get(scope);
        final String [] transitiveScopes = (String []) TRANSITIVE_SCOPES.get(scope);

        if (visibleScopes == null) {
            throw new MojoExecutionException("No valid scopes found for '" + scope + "'");
        }

        // Map from artifactName --> list of resolutions found on the tree
        final SortedMap resolutionMap = new TreeMap();
        final List errors = new ArrayList();
        final CountDownLatch latch = new CountDownLatch(project.getDependencies().size());
        boolean useParallelDepResolution = "true".equalsIgnoreCase(getPropertyOrDefault("dependency.version.check.useParallelDepResolution", "true"));
        LOG.info("Using parallel dependency resolution: " + useParallelDepResolution);

        for (final Iterator iter = project.getDependencies().iterator(); iter.hasNext();) {
            final Dependency dependency = (Dependency) iter.next();

            if (useParallelDepResolution) {
                executorService.submit(new Runnable() {
                    public void run() {
                        try {
                            updateResolutionMapForDep(visibleScopes, transitiveScopes, resolutionMap, dependency);
                        } catch (Throwable throwable) {
                            errors.add(throwable);
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            } else {
                updateResolutionMapForDep(visibleScopes, transitiveScopes, resolutionMap, dependency);
            }
        }
        if (useParallelDepResolution) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (errors.size() > 0) {
            throw new RuntimeException((Throwable) errors.get(0));
        }
        return resolutionMap;
    }

    private String getPropertyOrDefault(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }
        return value;
    }

    private void updateResolutionMapForDep(String[] visibleScopes, String[] transitiveScopes, SortedMap resolutionMap, Dependency dependency) throws InvalidDependencyVersionException, ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException {
        LOG.debug("Checking direct dependency {}...", dependency);
        if (!isVisible(dependency.getScope(), visibleScopes)) {
            LOG.debug("... in invisible scope, ignoring!");
            return;
        }

        LOG.debug("... visible, resolving");

        // Dependency is visible, now resolve it.
        final String artifactName = getQualifiedName(dependency);

        if (artifactName.length() > maxLen) {
            maxLen = artifactName.length();
        }

        final Artifact resolvedArtifact = (Artifact) resolvedDependenciesByName.get(artifactName);

        if (resolvedArtifact == null) {
            // This is a potential problem because it should not be possible that a dependency that is required
            // by the project is not in the list of resolved dependencies.
            LOG.warn("No artifact available for '{}' (probably a multi-module child artifact).", artifactName);
        }
        else {
            final VersionResolution resolution = resolveVersion(dependency, resolvedArtifact, artifactName, true);
            addToResolutionMap(resolutionMap, resolution);

            if (!ArrayUtils.isEmpty(transitiveScopes)) {

                final ArtifactFilter scopeFilter = new ArtifactScopeFilter(transitiveScopes);

                // List of VersionResolution objects.
                List transitiveDependencies = null;

                try {
                    transitiveDependencies = resolveTransitiveVersions(dependency, resolvedArtifact, artifactName, scopeFilter);
                }
                catch (MultipleArtifactsNotFoundException ex) {
                    logArtifactResolutionException(ex);
                    transitiveDependencies = resolveTransitiveVersions(dependency, ex.getResolvedArtifacts(), artifactName, scopeFilter);
                }
                catch (AbstractArtifactResolutionException ex) {
                    logArtifactResolutionException(ex);
                }

                if (transitiveDependencies != null) {
                    LOG.debug("Artifact {} contributes {}", artifactName, transitiveDependencies);
                    for (Iterator transitiveIt = transitiveDependencies.iterator(); transitiveIt.hasNext(); )
                    {
                        final VersionResolution versionResolution = (VersionResolution) transitiveIt.next();
                        addToResolutionMap(resolutionMap, versionResolution);
                    }
                }
            }
        }
    }

    /**
     * Returns true if a given scope is available in the list of scopes.
     */
    private boolean isVisible(final String scope, final String [] validScopes)
    {
        for (int i = 0; i < validScopes.length; i++) {
            if (scope.equals(validScopes[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convenience method for a multi map add. Also makes sure that all the actual versions line up.
     */
    private void addToResolutionMap(final Map resolutionMap, final VersionResolution resolution)
    {
        synchronized (resolution.getDependencyName().intern()) {
            List resolutions = (List) resolutionMap.get(resolution.getDependencyName());
            if (resolutions == null) {
                resolutions = new ArrayList();
                resolutionMap.put(resolution.getDependencyName(), resolutions);
            }

            for (Iterator it = resolutions.iterator(); it.hasNext(); ) {
                final VersionResolution existingResolution = (VersionResolution) it.next();
                // TODO: It might be reasonable to fail the build in this case. However, I have yet to see
                // this message... :-)
                if (!existingResolution.getActualVersion().equals(resolution.getActualVersion())) {
                    LOG.warn("Dependency '{} expects version '{}' but '{}' already resolved to '{}'!",
                            new Object[]{resolution.getDependencyName(), resolution.getActualVersion(), existingResolution.getDependencyName(), existingResolution.getActualVersion()});
                }
            }
            LOG.debug("Adding resolution: {}", resolution);
            resolutions.add(resolution);
        }
    }

    /**
     * Create a version resolution for the given dependency and artifact.
     */
    private VersionResolution resolveVersion(Dependency dependency, Artifact artifact, String artifactName, final boolean directArtifact)
    {
        VersionResolution resolution = null;

        try {
            // Build a version from the artifact that was resolved.
            ArtifactVersion resolvedVersion = artifact.getSelectedVersion();

            if (resolvedVersion == null) {
                resolvedVersion = new DefaultArtifactVersion(artifact.getVersion());
            }

            // versionRange represents the versions that will satisfy the dependency.
            VersionRange versionRange = VersionRange.createFromVersionSpec(dependency.getVersion());
            // expectedVersion is the version declared in the dependency.
            ArtifactVersion expectedVersion = versionRange.getRecommendedVersion();

            if (expectedVersion == null) {
                // Fall back to the artifact version if it fits.
                if (versionRange.containsVersion(resolvedVersion)) {
                    expectedVersion = resolvedVersion;
                }
                else {
                    LOG.error("Cannot determine the recommended version of dependency '{}'; its version specification is '{}', and the resolved version is '{}'.", new Object [] { artifactName, dependency.getVersion(), resolvedVersion.toString() });
                    return null;
                }
            }

            // Build internal versions
            final Version resolvedVersionObj = new Version(resolvedVersion.toString());
            final Version depVersionObj      = new Version(versionRange.toString(), expectedVersion.toString());

            resolution = new VersionResolution(artifactName, artifactName, depVersionObj, resolvedVersionObj, directArtifact);

            if (!isExcluded(artifact, depVersionObj, resolvedVersionObj)) {
                final Strategy strategy = findStrategy(resolution);

                if (!(versionRange.containsVersion(resolvedVersion) && strategy.isCompatible(resolvedVersionObj, depVersionObj))) {
                    resolution.setConflict(true);
                }
            }
        }
        catch (InvalidVersionSpecificationException ex) {
            LOG.warn("Could not parse the version specification of an artifact", ex);
        }
        catch (OverConstrainedVersionException ex) {
            LOG.warn("Could not resolve an artifact", ex);
        }
        return resolution;
    }

    /**
     * Resolve all transitive dependencies relative to a given dependency, based off the artifact given. A scope filter can be added which limits the
     * results to the scopes present in that filter.
     */
    private List resolveTransitiveVersions(final Dependency dependency, final Artifact artifact, final String artifactName, final ArtifactFilter scopeFilter)
        throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException
    {
        ArtifactFilter exclusionFilter = null;

        if (!CollectionUtils.isEmpty(dependency.getExclusions())) {
            final List exclusions = new ArrayList();
            for (Iterator j = dependency.getExclusions().iterator(); j.hasNext();) {
                final Exclusion e = (Exclusion) j.next();
                exclusions.add( e.getGroupId() + ":" + e.getArtifactId() );
            }

            exclusionFilter = new ExcludesArtifactFilter(exclusions);
            LOG.debug("Built Exclusion Filter: {}", exclusions);
        }

        final ArtifactFilter filter;
        if (exclusionFilter != null) {
            AndArtifactFilter andFilter = new AndArtifactFilter();
            andFilter.add(exclusionFilter);
            andFilter.add(scopeFilter);
            filter = andFilter;
        }
        else {
            filter = scopeFilter;
        }

        final Collection dependenciesToCheck = resolveDependenciesInItsOwnScope(artifact, filter);

        return resolveTransitiveVersions(dependency, dependenciesToCheck, artifactName, scopeFilter);
    }

    /**
     * Resolve all transitive dependencies relative to a given dependency, based off the list of artifacts given. A scope filter can be added which limits the
     * results to the scopes present in that filter.
     */
    private List resolveTransitiveVersions(final Dependency dependency, final Collection dependenciesToCheck, final String artifactName, final ArtifactFilter scopeFilter)
        throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException
    {
        final List resolutions = new ArrayList();

        for (Iterator dependenciesToCheckIter = dependenciesToCheck.iterator(); dependenciesToCheckIter.hasNext();) {
            Artifact dependencyArtifactToCheck = (Artifact)dependenciesToCheckIter.next();
            LOG.debug("Checking {}...", dependencyArtifactToCheck);
            if (!scopeFilter.include(dependencyArtifactToCheck)) {
                LOG.debug("... in invisible scope, ignoring!");
                continue; // for
            }

            LOG.debug("... visible ...");

            if (dependencyArtifactToCheck.isOptional()) {
                LOG.debug("... but optional, ignoring!");
                continue;
            }

            LOG.debug("... resolving!");

            String artifactToCheckName = getQualifiedName(dependencyArtifactToCheck);

            if (artifactToCheckName.length() > maxLen) {
                maxLen = artifactToCheckName.length();
            }

            Artifact resolvedDependency = (Artifact) resolvedDependenciesByName.get(artifactToCheckName);

            if (resolvedDependency == null) {
                LOG.debug("Dependency {}:{} of artifact {} is no longer used in the current project.", new Object [] { artifactToCheckName, dependencyArtifactToCheck.getVersion(), artifactName });
            }
            else {
                // if the artifact in question is excluded in the current pom, then we don't have to worry about it anyways
                // this should be in the resolver. CHECKME! if (!exclusions.contains(dependencyArtifactToCheck.getGroupId() + ":" + dependencyArtifactToCheck.getArtifactId())) {
                final Version resolvedVersion = getVersion(resolvedDependency);
                final Version versionToCheck  = getVersion(dependencyArtifactToCheck);

                final VersionResolution resolution = new VersionResolution(artifactName, artifactToCheckName, versionToCheck, resolvedVersion, false);

                resolutions.add(resolution);

                // we have an error if
                // - if resolved dependency has a lower version or different qualifier than the stated one of the current transitive dependency
                // - if resolver dependency has a higher major version than the stated one of the current transitive dependency and
                //   there is no explicit dependency to that major version in the current project
                // for this last check, we assume that explicit dependencies have already been checked against actual ones, so we only need to check
                // if the artifact is an explicit dependency

                final Strategy strategy = findStrategy(resolution);
                if (!isExcluded(resolvedDependency, versionToCheck, resolvedVersion)) {
                    if (!strategy.isCompatible(resolvedVersion, versionToCheck)) {
                        resolution.setConflict(true);
                    }
                }
                else if (warnIfMajorVersionIsHigher && !strategy.isCompatible(resolvedVersion, versionToCheck)) {
                    LOG.warn("Artifact {} depends on {} at an incompatible version ({}) than the current project ({})!", new Object [] { artifactName, artifactToCheckName, dependencyArtifactToCheck.getVersion(), resolvedDependency.getVersion() });
                }
            }
        }

        return resolutions;
    }

    /**
     * Makes sure that all the exclusions are valid. They are called "Exception" for historical reasons.
     */
    private void checkExceptions()
        throws MojoExecutionException
    {
        if (!ArrayUtils.isEmpty(exceptions)) {
            for (int idx = 0; idx < exceptions.length; idx++) {
                if (exceptions[idx].check()) {
                    LOG.info("Adding exclusion '{}'", exceptions[idx]);
                }
                else {
                    throw new MojoExecutionException("Illegal exclusion specification " + exceptions[idx].toString());
                }
            }
        }
    }

    /**
     * Returns true if a given artifact and version are excluded from checking.
     */
    private boolean isExcluded(Artifact artifact, Version expectedVersion, Version resolvedVersion)
    {
        if (exceptions != null) {
            for (int idx = 0; idx < exceptions.length; idx++) {
                if (exceptions[idx].matches(artifact, expectedVersion, resolvedVersion)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a Set of artifacts based off the given project. Artifacts can be filtered and optional dependencies can be excluded.
     *
     * It would be awesome if this method would also use the DependencyTreeBuilder which seems to yield better results (and is much closer to the actual compile tree in some cases)
     * than the artifactResolver. However, due to MNG-3236 the artifact filter is not applied when resolving dependencies and this method relies on the artifact filter to get
     * the scoping right. Well, maybe in maven 3.0 this will be better. Or different. Whatever comes first.
     */
    private Set resolveDependenciesInItsOwnScope(final MavenProject project, final ArtifactFilter filter, final boolean includeOptional) throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException
    {
        Set dependencyArtifacts = MavenMetadataSource.createArtifacts(artifactFactory,
                                                                      project.getDependencies(),
                                                                      null,
                                                                      filter,
                                                                      null);

        ArtifactResolutionResult result = artifactResolver.resolveTransitively(dependencyArtifacts,
                                                                               project.getArtifact(),
                                                                               Collections.EMPTY_MAP,
                                                                               localRepository,
                                                                               remoteRepositories,
                                                                               artifactMetadataSource,
                                                                               new ArtifactOptionalFilter(includeOptional));

        return result.getArtifacts();
    }

    /**
     * Returns a Set of artifacts based off another artifact. The list of artifacts resolved can be filtered.
     *
     * It would be awesome if this method would also use the DependencyTreeBuilder which seems to yield better results (and is much closer to the actual compile tree in some cases)
     * than the artifactResolver. However, due to MNG-3236 the artifact filter is not applied when resolving dependencies and this method relies on the artifact filter to get
     * the scoping right. Well, maybe in maven 3.0 this will be better. Or different. Whatever comes first.
     */
    private Set resolveDependenciesInItsOwnScope(final Artifact artifact, final ArtifactFilter filter) throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException
    {
        MavenProject projectForArtifact = mavenProjectBuilder.buildFromRepository(artifact, remoteRepositories,localRepository);

        // "false" == do not include any optional dependencies from here. As these dependencies are off an artifact that is already a dependency, this
        //            needs to ignore all optional deps. This avoids downloading poms that might not even exist and should not be part of the dependency
        //            resolution of the main project.
        return resolveDependenciesInItsOwnScope(projectForArtifact, filter, false);
    }

    /**
     * Return a version object for an Artifact.
     */
    private Version getVersion(Artifact artifact) throws OverConstrainedVersionException
    {
        Version version = null;

        if (artifact != null) {
            if ((artifact.getVersionRange() != null) && (artifact.getSelectedVersion() != null)) {
                version = new Version(artifact.getVersionRange().toString(), artifact.getSelectedVersion().toString());
            }
            else {
                version = new Version(artifact.getVersion());
            }
        }
        return version;
    }

    /**
     * Returns the qualified name for an Artifact.
     */
    private String getQualifiedName(Artifact artifact)
    {
        String result = artifact.getGroupId() + ":" + artifact.getArtifactId();

        if ((artifact.getType() != null) && !"jar".equals(artifact.getType())) {
            result = result +  ":" + artifact.getType();
        }
        if ((artifact.getClassifier() != null) && (!"tests".equals(artifact.getClassifier()) || !"test-jar".equals(artifact.getType()))) {
            result = result +  ":" + artifact.getClassifier();
        }
        return result;
    }

    /**
     * Returns the qualified name for a Dependency.
     */
    private String getQualifiedName(Dependency dependency)
    {
        String result = dependency.getGroupId() + ":" + dependency.getArtifactId() ;

        if ((dependency.getType() != null) && !"jar".equals(dependency.getType())) {
            result = result +  ":" + dependency.getType();
        }
        if ((dependency.getClassifier() != null) && (!"tests".equals(dependency.getClassifier()) || !"test-jar".equals(dependency.getType()))) {
            result = result +  ":" + dependency.getClassifier();
        }
        return result;
    }

    /**
     * Reports an exception thrown by the resolution process.
     */
    private void logArtifactResolutionException(AbstractArtifactResolutionException ex)
    {
        if (ex instanceof MultipleArtifactsNotFoundException) {
            MultipleArtifactsNotFoundException multiEx = (MultipleArtifactsNotFoundException)ex;
            StringBuilder                      builder = new StringBuilder();

            for (Iterator iter = multiEx.getMissingArtifacts().iterator(); iter.hasNext();) {
                Artifact artifact = (Artifact)iter.next();
                builder.append(getQualifiedName(artifact));

                if (iter.hasNext()) {
                    builder.append(", ");
                }
            }
            LOG.warn("Could not find artifacts '{}'", builder);
        }
        else {
            LOG.warn("Could not find artifact '{}'", getQualifiedName(ex.getArtifact()));
        }
        LOG.debug("Error:", ex);
    }
}
