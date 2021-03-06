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
package org.neo4j.causalclustering.core.consensus.term;

import java.io.IOException;

import org.neo4j.causalclustering.core.consensus.log.monitoring.RaftTermMonitor;
import org.neo4j.causalclustering.core.state.storage.StateStorage;
import org.neo4j.kernel.monitoring.Monitors;

public class MonitoredTermStateStorage implements StateStorage<TermState>
{
    private String TERM_TAG = "term";

    private final StateStorage<TermState> delegate;
    private final RaftTermMonitor termMonitor;

    public MonitoredTermStateStorage( StateStorage<TermState> delegate, Monitors monitors )
    {
        this.delegate = delegate;
        this.termMonitor = monitors.newMonitor( RaftTermMonitor.class, getClass().getName(), TERM_TAG );
    }

    @Override
    public TermState getInitialState()
    {
        return delegate.getInitialState();
    }

    @Override
    public void persistStoreData( TermState state ) throws IOException
    {
        delegate.persistStoreData( state );
        termMonitor.term( state.currentTerm() );
    }
}
