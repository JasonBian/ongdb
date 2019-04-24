/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
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
package org.neo4j.internal.schema;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;

import org.neo4j.common.EntityType;
import org.neo4j.common.TokenNameLookup;
import org.neo4j.lock.ResourceType;
import org.neo4j.lock.ResourceTypes;
import org.neo4j.token.api.TokenIdPrettyPrinter;

import static java.util.Objects.requireNonNull;
import static org.neo4j.common.EntityType.NODE;
import static org.neo4j.common.EntityType.RELATIONSHIP;
import static org.neo4j.internal.schema.IndexKind.GENERAL;
import static org.neo4j.internal.schema.IndexType.FULLTEXT;
import static org.neo4j.internal.schema.PropertySchemaType.COMPLETE_ALL_TOKENS;
import static org.neo4j.internal.schema.PropertySchemaType.PARTIAL_ANY_TOKEN;

public final class SchemaDescriptorImplementation implements SchemaDescriptor, LabelSchemaDescriptor, RelationTypeSchemaDescriptor, FulltextSchemaDescriptor
{
    private final IndexType indexType;
    private final EntityType entityType;
    private final PropertySchemaType propertySchemaType;
    private final IndexConfig indexConfig;
    private final int[] entityTokens;
    private final int[] propertyIds;
    /**
     * {@code true} if this schema matches the {@link LabelSchemaDescriptor} structure.
     */
    private final boolean archetypalLabelSchema;
    /**
     * {@code true} if this schema matches the {@link RelationTypeSchemaDescriptor} structure.
     */
    private final boolean archetypalRelationshipTypeSchema;
    /**
     * {@code true} if this schema matches the {@link FulltextSchemaDescriptor} structure.
     */
    private final boolean archetypalFulltextSchema;

    SchemaDescriptorImplementation( IndexType indexType, EntityType entityType, PropertySchemaType propertySchemaType, IndexConfig indexConfig,
            int[] entityTokens, int[] propertyIds )
    {
        this.indexType = requireNonNull( indexType, "IndexType cannot be null." );
        this.entityType = requireNonNull( entityType, "EntityType cannot be null." );
        this.propertySchemaType = requireNonNull( propertySchemaType, "PropertySchemaType cannot be null." );
        this.entityTokens = requireNonNull( entityTokens, "Entity tokens array cannot be null." );
        this.propertyIds = requireNonNull( propertyIds, "Property key ids array cannot be null." );
        this.indexConfig = requireNonNull( indexConfig, "IndexConfig cannot be null." );
        if ( entityTokens.length == 0 )
        {
            throw new IllegalArgumentException( "Schema descriptor must have at least one " + (entityType == NODE ? "label." : "relationship type.") );
        }
        if ( propertyIds.length == 0 )
        {
            throw new IllegalArgumentException( "Schema descriptor must have at least one property key id." );
        }

        boolean generalSingleEntity = indexType.getKind() == GENERAL && entityTokens.length == 1 && propertySchemaType == COMPLETE_ALL_TOKENS;

        this.archetypalLabelSchema = entityType == NODE && generalSingleEntity;
        this.archetypalRelationshipTypeSchema = entityType == RELATIONSHIP && generalSingleEntity;
        this.archetypalFulltextSchema = indexType == FULLTEXT && propertySchemaType == PARTIAL_ANY_TOKEN;
    }

    @Override
    public LabelSchemaDescriptor asLabelSchemaDescriptor()
    {
        if ( !archetypalLabelSchema )
        {
            throw cannotCastException( "LabelSchemaDescriptor" );
        }
        return this;
    }

    @Override
    public RelationTypeSchemaDescriptor asRelationshipTypeSchemaDescriptor()
    {
        if ( !archetypalRelationshipTypeSchema )
        {
            throw cannotCastException( "RelationTypeSchemaDescriptor" );
        }
        return this;
    }

    @Override
    public FulltextSchemaDescriptor asFulltextSchemaDescriptor()
    {
        if ( !archetypalFulltextSchema )
        {
            throw cannotCastException( "FulltextSchemaDescriptor" );
        }
        return this;
    }

    private IllegalStateException cannotCastException( String descriptorType )
    {
        return new IllegalStateException( "Cannot cast this schema to a " + descriptorType + " because it does not match that structure: " + this + "." );
    }

    @Override
    public boolean isAffected( long[] entityTokenIds )
    {
        for ( int id : entityTokens )
        {
            if ( ArrayUtils.contains( entityTokenIds, id ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public <R> R computeWith( SchemaComputer<R> computer )
    {
        if ( archetypalLabelSchema )
        {
            return computer.computeSpecific( this.asLabelSchemaDescriptor() );
        }
        if ( archetypalRelationshipTypeSchema )
        {
            return computer.computeSpecific( this.asRelationshipTypeSchemaDescriptor() );
        }
        return computer.computeSpecific( (SchemaDescriptor) this );
    }

    @Override
    public void processWith( SchemaProcessor processor )
    {
        if ( archetypalLabelSchema )
        {
            processor.processSpecific( this.asLabelSchemaDescriptor() );
        }
        else if ( archetypalRelationshipTypeSchema )
        {
            processor.processSpecific( this.asRelationshipTypeSchemaDescriptor() );
        }
        else
        {
            processor.processSpecific( (SchemaDescriptor) this );
        }
    }

    @Override
    public String toString()
    {
        return "SchemaDescriptor[" + userDescription( TokenNameLookup.idTokenNameLookup ) + "]";
    }

    @Override
    public String userDescription( TokenNameLookup tokenNameLookup )
    {
        return entityType + ":" + String.join( ", ", tokenNameLookup.entityTokensGetNames( entityType, entityTokens ) ) + "(" +
                TokenIdPrettyPrinter.niceProperties( tokenNameLookup, propertyIds ) + ")";
    }

    @Override
    public int[] getPropertyIds()
    {
        return propertyIds;
    }

    @Override
    public int[] getEntityTokenIds()
    {
        return entityTokens;
    }

    @Override
    public ResourceType keyType()
    {
        return entityType == EntityType.NODE ? ResourceTypes.LABEL : ResourceTypes.RELATIONSHIP_TYPE;
    }

    @Override
    public EntityType entityType()
    {
        return entityType;
    }

    @Override
    public PropertySchemaType propertySchemaType()
    {
        return propertySchemaType;
    }

    @Override
    public SchemaDescriptor schema()
    {
        return this;
    }

    @Override
    public IndexType getIndexType()
    {
        return indexType;
    }

    @Override
    public IndexConfig getIndexConfig()
    {
        return indexConfig;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !(o instanceof SchemaDescriptorImplementation) )
        {
            return false;
        }
        SchemaDescriptorImplementation that = (SchemaDescriptorImplementation) o;
        return indexType == that.indexType && entityType == that.entityType && propertySchemaType == that.propertySchemaType &&
                Arrays.equals( entityTokens, that.entityTokens ) && Arrays.equals( propertyIds, that.propertyIds );
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash( indexType, entityType, propertySchemaType );
        result = 31 * result + Arrays.hashCode( entityTokens );
        result = 31 * result + Arrays.hashCode( propertyIds );
        return result;
    }
}
