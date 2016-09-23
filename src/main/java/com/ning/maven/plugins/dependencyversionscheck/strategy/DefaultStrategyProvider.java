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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation for {@link com.ning.maven.plugins.dependencyversionscheck.strategy.StrategyProvider}.
 *
 * @plexus.component role="com.ning.maven.plugins.dependencyversionscheck.strategy.StrategyProvider"
 */
public class DefaultStrategyProvider implements StrategyProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(DefaultStrategyProvider.class);

    /**
     * @plexus.requirement role="com.ning.maven.plugins.dependencyversionscheck.strategy.Strategy"
     */
    protected List resolverDefinitions;

    private Map resolvers = null;

    public Map getStrategies()
    {
        if (resolvers == null) {
            Map newResolvers = new HashMap();
            if (!CollectionUtils.isEmpty(resolverDefinitions)) {
                for (Iterator it = resolverDefinitions.iterator(); it.hasNext();) {
                    final Strategy resolver = (Strategy) it.next();
                    final String name = resolver.getName().toLowerCase(Locale.ENGLISH);

                    LOG.debug("Adding {} as resolver.", name);
                    newResolvers.put(name, resolver);
                }
            }
            resolvers = newResolvers;
        }
        return resolvers;
    }

    public Strategy forName(final String name)
    {
        if (name == null) {
            return null;
        }
        final Map strategies = getStrategies();
        return (Strategy) strategies.get(name.toLowerCase(Locale.ENGLISH));
    }
}

