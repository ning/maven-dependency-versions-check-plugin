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

import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

public class Version implements Comparable
{
    private final Integer[] version;
    private final String    qualifier;
    private final String    rawVersionStr;

    public Version(String versionStr)
    {
        this(versionStr, versionStr);
    }
    
    public Version(String rawVersionStr, String selectedVersionStr)
    {
        if (StringUtils.isEmpty(rawVersionStr) || StringUtils.isEmpty(selectedVersionStr)) {
            throw new NullPointerException("Version cannot be null");
        }

        this.rawVersionStr = rawVersionStr;

        int splitIdx = -1;

        for (int idx = 0; idx < selectedVersionStr.length(); idx++) {
            char c = selectedVersionStr.charAt(idx);

            if ((c >= '0') && (c <= '9')) {
                splitIdx = idx;
            }
            else if (c != '.') {
                break;
            }
        }
        if (splitIdx > 0) {
            StringTokenizer tokenizer = new StringTokenizer(selectedVersionStr.substring(0, splitIdx + 1), ".");

            version = new Integer[tokenizer.countTokens()];

            int idx = 0;

            while (tokenizer.hasMoreTokens()) {
                version[idx++] = Integer.valueOf(tokenizer.nextToken());
            }

            String qualifierPart = (splitIdx == selectedVersionStr.length()) ? "" : selectedVersionStr.substring(splitIdx + 1);

            if (qualifierPart.startsWith("-") || qualifierPart.startsWith("_") || qualifierPart.startsWith(".")) {
                qualifierPart = qualifierPart.substring(1);
            }
            qualifier = StringUtils.isEmpty(qualifierPart) ? null : qualifierPart;
        }
        else {
            version   = new Integer[0];
            qualifier = selectedVersionStr; 
        }
    }

    public String getRawVersionString()
    {
        return rawVersionStr;
    }

    public String toString()
    {
        if (qualifier == null) {
            return StringUtils.join(version, ".");
        }
        else {
            return version.length == 0 ? qualifier : StringUtils.join(version, ".") + "-" + qualifier;
        }
    }

    public boolean isHigherThanOrEqual(Version otherVersion)
    {
        if (version.length == 0) {
            return (otherVersion.version.length == 0) && StringUtils.equals(qualifier, otherVersion.qualifier);
        }
        else if (otherVersion.version.length == 0) {
            return false;
        }
        else {
            int lenToCheck = Math.min(version.length, otherVersion.version.length);

            for (int idx = 0; idx < lenToCheck; idx++) {
                if (version[idx].intValue() < otherVersion.version[idx].intValue()) {
                    return false;
                }
                else if (version[idx].intValue() > otherVersion.version[idx].intValue()) {
                    return true;
                }
            }
            return version.length >= otherVersion.version.length;
        }
    }

    public boolean hasHigherMajorVersion(Version otherVersion)
    {
        if ((version.length == 0) || (otherVersion.version.length == 0)) {
            return false;
        }
        else {
            return version[0].intValue() > otherVersion.version[0].intValue();
        }
    }

    public boolean equals(Object obj)
    {
        return (obj instanceof Version) && toString().equals(obj.toString());
    }

    public int hashCode()
    {
        return toString().hashCode();
    }

    public int compareTo(Object obj)
    {
        return toString().compareTo(obj.toString());
    }
}
