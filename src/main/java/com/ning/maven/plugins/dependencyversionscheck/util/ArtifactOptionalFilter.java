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

/**
 * Excludes or includes optional artifacts. If the optional argument on the c'tor is true,
 * allow optional artifacts, otherwise drop them.
 */
public class ArtifactOptionalFilter implements ArtifactFilter
{
    private final boolean optional;

    public ArtifactOptionalFilter(final boolean optional)
    {
        this.optional = optional;
    }

    public boolean include(final Artifact artifact)
    {
        if (artifact.isOptional()) {
            return optional;
        }
        else {
            return true;
        }
    }
}

