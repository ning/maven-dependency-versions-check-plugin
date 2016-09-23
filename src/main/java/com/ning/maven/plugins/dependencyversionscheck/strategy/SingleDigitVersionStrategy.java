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
 *
 * Single Digit, may have a prefix. Assume that larger numbers are backwards compatible.
 *
 * e.g. used for google guava.
 *
 * @plexus.component role="com.ning.maven.plugins.dependencyversionscheck.strategy.Strategy" role-hint="single-digit"
 */
public class SingleDigitVersionStrategy implements Strategy
{
    private static final Logger LOG = LoggerFactory.getLogger(Version.class);

    public String getName()
    {
        return "single-digit";
    }

    public boolean isCompatible(final Version versionA, final Version versionB)
    {
        LOG.debug("Is {} compatible to {}... ", versionA, versionB);
        final VersionElement[] versionAElements = versionA.getVersionElements();
        final VersionElement[] versionBElements = versionB.getVersionElements();

        final boolean res = versionAElements[0].getNumber() >= versionBElements[0].getNumber();
        LOG.debug("... {}.", res ? "yes" : "no");
        return res;
    }
}
