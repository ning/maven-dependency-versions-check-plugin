/*
 * Copyright (C) 2011 Henning Schmiedehausen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import com.ning.maven.plugins.dependencyversionscheck.version.VersionResolution;

/**
 * Lists dependency versions in different scopes.
 *
 * @goal list
 * @requiresDependencyResolution test
 * @since 2.0.0
 */
public class DependencyVersionsListMojo extends AbstractDependencyVersionsMojo
{
    /**
     * The scope to list. Defaults to "compile". Valid values are "compile", "test" and "runtime".
     *
     * @parameter expression="${scope}" default-value="compile"
     */
    protected String scope = "compile";

    /**
     * Whether to list only direct dependencies or all dependencies. Default is to list all dependencies.
     *
     * @parameter expression="${directOnly}" default-value="false"
     */
    protected boolean directOnly = false;

    /**
     * Whether to list all dependencies or only dependencies in conflict. Default is to list all dependencies.
     *
     * @parameter expression="${conflictsOnly}" default-value="false"
     */
    protected boolean conflictsOnly = false;

    protected void doExecute() throws Exception
    {
        checkScope();

        final Map resolutionMap = buildResolutionMap(scope);

        LOG.info("{} dependencies for scope '{}':", (directOnly ? "Direct" : "Transitive"), scope);

        for (final Iterator it = resolutionMap.entrySet().iterator(); it.hasNext();) {
            final Map.Entry entry = (Map.Entry) it.next();
            final String artifactName = (String) entry.getKey();
            final List resolutions = (List) entry.getValue();

            if (CollectionUtils.isEmpty(resolutions)) {
                LOG.warn("No resolutions for '{}', this should never happen!", artifactName);
                continue; // for
            }

            final VersionResolution resolution = (VersionResolution) resolutions.get(0);

            // Map from version to VersionInformation
            final Map versionMap = new TreeMap();

            boolean foundConflict = false;
            boolean foundDirectDependency = false;

            for (Iterator resolutionIt = resolutions.iterator(); resolutionIt.hasNext(); ) {
                final VersionResolution versionResolution = (VersionResolution) resolutionIt.next();
                final String expectedVersion = versionResolution.getExpectedVersion().getSelectedVersion();

                VersionInformation versionInformation = (VersionInformation) versionMap.get(expectedVersion);
                if (versionInformation == null) {
                    versionInformation = new VersionInformation(expectedVersion);
                    versionMap.put(expectedVersion, versionInformation);
                }

                if (versionResolution.isConflict()) {
                    versionInformation.setConflict(true);
                    foundConflict = true;
                }

                if (versionResolution.isDirectDependency()) {
                    versionInformation.setDirectDependency(true);
                    foundDirectDependency = true;
                }
            }

            if (conflictsOnly && !foundConflict) {
                continue; // for;
            }

            if (directOnly && !foundDirectDependency) {
                continue; // for;
            }

            final StringBuilder result = new StringBuilder(StringUtils.rightPad(artifactName + ": ", maxLen + 2));
            result.append(resolution.getDependencyName()).append("-").append(resolution.getActualVersion().getSelectedVersion());
            result.append(" (");

            for (Iterator versionIt = versionMap.values().iterator(); versionIt.hasNext(); ) {
                VersionInformation versionInformation = (VersionInformation) versionIt.next();

                result.append(versionInformation);

                if (versionIt.hasNext()) {
                    result.append(", ");
                }
            }

            result.append(")");

            LOG.info(result.toString());
        }
    }

    private void checkScope()
        throws MojoExecutionException
    {
        if (!(Artifact.SCOPE_COMPILE.equals(scope)
                        || Artifact.SCOPE_TEST.equals(scope)
                        || Artifact.SCOPE_RUNTIME.equals(scope))) {
            throw new MojoExecutionException("Scope '" + scope + "' is invalid!");
        }
    }

    private static class VersionInformation
    {
        private final String version;

        private boolean conflict = false;
        private boolean directDependency = false;

        private VersionInformation(final String version)
        {
            this.version = version;
        }

        private void setConflict(boolean conflict)
        {
            this.conflict = conflict;
        }

        private void setDirectDependency(boolean directDependency)
        {
            this.directDependency = directDependency;
        }

        public String toString()
        {
            String result = version;
            if (directDependency) {
                result = "*" + result + "*";
            }
            if (conflict) {
                result = "!" + result + "!";
            }
            return result;
        }
    }
}

