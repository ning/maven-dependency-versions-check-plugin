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

public class VersionResolution
{
    private final String  dependentName;
    private final String  dependencyName;
    private final Version expectedVersion;
    private final Version actualVersion;
    private boolean       isConflict;

    public VersionResolution(String dependentName, String dependencyName, Version expectedVersion, Version actualVersion)
    {
        this.dependentName   = dependentName;
        this.dependencyName  = dependencyName;
        this.expectedVersion = expectedVersion;
        this.actualVersion   = actualVersion;
    }

    public String getDependentName()
    {
        return dependentName;
    }

    public String getDependencyName()
    {
        return dependencyName;
    }


    public Version getExpectedVersion()
    {
        return expectedVersion;
    }

    public Version getActualVersion()
    {
        return actualVersion;
    }

    public void setConflict(boolean isConflict)
    {
        this.isConflict = isConflict;
    }

    public boolean isConflict()
    {
        return isConflict;
    }
}
