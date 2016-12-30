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

package com.ning.maven.plugins.dependencyversionscheck.strategy;

import com.ning.maven.plugins.dependencyversionscheck.version.Version;

import org.junit.Assert;
import org.junit.Test;

public class TestDefaultVersionStrategy {

    @Test
    public void testSnapshotVersion() {
        assertCompatible("2.0.0", "2.0.0-SNAPSHOT");
    }

    @Test
    public void testSnapshotBadVersion() {
        assertNotCompatible("2.0.9", "2.1.0-SNAPSHOT");
    }

    @Test
    public void testMinorVersion() {
        assertCompatible("2.1.0", "2.0.9");
    }

    @Test
    public void testMinorBadVersion() {
        assertNotCompatible("2.1.9", "2.2.0");
    }

    @Test
    public void testPatchVersion() {
        assertCompatible("2.2.2", "2.2.1");
    }

    @Test
    public void testPatchBadVersion() {
        assertNotCompatible("2.2.2", "2.2.3");
    }

    private static void assertCompatible(String resolvedVersion, String otherVersion) {
        Version resolved = new Version(resolvedVersion);
        Version other = new Version(otherVersion);
        DefaultVersionStrategy strategy = new DefaultVersionStrategy();

        Assert.assertTrue(strategy.isCompatible(resolved, other));
    }

    private static void assertNotCompatible(String resolvedVersion, String otherVersion) {
        Version resolved = new Version(resolvedVersion);
        Version other = new Version(otherVersion);
        DefaultVersionStrategy strategy = new DefaultVersionStrategy();

        Assert.assertFalse(strategy.isCompatible(resolved, other));
    }
}
