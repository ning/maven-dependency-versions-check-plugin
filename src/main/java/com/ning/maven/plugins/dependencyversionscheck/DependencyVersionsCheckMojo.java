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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
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

/**
 * Checks dependency versions.
 *
 * TODO: a list mojo would be useful which simply lists the resolutions
 *
 * @goal check
 * @phase verify
 * @requiresDependencyResolution test
 * @see <a href="http://docs.codehaus.org/display/MAVENUSER/Mojo+Developer+Cookbook">Mojo Developer Cookbook</a>
 */
public class DependencyVersionsCheckMojo extends AbstractMojo
{
    /**
     * The maven project (effective pom).
     * @parameter expression="${project}"
     * @required
     * @readonly
    */
    private MavenProject project;

    /**
     * For creating MavenProject objects.
     * @component
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * The artifact factory.
     * @component
     */
    private ArtifactFactory artifactFactory;
    
    /**
     * Resolves artifacts.
     * @component
     */
    private ArtifactResolver artifactResolver;

    /**
     * For resolving versions.
     * @component
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * The local repo for the project if defined;
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /**
     * Remote repositories.
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    private List remoteRepositories;

    /**
     * A set of artifacts with expected and resolved versions that are to be except from the check.
     * @parameter alias="exceptions"
     */
    private Exception[] exceptions;

    /**
     * Whether to warn if the resolved major version is higher then the expected one of the project or one of the depedencies.
     * @parameter default-value="false"
     */
    private boolean warnIfMajorVersionIsHigher;

    /**
     * Whether the mojo should fail the build if a conflict was found.
     * @parameter default-value="false"
     */
    private boolean failBuildInCaseOfConflict;

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if ((exceptions != null) && (exceptions.length > 0)) {
            for (int idx = 0; idx < exceptions.length; idx++) {
                if (exceptions[idx].check()) {
                    getLog().info("Adding exclusion " + exceptions[idx].toString());
                }
                else {
                    throw new MojoExecutionException("Illegal exclusion specification " + exceptions[idx].toString());
                }
            }
        }

        getLog().info("Checking dependency versions");

        Map resolvedDependenciesByName = new HashMap();
        Collection resolvedDependencies       = null;

        // first we resolve the dependencies for the current project
        try {
            resolvedDependencies = resolveDependenciesInItsOwnScope(project);
        }
        catch (InvalidDependencyVersionException ex) {
            getLog().error("Could not properly resolve all dependencies", ex);
        }
        catch (MultipleArtifactsNotFoundException ex) {
            logArtifactResolutionException(ex);
            resolvedDependencies = ex.getResolvedArtifacts();
        }
        catch (AbstractArtifactResolutionException ex) {
            logArtifactResolutionException(ex);
        }

        if (resolvedDependencies != null) {
            for (Iterator iter = resolvedDependencies.iterator(); iter.hasNext();) {
                Artifact artifact = (Artifact)iter.next();

                resolvedDependenciesByName.put(getQualifiedName(artifact), artifact);
            }
        }

        List resolutions = new ArrayList();
        // we also need the names of the explicit dependencies of the current project
        Set explicitDependenciesByName = new TreeSet();

        for (Iterator iter = project.getDependencies().iterator(); iter.hasNext();) {
            Dependency dependency       = (Dependency)iter.next();
            String     artifactName     = getQualifiedName(dependency);
            Artifact   resolvedArtifact = (Artifact)resolvedDependenciesByName.get(artifactName);

            explicitDependenciesByName.add(artifactName);

            if (resolvedArtifact == null) {
                 getLog().warn("No artifact available for " + artifactName + " (probably a multi-module child artifact)");
            }
            else {
                VersionResolution resolution = checkExplicitDependency(dependency, resolvedArtifact, artifactName);

                if (resolution != null) {
                    resolutions.add(resolution);
                }

                try {
                    getLog().debug("  Checking dependency " + artifactName);
                    resolutions.addAll(checkArtifactAgainstDependencies(dependency, resolvedArtifact, artifactName, resolvedDependenciesByName));
                }
                catch (InvalidDependencyVersionException ex) {
                    getLog().error("Could not properly resolve all dependencies", ex);
                }
                catch (AbstractArtifactResolutionException ex) {
                    logArtifactResolutionException(ex);
                }
                catch (ProjectBuildingException ex) {
                    getLog().warn("Could not properly resolve artifact", ex);
                }
            }
        }

        reportConflicts(resolutions, explicitDependenciesByName);
    }

    private VersionResolution checkExplicitDependency(Dependency dependency, Artifact artifact, String artifactName)
    {
        VersionResolution resolution = null;

        try {
            ArtifactVersion resolvedVersion = artifact.getSelectedVersion();

            if (resolvedVersion == null) {
                resolvedVersion = new DefaultArtifactVersion(artifact.getVersion());
            }

            VersionRange    versionRange    = VersionRange.createFromVersionSpec(dependency.getVersion());
            ArtifactVersion expectedVersion = versionRange.getRecommendedVersion();

            if (expectedVersion == null) {
                if (versionRange.containsVersion(resolvedVersion)) {
                    expectedVersion = resolvedVersion;
                }
                else {
                    getLog().error("Cannot determine the recommended version of dependency " + artifactName + "; its version specification is" + dependency.getVersion() + ", and the resolved version is " + resolvedVersion.toString());
                    return resolution;
                }
            }

            Version resolvedVersionObj = new Version(resolvedVersion.toString());
            Version depVersionObj      = new Version(versionRange.toString(), expectedVersion.toString());

            resolution = new VersionResolution(null, artifactName, depVersionObj, resolvedVersionObj);

            if (!versionRange.containsVersion(resolvedVersion) &&
                !resolvedVersionObj.isHigherThanOrEqual(depVersionObj) && 
                !isExcluded(artifact, depVersionObj, resolvedVersionObj)) {

                resolution.setConflict(true);
            }
        }
        catch (InvalidVersionSpecificationException ex) {
            getLog().warn("Could not parse the version specification of an artifact", ex);
        }
        catch (OverConstrainedVersionException ex) {
            getLog().warn("Could not resolve an artifact", ex);
        }
        return resolution;
    }
    
    private Set resolveDependenciesInItsOwnScope(MavenProject project) throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException
    {
        Set dependencyArtifacts = MavenMetadataSource.createArtifacts(artifactFactory,
                                                                      project.getDependencies(),
                                                                      null,
                                                                      null,
                                                                      null);

        ArtifactResolutionResult result = artifactResolver.resolveTransitively(dependencyArtifacts,
                                                                               project.getArtifact(),
                                                                               Collections.EMPTY_MAP,
                                                                               localRepository,
                                                                               remoteRepositories,
                                                                               artifactMetadataSource,
                                                                               null);
                                                                       
        return result.getArtifacts();
    }

    private Set resolveDependenciesInItsOwnScope(Artifact artifact) throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException
    {
        MavenProject projectForArtifact = mavenProjectBuilder.buildFromRepository(artifact, remoteRepositories,localRepository);

        return resolveDependenciesInItsOwnScope(projectForArtifact);
    }

    private List checkArtifactAgainstDependencies(Dependency dependency,
                                                  Artifact   artifact,
                                                  String     nameOfArtifactToCheck,
                                                  Map        resolvedDependenciesByName) throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException
    {
        Set  dependenciesToCheck = resolveDependenciesInItsOwnScope(artifact);
        List resolutions         = new ArrayList();
        Set  exclusions          = new HashSet();

        if (dependency.getExclusions() != null) {
            for (Iterator exclusionIter = dependency.getExclusions().iterator(); exclusionIter.hasNext();) {
                Exclusion exclusion = (Exclusion)exclusionIter.next();
    
                exclusions.add(exclusion.getGroupId() + ":" + exclusion.getArtifactId());
            }
        }

        for (Iterator dependenciesToCheckIter = dependenciesToCheck.iterator(); dependenciesToCheckIter.hasNext();) {
            Artifact dependencyArtifactToCheck = (Artifact)dependenciesToCheckIter.next();

            // only artifacts in scopes compile, runtime and system are inherited
            if (Artifact.SCOPE_COMPILE.equals(dependencyArtifactToCheck.getScope()) ||
                Artifact.SCOPE_RUNTIME.equals(dependencyArtifactToCheck.getScope()) ||
                Artifact.SCOPE_SYSTEM.equals(dependencyArtifactToCheck.getScope())) {

                String   artifactName       = getQualifiedName(dependencyArtifactToCheck);
                Artifact resolvedDependency = (Artifact)resolvedDependenciesByName.get(artifactName);
    
                if (resolvedDependency == null) {
                    getLog().debug("    Dependency " + artifactName + ":" + dependencyArtifactToCheck.getVersion() +" of artifact " + nameOfArtifactToCheck + " is no longer used in the current project");
                }
                else {
                    // if the artifact in question is excluded in the current pom, then we don't have to worry about it anyways
                    if (!exclusions.contains(dependencyArtifactToCheck.getGroupId() + ":" + dependencyArtifactToCheck.getArtifactId())) {
                        Version           resolvedVersion = getVersion(resolvedDependency);
                        Version           versionToCheck  = getVersion(dependencyArtifactToCheck);
                        VersionResolution resolution      = new VersionResolution(nameOfArtifactToCheck, artifactName, versionToCheck, resolvedVersion);
        
                        resolutions.add(resolution);
    
                        // we have an error if
                        // - if resolved dependency has a lower version or different qualifier than the stated one of the current transitive dependency
                        // - if resolver dependency has a higher major version than the stated one of the current transitive dependency and
                        //   there is no explicit dependency to that major version in the current project
                        // for this last check, we assume that explicit dependencies have already been checked against actual ones, so we only need to check
                        // if the artifact is an explicit dependency
                        if (!resolvedVersion.isHigherThanOrEqual(versionToCheck)) {
                            if (!isExcluded(resolvedDependency, versionToCheck, resolvedVersion)) {
                                resolution.setConflict(true);
                            }
                        }
                        else if (warnIfMajorVersionIsHigher && resolvedVersion.hasHigherMajorVersion(versionToCheck)) {
                            getLog().warn("    Artifact " + nameOfArtifactToCheck + " depends on " + artifactName + " at a higher major version (" + dependencyArtifactToCheck.getVersion() + ") than the current project (" + resolvedDependency.getVersion() + ")");
                        }
                    }
                }
            }
        }
        return resolutions;
    }

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
    
    private void reportConflicts(List resolutions, Set explicitDependencyNames) throws MojoFailureException
    {
        Map resolutionsByDependencyName      = new HashMap();
        Map resolvedVersionsByDependencyName = new HashMap();
        Set conflictedArtifacts              = new TreeSet();
        Map expectedVersionsByDependencyName = new HashMap();

        // we're organizing the resolutions in a specific way to simplify the output:
        // dependency -> ( expected version -> dependent )
        for (Iterator iter = resolutions.iterator(); iter.hasNext();) {
            VersionResolution resolution = (VersionResolution)iter.next();

            resolvedVersionsByDependencyName.put(resolution.getDependencyName(), resolution.getActualVersion());

            // current project ?
            if (resolution.getDependentName() == null) {
                expectedVersionsByDependencyName.put(resolution.getDependencyName(), resolution.getExpectedVersion());
            }
            else {
                Map resolutionsByExpectedVersion = (Map)resolutionsByDependencyName.get(resolution.getDependencyName());

                if (resolutionsByExpectedVersion == null) {
                    resolutionsByExpectedVersion = new TreeMap();
                    resolutionsByDependencyName.put(resolution.getDependencyName(), resolutionsByExpectedVersion);
                }

                Set resolutionsByDependentName = (Set)resolutionsByExpectedVersion.get(resolution.getExpectedVersion());

                if (resolutionsByDependentName == null) {
                    resolutionsByDependentName = new TreeSet();
                    resolutionsByExpectedVersion.put(resolution.getExpectedVersion(), resolutionsByDependentName);
                }

                resolutionsByDependentName.add(resolution.getDependentName());
            }
            if (resolution.isConflict()) {
                conflictedArtifacts.add(resolution.getDependencyName());
            }
        }
        // we log direct dependencies first
        for (Iterator explicitDependencyIter = explicitDependencyNames.iterator(); explicitDependencyIter.hasNext();) {
            String artifactName = (String)explicitDependencyIter.next();

            if (conflictedArtifacts.contains(artifactName)) {
                Map     resolutionsForArtifact = (Map)resolutionsByDependencyName.get(artifactName);
                Version expectedVersion        = (Version)expectedVersionsByDependencyName.get(artifactName);
                Version resolvedVersion        = (Version)resolvedVersionsByDependencyName.get(artifactName);

                logResolutionsForConflict("Found a problem with the direct dependency " + artifactName + " of the current project\n  Expected version is " + expectedVersion.getRawVersionString(),
                                          resolutionsForArtifact,
                                          resolvedVersion);
            }
        }
        for (Iterator conflictedArtifactsIter = conflictedArtifacts.iterator(); conflictedArtifactsIter.hasNext();) {
            String conflictedArtifactName = (String)conflictedArtifactsIter.next();

            if (!explicitDependencyNames.contains(conflictedArtifactName)) {
                Map     resolutionsForArtifact = (Map)resolutionsByDependencyName.get(conflictedArtifactName);
                Version resolvedVersion        = (Version)resolvedVersionsByDependencyName.get(conflictedArtifactName);
    
                logResolutionsForConflict("Found a problem with the dependency " + conflictedArtifactName,
                                          resolutionsForArtifact,
                                          resolvedVersion);
            }
        }
        if (failBuildInCaseOfConflict && !conflictedArtifacts.isEmpty()) {
            throw new MojoFailureException("Found dependency version conflicts");
        }
    }

    private void logResolutionsForConflict(String  mainMessage,
                                           Map     resolutionsForArtifact,
                                           Version resolvedVersion)
    {
        StringBuilder msgBuilder = new StringBuilder(mainMessage);

        msgBuilder.append("\n  Resolved version is ");
        msgBuilder.append(resolvedVersion.getRawVersionString());
        for (Iterator expectedVersionIter = resolutionsForArtifact.entrySet().iterator(); expectedVersionIter.hasNext();) {
            Map.Entry expectedVersionEntry = (Map.Entry)expectedVersionIter.next();
            Version   expectedVersion      = (Version)expectedVersionEntry.getKey();
            Set       dependents           = (Set)expectedVersionEntry.getValue();

            if (resolvedVersion.hasHigherMajorVersion(expectedVersion)) {
                msgBuilder.append("\n  A lower major version ");
            }
            else if (resolvedVersion.isHigherThanOrEqual(expectedVersion)) {
                msgBuilder.append("\n  Version ");
            } 
            else {
                msgBuilder.append("\n  A newer version ");
            } 
            msgBuilder.append(expectedVersion.getRawVersionString());
            msgBuilder.append(" was expected by artifact");
            if (dependents.size() > 1) {
                msgBuilder.append("s");
            }
            msgBuilder.append(": ");

            boolean isFirst = true;

            for (Iterator dependentNameIter = dependents.iterator(); dependentNameIter.hasNext();) {
                String name = (String)dependentNameIter.next();

                if (isFirst) {
                    isFirst = false;
                }
                else {
                    msgBuilder.append(", ");
                }
                if (name == null) {
                    msgBuilder.append("Current project");
                }
                else {
                    msgBuilder.append(name);
                }
            }
        }

        if (failBuildInCaseOfConflict) {
            getLog().error(msgBuilder.toString());
        }
        else {
            getLog().warn(msgBuilder.toString());
        }
    }
    
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

    private void logArtifactResolutionException(AbstractArtifactResolutionException ex)
    {
        if (ex instanceof MultipleArtifactsNotFoundException) {
            MultipleArtifactsNotFoundException multiEx = (MultipleArtifactsNotFoundException)ex;
            StringBuilder                      builder = new StringBuilder();

            for (Iterator iter = multiEx.getMissingArtifacts().iterator(); iter.hasNext();) {
                Artifact artifact = (Artifact)iter.next();

                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(getQualifiedName(artifact));
            }
            getLog().warn("Could not find artifacts " + builder.toString());
        }
        else {
            getLog().warn("Could not find artifact " + getQualifiedName(ex.getArtifact()));
        }
        getLog().debug(ex);
    }
}
