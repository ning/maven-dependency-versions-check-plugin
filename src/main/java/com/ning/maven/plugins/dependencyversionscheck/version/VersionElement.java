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

package com.ning.maven.plugins.dependencyversionscheck.version;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Represents a single element of a version number.
 */
public final class VersionElement
{
    /** The raw value of the element. */
    private final String element;

    /** OR'ed list of Flags in Version. */
    private final long flags;

    /** OR'ed list of Divider flags in Version. */
    private final long divider;

    /** The raw value of the divider char (the character immediately following the version element) or 0 if there is no divider char. */
    private final char dividerChar;

    public VersionElement(final String element, final long flags, final long divider, final char dividerChar)
    {
        this.element = element;
        this.flags = flags;
        this.divider = divider;
        this.dividerChar = dividerChar;
    }

    public String getElement()
    {
        return element;
    }

    public long getFlags()
    {
        return flags;
    }

    public long getDivider()
    {
        return divider;
    }

    public char getDividerChar()
    {
        return dividerChar;
    }

    public boolean hasNumbers()
    {
        return (flags & Version.NUMBERS) != 0;
    }

    public boolean isNumber()
    {
        return (flags & Version.ALL_NUMBERS) != 0;
    }

    public boolean hasLetters()
    {
        return (flags & Version.LETTERS) != 0;
    }

    public boolean hasDot()
    {
        return (divider & Version.DOT_DIVIDER) != 0;
    }

    public boolean hasEndOfVersion()
    {
        return (divider & Version.END_OF_VERSION) != 0;
    }

    public long getNumber()
    {
        if (!hasNumbers()) {
            return 0L;
        }
        else {

            final String result;
            if (isNumber()) {
                result = element;
            }
            else {
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < element.length(); i++) {
                    if (Character.isDigit(element.charAt(i))) {
                        sb.append(element.charAt(i));
                    }
                }
                result = sb.toString();
            }

            return Long.parseLong(result, 10);
        }
    }

    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(element);
        if ((flags & Version.END_OF_VERSION) == 0) {
            sb.append(dividerChar);
        }
        return sb.toString();
    }

    public boolean equals(final Object other)
    {
        if (other == null || other.getClass() != this.getClass()) {
            return false;
        }

        if (other == this) {
            return true;
        }

        VersionElement castOther = (VersionElement) other;
        return new EqualsBuilder().append(element, castOther.element).append(flags, castOther.flags).append(divider, castOther.divider).append(dividerChar, castOther.dividerChar).isEquals();
    }

    private transient int hashCode;

    public int hashCode()
    {
        if (hashCode == 0) {
            hashCode = new HashCodeBuilder().append(element).append(flags).append(divider).append(dividerChar).toHashCode();
        }
        return hashCode;
    }
}
