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
package org.neo4j.kernel.stresstests.transaction.checkpoint.tracers;

import org.neo4j.io.pagecache.tracing.PageCacheTracer;
import org.neo4j.kernel.impl.transaction.tracing.CheckPointTracer;
import org.neo4j.kernel.impl.transaction.tracing.TransactionTracer;
import org.neo4j.scheduler.JobScheduler;
import org.neo4j.kernel.monitoring.Monitors;
import org.neo4j.kernel.monitoring.tracing.TracerFactory;
import org.neo4j.logging.Log;
import org.neo4j.time.SystemNanoClock;

public class TimerTracerFactory implements TracerFactory
{
    private TimerTransactionTracer timerTransactionTracer = new TimerTransactionTracer();

    @Override
    public String getImplementationName()
    {
        return "timer";
    }

    @Override
    public PageCacheTracer createPageCacheTracer( Monitors monitors, JobScheduler jobScheduler, SystemNanoClock clock,
            Log log )
    {
        return PageCacheTracer.NULL;
    }

    @Override
    public TransactionTracer createTransactionTracer( Monitors monitors, JobScheduler jobScheduler )
    {
        return timerTransactionTracer;
    }

    @Override
    public CheckPointTracer createCheckPointTracer( Monitors monitors, JobScheduler jobScheduler )
    {
        return timerTransactionTracer;
    }
}
