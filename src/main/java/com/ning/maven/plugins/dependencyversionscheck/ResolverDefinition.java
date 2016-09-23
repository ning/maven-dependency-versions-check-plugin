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

package com.ning.maven.plugins.dependencyversionscheck;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Represents a "resolver" element in the configuration section.
 */
public class ResolverDefinition
{
    private String id;
    private String strategyName;
    private String [] includes;


    public void setId(String id)
    {
        this.id = id;
    }

    public void setStrategyName(String strategyName)
    {
        this.strategyName = strategyName;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setIncludes(String[] includes)
    {
        this.includes = includes;
    }

    public String getId()
    {
        return id;
    }

    public String getStrategyName()
    {
        return strategyName;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public String[] getIncludes()
    {
        return includes;
    }

    private transient String toString;

    public String toString()
    {
        if (toString == null) {
            toString = new ToStringBuilder(this).append("id", id).append("strategyName", strategyName).append("includes", includes).toString();
        }
        return toString;
    }

    public boolean equals(final Object other)
    {
        if (!(other instanceof ResolverDefinition))
            return false;
        ResolverDefinition castOther = (ResolverDefinition) other;
        return new EqualsBuilder().append(id, castOther.id).append(strategyName, castOther.strategyName).append(includes, castOther.includes).isEquals();
    }

    public int hashCode()
    {
        return new HashCodeBuilder().append(id).append(strategyName).append(includes).toHashCode();
    }
}


