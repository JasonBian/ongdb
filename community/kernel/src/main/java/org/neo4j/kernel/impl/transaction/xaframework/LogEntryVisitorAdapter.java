/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
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
package org.neo4j.kernel.impl.transaction.xaframework;

import java.util.List;

import org.neo4j.helpers.Function;
import org.neo4j.kernel.impl.nioneo.xa.Command;

public class LogEntryVisitorAdapter implements Function<List<LogEntry>, List<LogEntry>>
{
    private final TransactionInterceptor interceptor;

    public LogEntryVisitorAdapter( TransactionInterceptor interceptor )
    {
        this.interceptor = interceptor;
    }

    @Override
    public List<LogEntry> apply( List<LogEntry> entries )
    {
        for ( LogEntry entry : entries )
        {
            if ( entry instanceof LogEntry.Command )
            {
                LogEntry.Command commandEntry = (LogEntry.Command) entry;
                if ( commandEntry.getXaCommand() instanceof Command )
                {
                    ( (Command) commandEntry.getXaCommand() ).accept( interceptor );
                }
            }
            else if ( entry instanceof LogEntry.Start )
            {
                interceptor.setStartEntry( (LogEntry.Start) entry );
            }
            else if ( entry instanceof LogEntry.Commit )
            {
                interceptor.setCommitEntry( (LogEntry.Commit) entry );
            }
        }
        interceptor.complete();
        return entries;
    }
}
