/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) as found
 * in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package org.neo4j.server.rest.causalclustering;

import org.neo4j.server.rest.repr.MappingRepresentation;
import org.neo4j.server.rest.repr.MappingSerializer;

import static org.neo4j.server.rest.causalclustering.CausalClusteringService.AVAILABLE;
import static org.neo4j.server.rest.causalclustering.CausalClusteringService.READ_ONLY;
import static org.neo4j.server.rest.causalclustering.CausalClusteringService.DESCRIPTION;
import static org.neo4j.server.rest.causalclustering.CausalClusteringService.WRITABLE;

public class CausalClusteringDiscovery extends MappingRepresentation
{
    private static final String DISCOVERY_REPRESENTATION_TYPE = "discovery";

    private final String basePath;

    CausalClusteringDiscovery( String basePath )
    {
        super( DISCOVERY_REPRESENTATION_TYPE );
        this.basePath = basePath;
    }

    @Override
    protected void serialize( MappingSerializer serializer )
    {
        serializer.putRelativeUri( AVAILABLE, basePath + "/" + AVAILABLE );
        serializer.putRelativeUri( READ_ONLY, basePath + "/" + READ_ONLY );
        serializer.putRelativeUri( WRITABLE, basePath + "/" + WRITABLE );
        serializer.putRelativeUri( DESCRIPTION, basePath + "/" + DESCRIPTION );
    }
}
