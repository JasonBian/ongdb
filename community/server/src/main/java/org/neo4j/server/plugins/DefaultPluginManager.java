/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB.
 *
 * ONgDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.helpers.collection.Pair;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;
import org.neo4j.server.rest.repr.BadInputException;
import org.neo4j.server.rest.repr.ExtensionPointRepresentation;
import org.neo4j.server.rest.repr.Representation;

/**
 * @deprecated Server plugins are deprecated for removal in the next major release. Please use unmanaged extensions instead.
 */
@Deprecated
public final class DefaultPluginManager implements PluginManager
{
    private final Map<String/*name*/,ServerExtender> extensions = new HashMap<>();

    @Deprecated
    public DefaultPluginManager( LogProvider logProvider )
    {
        Map<String,Pair<ServerPlugin,ServerExtender>> extensions = new HashMap<>();
        Log log = logProvider.getLog( getClass() );
        Iterable<ServerPlugin> loadedPlugins = ServerPlugin.load();
        for ( ServerPlugin plugin : loadedPlugins )
        {
            PluginPointFactory factory = new PluginPointFactoryImpl();
            final ServerExtender extender = new ServerExtender( factory );
            try
            {
                plugin.loadServerExtender( extender );
            }
            catch ( Exception | LinkageError ex )
            {
                log.warn( "Failed to load plugin [%s]: %s", plugin.toString(), ex.getMessage() );
                continue;
            }
            Pair<ServerPlugin,ServerExtender> old = extensions.put( plugin.name, Pair.of( plugin, extender ) );
            if ( old != null )
            {
                log.warn( String.format( "Extension naming conflict \"%s\" between \"%s\" and \"%s\"", plugin.name,
                        old.first().getClass(), plugin.getClass() ) );
            }
        }
        for ( Pair<ServerPlugin,ServerExtender> extension : extensions.values() )
        {
            log.info( String.format( "Loaded server plugin \"%s\"", extension.first().name ) );
            for ( PluginPoint point : extension.other().all() )
            {
                log.info( String.format( "  %s.%s: %s", point.forType().getSimpleName(), point.name(),
                        point.getDescription() ) );
            }
            this.extensions.put( extension.first().name, extension.other() );
        }
    }

    @Deprecated
    @Override
    public Map<String,List<String>> getExensionsFor( Class<?> type )
    {
        Map<String,List<String>> result = new HashMap<>();
        for ( Map.Entry<String,ServerExtender> extension : extensions.entrySet() )
        {
            List<String> methods = new ArrayList<>();
            for ( PluginPoint method : extension.getValue()
                    .getExtensionsFor( type ) )
            {
                methods.add( method.name() );
            }
            if ( !methods.isEmpty() )
            {
                result.put( extension.getKey(), methods );
            }
        }
        return result;
    }

    private PluginPoint extension( String name, Class<?> type, String method ) throws PluginLookupException
    {
        ServerExtender extender = extensions.get( name );
        if ( extender == null )
        {
            throw new PluginLookupException( "No such ServerPlugin: \"" + name + "\"" );
        }
        return extender.getExtensionPoint( type, method );
    }

    @Deprecated
    @Override
    public ExtensionPointRepresentation describe( String name, Class<?> type, String method )
            throws PluginLookupException
    {
        return describe( extension( name, type, method ) );
    }

    private ExtensionPointRepresentation describe( PluginPoint extension )
    {
        ExtensionPointRepresentation representation = new ExtensionPointRepresentation( extension.name(),
                extension.forType(), extension.getDescription() );
        extension.describeParameters( representation );
        return representation;
    }

    @Deprecated
    @Override
    public List<ExtensionPointRepresentation> describeAll( String name ) throws PluginLookupException
    {
        ServerExtender extender = extensions.get( name );
        if ( extender == null )
        {
            throw new PluginLookupException( "No such ServerPlugin: \"" + name + "\"" );
        }
        List<ExtensionPointRepresentation> result = new ArrayList<>();
        for ( PluginPoint plugin : extender.all() )
        {
            result.add( describe( plugin ) );
        }
        return result;
    }

    @Deprecated
    @Override
    public <T> Representation invoke( GraphDatabaseAPI graphDb, String name, Class<T> type, String method,
            T context, ParameterList params ) throws PluginLookupException, BadInputException,
            PluginInvocationFailureException, BadPluginInvocationException
    {
        PluginPoint plugin = extension( name, type, method );
        try
        {
            return plugin.invoke( graphDb, context, params );
        }
        catch ( BadInputException | PluginInvocationFailureException | BadPluginInvocationException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new PluginInvocationFailureException( e );
        }
    }

    @Deprecated
    @Override
    public Set<String> extensionNames()
    {
        return Collections.unmodifiableSet( extensions.keySet() );
    }
}
