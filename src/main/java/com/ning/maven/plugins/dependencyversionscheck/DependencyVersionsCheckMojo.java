/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.maven.plugins.dependencyversionscheck;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.plugin.MojoFailureException;

import com.ning.maven.plugins.dependencyversionscheck.version.Version;
import com.ning.maven.plugins.dependencyversionscheck.version.VersionResolution;

/**
 * Checks dependency versions.
 *
 * @goal check
 * @phase verify
 * @requiresDependencyResolution test
 * @see <a href="http://docs.codehaus.org/display/MAVENUSER/Mojo+Developer+Cookbook">Mojo Developer Cookbook</a>
 */
public class DependencyVersionsCheckMojo extends AbstractDependencyVersionsMojo
{
    /**
     * Whether the mojo should fail the build if a conflict was found.
     *
     * @parameter default-value="false"
     */
    protected boolean failBuildInCaseOfConflict;


    protected void doExecute() throws Exception
    {
        LOG.info("Checking dependency versions");

        // Use the special scope "null" to check a transitive hull of all scopes.
        // See the definitions of TRANSITIVE_SCOPE and VISIBLE_SCOPE in
        // AbstractDependencyVersionsMojo for details.
        final Map resolutionMap = buildResolutionMap(null);

        reportConflicts(resolutionMap);
    }

    private void reportConflicts(Map resolutionMap) throws MojoFailureException
    {
        Map resolutionsByDependencyName = new HashMap();
        Map resolvedVersionsByDependencyName = new HashMap();
        Set conflictedArtifacts = new TreeSet();
        Map expectedVersionsByDependencyName = new HashMap();
        Set explicitDependencyNames = new HashSet();

        // we're organizing the resolutions in a specific way to simplify the output:
        // dependency -> ( expected version -> dependent )
        for (Iterator iter = resolutionMap.entrySet().iterator(); iter.hasNext();) {
            final Map.Entry entry = (Map.Entry) iter.next();
            List resolutions = (List) entry.getValue();
            if (CollectionUtils.isEmpty(resolutions)) {
                LOG.warn("No resolutions found for {}, skipping!", (String) entry.getKey());
            }

            for (Iterator resolutionIt = resolutions.iterator(); resolutionIt.hasNext();) {
                final VersionResolution resolution = (VersionResolution) resolutionIt.next();

                resolvedVersionsByDependencyName.put(resolution.getDependencyName(), resolution.getActualVersion());

                if (resolution.isDirectDependency()) {
                    expectedVersionsByDependencyName.put(resolution.getDependencyName(), resolution.getExpectedVersion());
                    explicitDependencyNames.add(resolution.getDependencyName());
                }
                else {
                    Map resolutionsByExpectedVersion = (Map) resolutionsByDependencyName.get(resolution.getDependencyName());

                    if (resolutionsByExpectedVersion == null) {
                        resolutionsByExpectedVersion = new TreeMap();
                        resolutionsByDependencyName.put(resolution.getDependencyName(), resolutionsByExpectedVersion);
                    }

                    Set resolutionsByDependentName = (Set) resolutionsByExpectedVersion.get(resolution.getExpectedVersion());

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
        }

        // we log direct dependencies first
        for (Iterator explicitDependencyIter = explicitDependencyNames.iterator(); explicitDependencyIter.hasNext();) {
            String artifactName = (String) explicitDependencyIter.next();

            if (conflictedArtifacts.contains(artifactName)) {
                Map resolutionsForArtifact = (Map) resolutionsByDependencyName.get(artifactName);
                Version expectedVersion = (Version) expectedVersionsByDependencyName.get(artifactName);
                Version resolvedVersion = (Version) resolvedVersionsByDependencyName.get(artifactName);

                logResolutionsForConflict("Found a problem with the direct dependency " + artifactName + " of the current project\n  Expected version is " + expectedVersion.getSelectedVersion(),
                    resolutionsForArtifact,
                    resolvedVersion);
            }
        }
        for (Iterator conflictedArtifactsIter = conflictedArtifacts.iterator(); conflictedArtifactsIter.hasNext();) {
            String conflictedArtifactName = (String) conflictedArtifactsIter.next();

            if (!explicitDependencyNames.contains(conflictedArtifactName)) {
                Map resolutionsForArtifact = (Map) resolutionsByDependencyName.get(conflictedArtifactName);
                Version resolvedVersion = (Version) resolvedVersionsByDependencyName.get(conflictedArtifactName);

                logResolutionsForConflict("Found a problem with the dependency " + conflictedArtifactName,
                    resolutionsForArtifact,
                    resolvedVersion);
            }
        }
        if (failBuildInCaseOfConflict && !conflictedArtifacts.isEmpty()) {
            throw new MojoFailureException("Found dependency version conflicts");
        }
    }

    protected void logResolutionsForConflict(String mainMessage,
        Map resolutionsForArtifact,
        Version resolvedVersion)
    {
        StringBuilder msgBuilder = new StringBuilder(mainMessage);

        msgBuilder.append("\n  Resolved version is ");
        msgBuilder.append(resolvedVersion.getSelectedVersion());
        for (Iterator expectedVersionIter = resolutionsForArtifact.entrySet().iterator(); expectedVersionIter.hasNext();) {
            Map.Entry expectedVersionEntry = (Map.Entry) expectedVersionIter.next();
            Version expectedVersion = (Version) expectedVersionEntry.getKey();
            Set dependents = (Set) expectedVersionEntry.getValue();

            msgBuilder.append("\n  Version ");
            msgBuilder.append(expectedVersion.getSelectedVersion());
            msgBuilder.append(" was expected by artifact");
            if (dependents.size() > 1) {
                msgBuilder.append("s");
            }
            msgBuilder.append(": ");

            boolean isFirst = true;

            for (Iterator dependentNameIter = dependents.iterator(); dependentNameIter.hasNext();) {
                String name = (String) dependentNameIter.next();

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
            LOG.error(msgBuilder.toString());
        }
        else {
            LOG.warn(msgBuilder.toString());
        }
    }


}
