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

package com.ning.maven.plugins.dependencyversionscheck.util;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Filter a given artifact based on a list of scopes. Only allow
 * inclusion if the artifact is in one of the scopes.
 */
public class ArtifactScopeFilter implements ArtifactFilter
{
    final String [] scopes;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ArtifactScopeFilter(final String [] scopes)
    {
        this.scopes = scopes;
    }

    public boolean include(Artifact artifact)
    {
        for (int i = 0; i < scopes.length; i++) {
            if (artifact.getScope().equals(scopes[i])) {
                return true;
            }
        }
        return false;
    }
}

