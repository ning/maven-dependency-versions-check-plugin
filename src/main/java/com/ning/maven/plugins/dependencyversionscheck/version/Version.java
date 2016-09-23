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

package com.ning.maven.plugins.dependencyversionscheck.version;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Represents an artifact version.
 */
public final class Version implements Comparable
{
    // Flags for a version element.
    public static final long ALL_NUMBERS = 0x001;
    public static final long ALL_LETTERS = 0x002;
    public static final long ALL_OTHER = 0x004;
    public static final long STARTS_WITH_NUMBERS = 0x008;
    public static final long STARTS_WITH_LETTERS = 0x010;
    public static final long STARTS_WITH_OTHER = 0x020;
    public static final long ENDS_WITH_NUMBERS = 0x040;
    public static final long ENDS_WITH_LETTERS = 0x080;
    public static final long ENDS_WITH_OTHER = 0x100;
    public static final long NUMBERS = 0x200;
    public static final long LETTERS = 0x400;
    public static final long OTHER = 0x800;

    // Flags for a version element divider.
    public static final long DOT_DIVIDER = 0x01;
    public static final long MINUS_DIVIDER = 0x02;
    public static final long UNDERSCORE_DIVIDER = 0x04;
    public static final long OTHER_DIVIDER = 0x08;
    public static final long END_OF_VERSION = 0x10;

    private final String rawVersion;
    private final String selectedVersion;

    private final String[] rawElements;
    private final VersionElement[] versionElements;

    private final int elementCount;

    public Version(final String versionStr)
    {
        this(versionStr, versionStr);
    }

    public Version(final String rawVersion, final String selectedVersion)
    {
        if (StringUtils.isBlank(rawVersion) || StringUtils.isBlank(selectedVersion)) {
            throw new NullPointerException("Version cannot be null");
        }

        this.selectedVersion = selectedVersion;
        this.rawVersion = rawVersion;

        rawElements = StringUtils.splitPreserveAllTokens(selectedVersion, "-._");
        versionElements = new VersionElement[rawElements.length];

        // Position of the next splitter to look at.
        int charPos = 0;

        // Position to store the result in.
        int resultPos = 0;

        for (int i = 0; i < rawElements.length; i++) {
            final String rawElement = rawElements[i];

            long divider = 0L;
            final char dividerChar;

            charPos += rawElement.length();
            if (charPos < selectedVersion.length()) {
                dividerChar = selectedVersion.charAt(charPos);
                charPos++;

                if (dividerChar == '.') {
                    divider |= DOT_DIVIDER;
                }
                else if (dividerChar == '-') {
                    divider |= MINUS_DIVIDER;
                }
                else if (dividerChar == '_') {
                    divider |= UNDERSCORE_DIVIDER;
                }
                else {
                    divider |= OTHER_DIVIDER;
                }

            }
            else {
                dividerChar = 0;
                divider |= END_OF_VERSION;
            }

            final String element = StringUtils.trimToEmpty(rawElement);

            if (!StringUtils.isBlank(element)) {
                long flags = ALL_NUMBERS | ALL_LETTERS | ALL_OTHER;
                final char firstChar = element.charAt(0);
                final char lastChar = element.charAt(element.length() - 1);

                if (Character.isDigit(firstChar)) {
                    flags |= STARTS_WITH_NUMBERS;
                }
                else if (Character.isLetter(firstChar)) {
                    flags |= STARTS_WITH_LETTERS;
                }
                else {
                    flags |= STARTS_WITH_OTHER;
                }

                if (Character.isDigit(lastChar)) {
                    flags |= ENDS_WITH_NUMBERS;
                }
                else if (Character.isLetter(lastChar)) {
                    flags |= ENDS_WITH_LETTERS;
                }
                else {
                    flags |= ENDS_WITH_OTHER;
                }

                for (int j = 0; j < element.length(); j++) {

                    if (Character.isDigit(element.charAt(j))) {
                        flags &= ~(ALL_LETTERS | ALL_OTHER);
                        flags |= NUMBERS;
                    }
                    else if (Character.isLetter(element.charAt(j))) {
                        flags &= ~(ALL_NUMBERS | ALL_OTHER);
                        flags |= LETTERS;
                    }
                    else {
                        flags &= ~(ALL_LETTERS | ALL_NUMBERS);
                        flags |= OTHER;
                    }
                }

                versionElements[resultPos++] = new VersionElement(element, flags, divider, dividerChar);
            }
        }

        this.elementCount = resultPos;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public String[] getRawElements()
    {
        return rawElements;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public VersionElement[] getVersionElements()
    {
        return versionElements;
    }

    public int getElementCount()
    {
        return elementCount;
    }

    public String getRawVersion()
    {
        return rawVersion;
    }

    public String getSelectedVersion()
    {
        return selectedVersion;
    }

    public String toString()
    {
        return getSelectedVersion();
    }

    public boolean equals(final Object other)
    {
        if (other == null || other.getClass() != this.getClass()) {
            return false;
        }

        if (other == this) {
            return true;
        }

        Version castOther = (Version) other;
        return new EqualsBuilder().append(rawVersion, castOther.rawVersion)
            .append(selectedVersion, castOther.selectedVersion)
            .append(rawElements, castOther.rawElements)
            .append(versionElements, castOther.versionElements)
            .append(elementCount, castOther.elementCount)
            .isEquals();
    }



    private transient int hashCode;

    public int hashCode()
    {
        if (hashCode == 0) {
            hashCode = new HashCodeBuilder().append(rawVersion).append(selectedVersion).append(rawElements).append(versionElements).append(elementCount).toHashCode();
        }
        return hashCode;
    }

    public int compareTo(Object o)
    {
        return selectedVersion.compareTo(((Version) o).getSelectedVersion());
    }
}

