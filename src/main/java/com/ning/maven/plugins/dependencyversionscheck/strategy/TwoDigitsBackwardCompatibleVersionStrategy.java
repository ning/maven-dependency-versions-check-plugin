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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Relaxed variant of APR, very suitable for Java code. It is assumed that for every non-backwards compatible change, the artifactId
 * is changed (e.g. by attaching a number to the artifactId) and the code is repackaged into a different package. So it is possible to
 * have multiple, non-backwards compatible major versions on the classpath (foo vs. foo2 vs.foo3). So all versions with the same artifactId
 * are backwards compatible; only forwards compatibility must be ensured.
 *
 * By using the APR parser, the major version flags forwards compatibility, the minor and patch are not used. If a qualifier is present,
 * it must match.
 *
 * @plexus.component role="com.ning.maven.plugins.dependencyversionscheck.strategy.Strategy" role-hint="two-digits-backward-compatible"
 */
public class TwoDigitsBackwardCompatibleVersionStrategy extends APRVersionStrategy
{
    private static final Logger LOG = LoggerFactory.getLogger(Version.class);

    public String getName()
    {
        return "two-digits-backward-compatible";
    }

    protected boolean checkCompatible(final AprVersion aprVersionA, final AprVersion aprVersionB)
    {
        if (aprVersionA == null || aprVersionB == null) {
            LOG.debug("... no, could not parse versions.");
            return false;
        }

        if (aprVersionA.getMajor() >= aprVersionB.getMajor()) {
            LOG.debug("... yes, major version ok!");
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
}

