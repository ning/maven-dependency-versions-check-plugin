/*
 * Copyright (C) 2011 Henning Schmiedehausen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.maven.plugins.dependencyversionscheck.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.maven.plugins.dependencyversionscheck.version.Version;
import com.ning.maven.plugins.dependencyversionscheck.version.VersionElement;

/**
 * This is the default versioning strategy used by previous versions of the plugin.
 * It assumes that all smaller versions are compatible when replaced with larger numbers and compares version
 * elements from left to right. E.g. 3.2.1 > 3.2 and 2.1.1 > 1.0. Usually works pretty ok.
 *
 * @plexus.component role="com.ning.maven.plugins.dependencyversionscheck.strategy.Strategy" role-hint="default"
 */
public class DefaultVersionStrategy implements Strategy
{
    private static final Logger LOG = LoggerFactory.getLogger(Version.class);

    public String getName()
    {
        return "default";
    }

    public boolean isCompatible(final Version versionA, final Version versionB)
    {
        LOG.debug("Is {} compatible to {}... ", versionA, versionB);
        final VersionElement[] versionAElements = versionA.getVersionElements();
        final VersionElement[] versionBElements = versionB.getVersionElements();

        int lenToCheck = Math.min(versionAElements.length, versionBElements.length);

        for (int i = 0; i < lenToCheck; i++) {
            final VersionElement versionAElement = versionAElements[i];
            final VersionElement versionBElement = versionBElements[i];

            if (versionAElement.hasNumbers() && versionBElement.hasNumbers()) {
                long versionANumber = versionAElement.getNumber();
                long versionBNumber = versionBElement.getNumber();

                if (versionANumber < versionBNumber) {
                    LOG.debug("... no!");
                    return false;
                }
                else if (versionANumber > versionBNumber) {
                    LOG.debug("... yes!");
                    return true;
                }
            }
            else if (!versionAElement.getElement().equals(versionBElement.getElement())) {
                LOG.debug("... no!");
                return false;
            }
        }

        // Special case for -SNAPSHOT artifacts
        if (versionBElements.length == versionAElements.length + 1 &&
                versionBElements[versionBElements.length-1].getElement().equals("SNAPSHOT")) {
            LOG.debug("... yes, is just a SNAPSHOT release");
            return true;
        }

        final boolean result = versionAElements.length >= versionBElements.length;
        LOG.debug("... {}!", result ? "yes" : "no");
        return result;

    }
}
