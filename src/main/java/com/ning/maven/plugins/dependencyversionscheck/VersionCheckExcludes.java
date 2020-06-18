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

import com.ning.maven.plugins.dependencyversionscheck.version.Version;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;

public class VersionCheckExcludes
{
    private String groupId;
    private String artifactId;
    private String classifier;
    private String type = "jar";
    private Version expectedVersion;
    private Version resolvedVersion;

    public void setGroupdId(String groupId)
    {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId)
    {
        this.artifactId = artifactId;
    }

    public void setClassifier(String classifier)
    {
        this.classifier = classifier;
    }

    public void setType(String type)
    {
        this.type = (type == null ? "jar" : type);
    }

    public void setExpectedVersion(String versionStr)
    {
        this.expectedVersion = new Version(versionStr);
    }

    public void setResolvedVersion(String versionStr)
    {
        this.resolvedVersion = new Version(versionStr);
    }

    public boolean check()
    {
        return !StringUtils.isEmpty(groupId) && !StringUtils.isEmpty(artifactId) && (expectedVersion != null) && (resolvedVersion != null);
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        builder.append(groupId);
        builder.append(":");
        builder.append(artifactId);
        if (!"jar".equals(type)) {
            builder.append(":");
            builder.append(type);
        }
        if (classifier != null) {
            builder.append(":");
            builder.append(classifier);
        }
        builder.append(" ");
        builder.append(expectedVersion.getSelectedVersion());
        builder.append(" vs. ");
        builder.append(resolvedVersion.getSelectedVersion());
        return builder.toString();
    }

    public boolean matches(Artifact artifact, Version expectedVersion, Version resolvedVersion)
    {
        return StringUtils.equals(groupId, artifact.getGroupId()) &&
               StringUtils.equals(artifactId, artifact.getArtifactId()) &&
               StringUtils.equals(classifier, artifact.getClassifier()) &&
               StringUtils.equals(type, artifact.getType()) &&
               this.expectedVersion.equals(expectedVersion) &&
               this.resolvedVersion.equals(resolvedVersion);
    }
}
