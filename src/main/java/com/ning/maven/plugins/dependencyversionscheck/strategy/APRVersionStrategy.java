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

import com.ning.maven.plugins.dependencyversionscheck.version.Version;
import com.ning.maven.plugins.dependencyversionscheck.version.VersionElement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements Apache versioning strategy for two or three digits. It expects versions formatted as x.y, x.y.z. Versions
 * can have an additional qualifier.
 *
 * Version A (xa.ya.za) can replace Version B (xb.yb.zb) if xa == xb and xa >= xb. component z is always compatible.
 *
 * If an additional qualifier exists, the qualifiers must match.
 *
 * @plexus.component role="com.ning.maven.plugins.dependencyversionscheck.strategy.Strategy" role-hint="apr"
 */
public class APRVersionStrategy implements Strategy
{
    protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

    public String getName()
    {
        return "apr";
    }

    public final boolean isCompatible(final Version versionA, final Version versionB)
    {
        LOG.debug("Is {} compatible to {}... ", versionA, versionB);
        final VersionElement[] versionAElements = versionA.getVersionElements();
        final VersionElement[] versionBElements = versionB.getVersionElements();

        final AprVersion aprVersionA = getAprVersion(versionAElements);
        final AprVersion aprVersionB = getAprVersion(versionBElements);

        return checkCompatible(aprVersionA, aprVersionB);
    }

    protected boolean checkCompatible(final AprVersion aprVersionA, final AprVersion aprVersionB)
    {
        if (aprVersionA == null || aprVersionB == null) {
            LOG.debug("... no, could not parse versions.");
            return false;
        }

        if (aprVersionA.getMajor() != aprVersionB.getMajor()) {
            LOG.debug("... no, different major versions!");
            return false;
        }

        if (aprVersionA.getMinor() >= aprVersionB.getMinor()) {
            LOG.debug("... yes, minor version is ok!");
            return true;
        }

        final boolean res = StringUtils.equals(aprVersionA.getQualifier(), aprVersionB.getQualifier());
        if (!res) {
            LOG.debug("... no, qualifiers don't match!");
        }
        else {
            LOG.debug("... yes!");
        }
        return res;
    }

    private AprVersion getAprVersion(final VersionElement[] versionElements)
    {
        if (versionElements.length < 2) {
            return null;
        }

        if (!(versionElements[0].isNumber()
              && versionElements[0].hasDot()
              && versionElements[1].isNumber())) {
            return null;
        }

        final long major = versionElements[0].getNumber();
        final long minor = versionElements[1].getNumber();

        long patch = 0;
        int qualifierStart = 1;

        if (versionElements.length > 2) {
            if (versionElements[1].hasDot() && versionElements[2].isNumber()) {
                patch = versionElements[2].getNumber();
                qualifierStart = 2;
            }
        }

        StringBuffer qualifier = new StringBuffer();
        while (!versionElements[qualifierStart++].hasEndOfVersion()) {
            qualifier.append(versionElements[qualifierStart]);
        }

        return new AprVersion(major, minor, patch, qualifier.toString());
    }

    public static final class AprVersion
    {
        private final long major;
        private final long minor;
        private final long patch;
        private final String qualifier;

        public AprVersion(long major, long minor, long patch, String qualifier)
        {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.qualifier = qualifier;
        }

        public long getMajor()
        {
            return major;
        }

        public long getMinor()
        {
            return minor;
        }

        public long getPatch()
        {
            return patch;
        }

        public String getQualifier()
        {
            return qualifier;
        }
    }
}
