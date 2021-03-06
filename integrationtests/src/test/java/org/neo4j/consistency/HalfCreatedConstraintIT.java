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
package org.neo4j.consistency;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.neo4j.graphdb.DependencyResolver;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.EnterpriseGraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.internal.kernel.api.exceptions.schema.SchemaKernelException;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.schema.LabelSchemaDescriptor;
import org.neo4j.kernel.api.schema.SchemaDescriptorFactory;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.test.rule.TestDirectory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HalfCreatedConstraintIT
{
    @Rule
    public final TestDirectory testDirectory = TestDirectory.testDirectory();

    @Test
    public void uniqueIndexWithoutOwningConstraintIsIgnoredDuringCheck() throws ConsistencyCheckTool.ToolFailureException, IOException
    {
        File databaseDir = testDirectory.databaseDir();
        Label marker = Label.label( "MARKER" );
        String property = "property";

        GraphDatabaseService database = new EnterpriseGraphDatabaseFactory().newEmbeddedDatabase( databaseDir );
        try
        {
            createNodes( marker, property, database );
            addIndex( database );
            waitForIndexPopulationFailure( database );
        }
        catch ( SchemaKernelException e )
        {
            e.printStackTrace();
        }
        finally
        {
            database.shutdown();
        }

        ConsistencyCheckService.Result checkResult = ConsistencyCheckTool.runConsistencyCheckTool( new String[]{databaseDir.getAbsolutePath()},
                emptyPrintStream(), emptyPrintStream() );
        assertTrue( String.join( System.lineSeparator(), Files.readAllLines( checkResult.reportFile().toPath() ) ), checkResult.isSuccessful() );
    }

    private static void waitForIndexPopulationFailure( GraphDatabaseService database )
    {
        try ( Transaction ignored = database.beginTx() )
        {
            database.schema().awaitIndexesOnline( 10, TimeUnit.MINUTES );
            fail( "Unique index population should fail." );
        }
        catch ( IllegalStateException e )
        {
            // TODO: Do we really need to verify the message since we know an IllegalStateException was caught? If so, this needs to be updated.
            // assertEquals( "Index entered a FAILED state. Please see database logs.", e.getMessage() );
        }
    }

    private static void addIndex( GraphDatabaseService database ) throws SchemaKernelException
    {
        try ( Transaction transaction = database.beginTx() )
        {
            DependencyResolver resolver = ((GraphDatabaseAPI) database).getDependencyResolver();
            ThreadToStatementContextBridge statementBridge = resolver.provideDependency( ThreadToStatementContextBridge.class ).get();
            KernelTransaction kernelTransaction = statementBridge.getKernelTransactionBoundToThisThread( true );
            LabelSchemaDescriptor descriptor = SchemaDescriptorFactory.forLabel( 0, 0 );
            Config config = resolver.resolveDependency( Config.class );
            kernelTransaction.indexUniqueCreate( descriptor, config.get( GraphDatabaseSettings.default_schema_provider ) );
            transaction.success();
        }
    }

    private static void createNodes( Label marker, String property, GraphDatabaseService database )
    {
        try ( Transaction transaction = database.beginTx() )
        {
            for ( int i = 0; i < 10; i++ )
            {
                Node node = database.createNode( marker );
                node.setProperty( property, "a" );
            }
            transaction.success();
        }
    }

    private static PrintStream emptyPrintStream()
    {
        return new PrintStream( org.neo4j.io.NullOutputStream.NULL_OUTPUT_STREAM );
    }
}
