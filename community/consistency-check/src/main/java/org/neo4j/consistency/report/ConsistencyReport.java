/*
 * Copyright (c) "Neo4j"
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
package org.neo4j.consistency.report;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.annotations.documented.Documented;
import org.neo4j.annotations.documented.Warning;
import org.neo4j.consistency.RecordType;
import org.neo4j.consistency.store.synthetic.CountsEntry;
import org.neo4j.consistency.store.synthetic.IndexEntry;
import org.neo4j.consistency.store.synthetic.TokenScanDocument;
import org.neo4j.internal.schema.IndexDescriptor;
import org.neo4j.internal.schema.SchemaRule;
import org.neo4j.kernel.impl.store.record.DynamicRecord;
import org.neo4j.kernel.impl.store.record.LabelTokenRecord;
import org.neo4j.kernel.impl.store.record.NodeRecord;
import org.neo4j.kernel.impl.store.record.PropertyBlock;
import org.neo4j.kernel.impl.store.record.PropertyKeyTokenRecord;
import org.neo4j.kernel.impl.store.record.PropertyRecord;
import org.neo4j.kernel.impl.store.record.RelationshipGroupRecord;
import org.neo4j.kernel.impl.store.record.RelationshipRecord;
import org.neo4j.kernel.impl.store.record.RelationshipTypeTokenRecord;
import org.neo4j.kernel.impl.store.record.SchemaRecord;

public interface ConsistencyReport
{
    interface Reporter
    {
        SchemaConsistencyReport forSchema( SchemaRecord schema );

        NodeConsistencyReport forNode( NodeRecord node );

        RelationshipConsistencyReport forRelationship( RelationshipRecord relationship );

        PropertyConsistencyReport forProperty( PropertyRecord property );

        RelationshipTypeConsistencyReport forRelationshipTypeName( RelationshipTypeTokenRecord relationshipType );

        LabelTokenConsistencyReport forLabelName( LabelTokenRecord label );

        PropertyKeyTokenConsistencyReport forPropertyKey( PropertyKeyTokenRecord key );

        DynamicConsistencyReport forDynamicBlock( RecordType type, DynamicRecord record );

        DynamicLabelConsistencyReport forDynamicLabelBlock( RecordType type, DynamicRecord record );

        LabelScanConsistencyReport forNodeLabelScan( TokenScanDocument document );

        RelationshipTypeScanConsistencyReport forRelationshipTypeScan( TokenScanDocument document );

        IndexConsistencyReport forIndexEntry( IndexEntry entry );

        RelationshipGroupConsistencyReport forRelationshipGroup( RelationshipGroupRecord group );

        CountsConsistencyReport forCounts( CountsEntry countsEntry );
    }

    interface PrimitiveConsistencyReport extends ConsistencyReport
    {
        @Documented( "The referenced property record is not in use." )
        void propertyNotInUse( PropertyRecord property );

        @Documented( "The referenced property record is not the first in its property chain." )
        void propertyNotFirstInChain( PropertyRecord property );

        @Documented( "The referenced property is owned by another Node." )
        void multipleOwners( NodeRecord node );

        @Documented( "The referenced property is owned by another Relationship." )
        void multipleOwners( RelationshipRecord relationship );

        @Documented( "The property chain contains multiple properties that have the same property key id, " +
                "which means that the entity has at least one duplicate property." )
        void propertyKeyNotUniqueInChain();

        @Documented( "The property chain does not contain a property that is mandatory for this entity." )
        void missingMandatoryProperty( int key );

        @Documented( "The property record points to a previous record in the chain, making it a circular reference." )
        void propertyChainContainsCircularReference( PropertyRecord propertyRecord );

        @Documented( "This entity was not found in the expected index." )
        void notIndexed( IndexDescriptor index, Object[] propertyValues );

        @Documented( "This entity was found in the expected index, although multiple times" )
        void indexedMultipleTimes( IndexDescriptor index, Object[] propertyValues, long count );

        @Documented( "There is another entity in the unique index with the same property value(s)." )
        void uniqueIndexNotUnique( IndexDescriptor index, Object[] propertyValues, long duplicateEntityId );
    }

    interface NeoStoreConsistencyReport extends PrimitiveConsistencyReport
    {
    }

    interface SchemaConsistencyReport extends ConsistencyReport
    {
        @Documented( "The label token record referenced from the schema is not in use." )
        void labelNotInUse( LabelTokenRecord label );

        @Documented( "The relationship type token record referenced from the schema is not in use." )
        void relationshipTypeNotInUse( RelationshipTypeTokenRecord relationshipType );

        @Documented( "The property key token record is not in use." )
        void propertyKeyNotInUse( PropertyKeyTokenRecord propertyKey );

        @Documented( "The uniqueness constraint does not reference back to the given record" )
        void uniquenessConstraintNotReferencingBack( SchemaRecord ruleRecord );

        @Documented( "The uniqueness constraint reference an index of a different index type" )
        void uniquenessConstraintReferencingIndexOfWrongType( SchemaRecord ruleRecord );

        @Documented( "The constraint index does not reference back to the given record" )
        void constraintIndexRuleNotReferencingBack( SchemaRecord ruleRecord );

        @Documented( "The constraint index name is different from the name of its owning constraint" )
        void constraintIndexNameDoesNotMatchConstraintName( SchemaRecord ruleRecord, String indexName, String constraintName );

        @Documented( "This record is required to reference some other record of the given kind but no such obligation was found" )
        void missingObligation( String kind );

        @Documented( "This record requires some other record to reference back to it but there already was such " +
                "a conflicting obligation created by the record given as a parameter" )
        void duplicateObligation( SchemaRecord record );

        @Documented( "This record contains a schema rule which has the same content as the schema rule contained " +
                "in the record given as parameter" )
        void duplicateRuleContent( SchemaRecord record );

        @Documented( "This record contains a schema rule which has the same name as the schema rule contained " +
                "in the record given as parameter" )
        void duplicateRuleName( SchemaRecord record, String name );

        @Documented( "The schema rule is malformed (not deserializable)" )
        void malformedSchemaRule();

        @Documented( "The schema rule is of an unrecognized type" )
        void unsupportedSchemaRuleType( Class<? extends SchemaRule> ruleType );

        @Warning
        @Documented( "The schema rule has a reference to another schema rule that is not online." )
        void schemaRuleNotOnline( SchemaRule schemaRule );
    }

    interface NodeConsistencyReport extends PrimitiveConsistencyReport
    {
        @Documented( "The referenced relationship record is not in use." )
        void relationshipNotInUse( RelationshipRecord referenced );

        @Documented( "The referenced relationship record is a relationship between two other nodes." )
        void relationshipForOtherNode( RelationshipRecord relationship );

        @Documented( "The referenced relationship record is not the first in the relationship chain where this node " +
                "is source." )
        void relationshipNotFirstInSourceChain( RelationshipRecord relationship );

        @Documented( "The referenced relationship record is not the first in the relationship chain where this node " +
                "is target." )
        void relationshipNotFirstInTargetChain( RelationshipRecord relationship );

        @Documented( "The label token record referenced from a node record is not in use." )
        void labelNotInUse( LabelTokenRecord label );

        @Documented( "The label token record is referenced twice from the same node." )
        void labelDuplicate( long labelId );

        @Documented( "The label id array is not ordered" )
        void labelsOutOfOrder( long largest, long smallest );

        @Documented( "The dynamic label record is not in use." )
        void dynamicLabelRecordNotInUse( DynamicRecord record );

        @Documented( "This record points to a next record that was already part of this dynamic record chain." )
        void dynamicRecordChainCycle( DynamicRecord nextRecord );

        @Override
        @Documented( "This node was not found in the expected index." )
        void notIndexed( IndexDescriptor index, Object[] propertyValues );

        @Override
        @Documented( "This node was found in the expected index, although multiple times" )
        void indexedMultipleTimes( IndexDescriptor index, Object[] propertyValues, long count );

        @Override
        @Documented( "There is another node in the unique index with the same property value(s)." )
        void uniqueIndexNotUnique( IndexDescriptor index, Object[] propertyValues, long duplicateNodeId );

        @Documented( "The referenced relationship group record is not in use." )
        void relationshipGroupNotInUse( RelationshipGroupRecord group );

        @Documented( "The first relationship group record has another node set as owner." )
        void relationshipGroupHasOtherOwner( RelationshipGroupRecord group );

        @Documented( "The label does not exist" )
        void illegalLabel();
    }

    interface RelationshipConsistencyReport
            extends PrimitiveConsistencyReport
    {
        @Documented( "The relationship record is not in use, but referenced from relationships chain." )
        void notUsedRelationshipReferencedInChain( RelationshipRecord relationshipRecord );

        @Documented( "The relationship type field has an illegal value." )
        void illegalRelationshipType();

        @Documented( "The relationship type record is not in use." )
        void relationshipTypeNotInUse( RelationshipTypeTokenRecord relationshipType );

        @Documented( "The source node field has an illegal value." )
        void illegalSourceNode();

        @Documented( "The target node field has an illegal value." )
        void illegalTargetNode();

        @Documented( "The source node is not in use." )
        void sourceNodeNotInUse( NodeRecord node );

        @Documented( "The target node is not in use." )
        void targetNodeNotInUse( NodeRecord node );

        @Documented( "This record should be the first in the source chain, but the source node does not reference this record." )
        void sourceNodeDoesNotReferenceBack( NodeRecord node );

        @Documented( "This record should be the first in the target chain, but the target node does not reference this record." )
        void targetNodeDoesNotReferenceBack( NodeRecord node );

        @Documented( "The source node does not have a relationship chain." )
        void sourceNodeHasNoRelationships( NodeRecord source );

        @Documented( "The target node does not have a relationship chain." )
        void targetNodeHasNoRelationships( NodeRecord source );

        @Documented( "The previous record in the source chain is a relationship between two other nodes." )
        void sourcePrevReferencesOtherNodes( RelationshipRecord relationship );

        @Documented( "The next record in the source chain is a relationship between two other nodes." )
        void sourceNextReferencesOtherNodes( RelationshipRecord relationship );

        @Documented( "The previous record in the target chain is a relationship between two other nodes." )
        void targetPrevReferencesOtherNodes( RelationshipRecord relationship );

        @Documented( "The next record in the target chain is a relationship between two other nodes." )
        void targetNextReferencesOtherNodes( RelationshipRecord relationship );

        @Documented( "The previous record in the source chain does not have this record as its next record." )
        void sourcePrevDoesNotReferenceBack( RelationshipRecord relationship );

        @Documented( "The next record in the source chain does not have this record as its previous record." )
        void sourceNextDoesNotReferenceBack( RelationshipRecord relationship );

        @Documented( "The previous record in the target chain does not have this record as its next record." )
        void targetPrevDoesNotReferenceBack( RelationshipRecord relationship );

        @Documented( "The next record in the target chain does not have this record as its previous record." )
        void targetNextDoesNotReferenceBack( RelationshipRecord relationship );

        @Override
        @Documented( "This relationship was not found in the expected index." )
        void notIndexed( IndexDescriptor index, Object[] propertyValues );

        @Override
        @Documented( "This relationship was found in the expected index, although multiple times" )
        void indexedMultipleTimes( IndexDescriptor index, Object[] propertyValues, long count );

        @Override
        @Documented( "There is another relationship in the unique index with the same property value(s)." )
        void uniqueIndexNotUnique( IndexDescriptor index, Object[] propertyValues, long duplicateEntityId );
    }

    interface PropertyConsistencyReport extends ConsistencyReport
    {
        @Documented( "The property key as an invalid value." )
        void invalidPropertyKey( PropertyBlock block );

        @Documented( "The key for this property is not in use." )
        void keyNotInUse( PropertyBlock block, PropertyKeyTokenRecord key );

        @Documented( "The previous property record is not in use." )
        void prevNotInUse( PropertyRecord property );

        @Documented( "The next property record is not in use." )
        void nextNotInUse( PropertyRecord property );

        @Documented( "The previous property record does not have this record as its next record." )
        void previousDoesNotReferenceBack( PropertyRecord property );

        @Documented( "The next property record does not have this record as its previous record." )
        void nextDoesNotReferenceBack( PropertyRecord property );

        @Documented( "The type of this property is invalid." )
        void invalidPropertyType( PropertyBlock block );

        @Documented( "The string block is not in use." )
        void stringNotInUse( PropertyBlock block, DynamicRecord value );

        @Documented( "The array block is not in use." )
        void arrayNotInUse( PropertyBlock block, DynamicRecord value );

        @Documented( "The string block is empty." )
        void stringEmpty( PropertyBlock block, DynamicRecord value );

        @Documented( "The array block is empty." )
        void arrayEmpty( PropertyBlock block, DynamicRecord value );

        /*
         * Pass in property record id and property key id, not PropertyRecord or PropertyBlock because if this record contains
         * and invalid value it will throw exception when trying to do a toString() of it.
         */
        @Documented( "The property value is invalid." )
        void invalidPropertyValue( long propertyRecordId, int propertyKeyId );

        @Documented( "This record is first in a property chain, but no Node or Relationship records reference this record." )
        void orphanPropertyChain();

        @Documented( "The string property is not referenced anymore, but the corresponding block has not been deleted." )
        void stringUnreferencedButNotDeleted( PropertyBlock block );

        @Documented( "The array property is not referenced anymore, but the corresponding block as not been deleted." )
        void arrayUnreferencedButNotDeleted( PropertyBlock block );

        @Documented( "This property was declared to be changed for a node or relationship, but that node or relationship " +
                "does not contain this property in its property chain." )
        void ownerDoesNotReferenceBack();

        @Documented( "This property was declared to be changed for a node or relationship, but that node or relationship " +
                "did not contain this property in its property chain prior to the change. The property is referenced by another owner." )
        void changedForWrongOwner();

        @Documented( "The string record referred from this property is also referred from a another property." )
        void stringMultipleOwners( PropertyRecord otherOwner );

        @Documented( "The array record referred from this property is also referred from a another property." )
        void arrayMultipleOwners( PropertyRecord otherOwner );

        @Documented( "The string record referred from this property is also referred from a another string record." )
        void stringMultipleOwners( DynamicRecord dynamic );

        @Documented( "The array record referred from this property is also referred from a another array record." )
        void arrayMultipleOwners( DynamicRecord dynamic );
    }

    interface NameConsistencyReport extends ConsistencyReport
    {
        @Documented( "The name block is not in use." )
        void nameBlockNotInUse( DynamicRecord record );

        @Warning
        @Documented( "The token name is empty. Empty token names are discouraged and also prevented in version 2.0.x and " +
                "above, but they can be accessed just like any other tokens. It's possible that this token have been " +
                "created in an earlier version where there were no checks for name being empty." )
        void emptyName( DynamicRecord name );

        @Documented( "The string record referred from this name record is also referred from a another string record." )
        void nameMultipleOwners( DynamicRecord otherOwner );
    }

    interface RelationshipTypeConsistencyReport extends NameConsistencyReport
    {
        @Documented( "The string record referred from this relationship type is also referred from a another relationship type." )
        void nameMultipleOwners( RelationshipTypeTokenRecord otherOwner );
    }

    interface LabelTokenConsistencyReport extends NameConsistencyReport
    {
        @Documented( "The string record referred from this label name is also referred from a another label name." )
        void nameMultipleOwners( LabelTokenRecord otherOwner );
    }

    interface PropertyKeyTokenConsistencyReport extends NameConsistencyReport
    {
        @Documented( "The string record referred from this key is also referred from a another key." )
        void nameMultipleOwners( PropertyKeyTokenRecord otherOwner );
    }

    interface RelationshipGroupConsistencyReport extends ConsistencyReport
    {
        @Documented( "The relationship type field has an illegal value." )
        void illegalRelationshipType();

        @Documented( "The relationship type record is not in use." )
        void relationshipTypeNotInUse( RelationshipTypeTokenRecord referred );

        @Documented( "The next relationship group is not in use." )
        void nextGroupNotInUse();

        @Documented( "The location of group in the chain is invalid, should be sorted by type ascending." )
        void invalidTypeSortOrder();

        @Documented( "The first outgoing relationship is not in use." )
        void firstOutgoingRelationshipNotInUse();

        @Documented( "The first incoming relationship is not in use." )
        void firstIncomingRelationshipNotInUse();

        @Documented( "The first loop relationship is not in use." )
        void firstLoopRelationshipNotInUse();

        @Documented( "The first outgoing relationship is not the first in its chain." )
        void firstOutgoingRelationshipNotFirstInChain();

        @Documented( "The first incoming relationship is not the first in its chain." )
        void firstIncomingRelationshipNotFirstInChain();

        @Documented( "The first loop relationship is not the first in its chain." )
        void firstLoopRelationshipNotFirstInChain();

        @Documented( "The first outgoing relationship is of a different type than its group." )
        void firstOutgoingRelationshipOfOtherType();

        @Documented( "The first incoming relationship is of a different type than its group." )
        void firstIncomingRelationshipOfOtherType();

        @Documented( "The first loop relationship is of a different type than its group." )
        void firstLoopRelationshipOfOtherType();

        @Documented( "The owner of the relationship group is not in use." )
        void ownerNotInUse();

        @Documented( "Illegal owner value." )
        void illegalOwner();

        @Documented( "Next chained relationship group has another owner." )
        void nextHasOtherOwner( RelationshipGroupRecord referred );

        @Documented( "The first incoming relationship does not share node with group" )
        void firstIncomingRelationshipDoesNotShareNodeWithGroup( RelationshipRecord record );

        @Documented( "The first outgoing relationship does not share node with group" )
        void firstOutgoingRelationshipDoesNotShareNodeWithGroup( RelationshipRecord record );

        @Documented( "The first loop relationship does not share node with group" )
        void firstLoopRelationshipDoesNotShareNodeWithGroup( RelationshipRecord record );

        @Documented( "The relationship group chain has multiple last groups" )
        void multipleLastGroups( NodeRecord record );
    }

    interface DynamicConsistencyReport extends ConsistencyReport
    {
        @Documented( "The next block is not in use." )
        void nextNotInUse( DynamicRecord next );

        @Warning
        @Documented( "The record is not full, but references a next block." )
        void recordNotFullReferencesNext();

        @Documented( "The length of the block is invalid." )
        void invalidLength();

        @Warning
        @Documented( "The block is empty." )
        void emptyBlock();

        @Warning
        @Documented( "The next block is empty." )
        void emptyNextBlock( DynamicRecord next );

        @Documented( "The next block references a previous record in the chain." )
        void circularReferenceNext( DynamicRecord next );

        @Documented( "The next block of this record is also referenced by another dynamic record." )
        void nextMultipleOwners( DynamicRecord otherOwner );

        @Documented( "The next block of this record is also referenced by a property record." )
        void nextMultipleOwners( PropertyRecord otherOwner );

        @Documented( "The next block of this record is also referenced by a relationship type." )
        void nextMultipleOwners( RelationshipTypeTokenRecord otherOwner );

        @Documented( "The next block of this record is also referenced by a property key." )
        void nextMultipleOwners( PropertyKeyTokenRecord otherOwner );

        @Documented( "This record not referenced from any other dynamic block, or from any property or name record." )
        void orphanDynamicRecord();
    }

    interface DynamicLabelConsistencyReport extends ConsistencyReport
    {
        @Documented( "This label record is not referenced by its owning node record or that record is not in use." )
        void orphanDynamicLabelRecordDueToInvalidOwner( NodeRecord owningNodeRecord );

        @Documented( "This label record does not have an owning node record." )
        void orphanDynamicLabelRecord();
    }

    interface NodeInUseWithCorrectLabelsReport extends ConsistencyReport
    {
        void nodeNotInUse( NodeRecord referredNodeRecord );

        void nodeDoesNotHaveExpectedLabel( NodeRecord referredNodeRecord, long expectedLabelId );

        void nodeLabelNotInIndex( NodeRecord referredNodeRecord, long missingLabelId );
    }

    interface RelationshipInUseWithCorrectRelationshipTypeReport extends ConsistencyReport
    {
        void relationshipNotInUse( RelationshipRecord referredRelationshipRecord );

        void relationshipDoesNotHaveExpectedRelationshipType( RelationshipRecord referredRelationshipRecord, long expectedRelationshipTypeId );

        void relationshipTypeNotInIndex( RelationshipRecord referredRelationshipRecord, long missingTypeId );
    }

    interface LabelScanConsistencyReport extends NodeInUseWithCorrectLabelsReport
    {
        @Override
        @Documented( "This label scan document refers to a node record that is not in use." )
        void nodeNotInUse( NodeRecord referredNodeRecord );

        @Override
        @Documented( "This label scan document refers to a node that does not have the expected label." )
        void nodeDoesNotHaveExpectedLabel( NodeRecord referredNodeRecord, long expectedLabelId );

        @Override
        @Documented( "This node record has a label that is not found in the label scan store entry for this node" )
        void nodeLabelNotInIndex( NodeRecord referredNodeRecord, long missingLabelId );

        @Warning
        @Documented( "Label index was not properly shutdown and rebuild is required." )
        void dirtyIndex();
    }

    interface RelationshipTypeScanConsistencyReport extends RelationshipInUseWithCorrectRelationshipTypeReport
    {
        @Override
        @Documented( "This relationship type scan document refers to a relationship record that is not in use." )
        void relationshipNotInUse( RelationshipRecord referredRelationshipRecord );

        @Override
        @Documented( "This relationship type scan document refers to a relationship that does not have the expected type." )
        void relationshipDoesNotHaveExpectedRelationshipType( RelationshipRecord referredRelationshipRecord, long expectedRelationshipTypeId );

        @Override
        @Documented( "This relationship record has a type that is not found in the relationship type scan store entry for this relationship." )
        void relationshipTypeNotInIndex( RelationshipRecord referredRelationshipRecord, long missingTypeId );

        @Warning
        @Documented( "Relationship type index was not properly shutdown and rebuild is required." )
        void dirtyIndex();
    }

    interface IndexConsistencyReport extends NodeInUseWithCorrectLabelsReport, RelationshipInUseWithCorrectRelationshipTypeReport
    {
        @Override
        @Documented( "This index entry refers to a node record that is not in use." )
        void nodeNotInUse( NodeRecord referredNodeRecord );

        @Override
        @Documented( "This index entry refers to a relationship record that is not in use." )
        void relationshipNotInUse( RelationshipRecord referredRelationshipRecord );

        @Override
        @Documented( "This relationship record has a type that is not found in the index for this relationship." )
        void relationshipTypeNotInIndex( RelationshipRecord referredRelationshipRecord, long missingTypeId );

        @Override
        @Documented( "This index entry refers to a node that does not have the expected label." )
        void nodeDoesNotHaveExpectedLabel( NodeRecord referredNodeRecord, long expectedLabelId );

        @Override
        @Documented( "This index entry refers to a relationship that does not have the expected relationship type." )
        void relationshipDoesNotHaveExpectedRelationshipType( RelationshipRecord referredRelationshipRecord, long expectedRelationshipTypeId );

        @Documented( "This index entry refers to a node that shouldn't be in the index." )
        void nodeIndexedWhenShouldNot( NodeRecord referredNodeRecord );

        @Documented( "This index entry refers to a relationship that shouldn't be in the index." )
        void relationshipIndexedWhenShouldNot( RelationshipRecord referredRelationshipRecord );

        @Documented( "This index entry does not have the same values as the referred node." )
        void nodeIndexedWithWrongValues( NodeRecord referredNodeRecord, Object[] propertyValues );

        @Documented( "This index entry does not have the same values as the referred relationship." )
        void relationshipIndexedWithWrongValues( RelationshipRecord referredRelationshipRecord, Object[] propertyValues );

        @Override
        @Documented( "This node record has a label that is not found in the index for this node" )
        void nodeLabelNotInIndex( NodeRecord referredNodeRecord, long missingLabelId );

        @Warning
        @Documented( "Index was not properly shutdown and rebuild is required." )
        void dirtyIndex();

        @Documented( "This index entry is for a relationship index, but it is used as a constraint index" )
        void relationshipConstraintIndex();
    }

    interface CountsConsistencyReport extends ConsistencyReport
    {
        @Documented( "The node count does not correspond with the expected count." )
        void inconsistentNodeCount( long expectedCount );

        @Documented( "The relationship count does not correspond with the expected count." )
        void inconsistentRelationshipCount( long expectedCount );
    }

    class NoConsistencyReport implements Reporter
    {
        private final Object proxy;

        NoConsistencyReport()
        {
            List<Class<?>> reports = new ArrayList<>();
            reports.add( SchemaConsistencyReport.class );
            reports.add( NodeConsistencyReport.class );
            reports.add( RelationshipConsistencyReport.class );
            reports.add( PropertyConsistencyReport.class );
            reports.add( RelationshipTypeConsistencyReport.class );
            reports.add( LabelTokenConsistencyReport.class );
            reports.add( PropertyKeyTokenConsistencyReport.class );
            reports.add( DynamicConsistencyReport.class );
            reports.add( DynamicLabelConsistencyReport.class );
            reports.add( RelationshipGroupConsistencyReport.class );
            reports.add( CountsConsistencyReport.class );
            InvocationHandler noop = ( proxy, method, args ) -> null;
            this.proxy = Proxy.newProxyInstance( NoConsistencyReport.class.getClassLoader(), reports.toArray( new Class[0] ), noop );
        }

        @Override
        public SchemaConsistencyReport forSchema( SchemaRecord schema )
        {
            return (SchemaConsistencyReport) proxy;
        }

        @Override
        public NodeConsistencyReport forNode( NodeRecord node )
        {
            return (NodeConsistencyReport) proxy;
        }

        @Override
        public RelationshipConsistencyReport forRelationship( RelationshipRecord relationship )
        {
            return (RelationshipConsistencyReport) proxy;
        }

        @Override
        public PropertyConsistencyReport forProperty( PropertyRecord property )
        {
            return (PropertyConsistencyReport) proxy;
        }

        @Override
        public RelationshipTypeConsistencyReport forRelationshipTypeName( RelationshipTypeTokenRecord relationshipType )
        {
            return (RelationshipTypeConsistencyReport) proxy;
        }

        @Override
        public LabelTokenConsistencyReport forLabelName( LabelTokenRecord label )
        {
            return (LabelTokenConsistencyReport) proxy;
        }

        @Override
        public PropertyKeyTokenConsistencyReport forPropertyKey( PropertyKeyTokenRecord key )
        {
            return (PropertyKeyTokenConsistencyReport) proxy;
        }

        @Override
        public DynamicConsistencyReport forDynamicBlock( RecordType type, DynamicRecord record )
        {
            return (DynamicConsistencyReport) proxy;
        }

        @Override
        public DynamicLabelConsistencyReport forDynamicLabelBlock( RecordType type, DynamicRecord record )
        {
            return (DynamicLabelConsistencyReport) proxy;
        }

        @Override
        public LabelScanConsistencyReport forNodeLabelScan( TokenScanDocument document )
        {
            return (LabelScanConsistencyReport) proxy;
        }

        @Override
        public RelationshipTypeScanConsistencyReport forRelationshipTypeScan( TokenScanDocument document )
        {
            return (RelationshipTypeScanConsistencyReport) proxy;
        }

        @Override
        public IndexConsistencyReport forIndexEntry( IndexEntry entry )
        {
            return (IndexConsistencyReport) proxy;
        }

        @Override
        public RelationshipGroupConsistencyReport forRelationshipGroup( RelationshipGroupRecord group )
        {
            return (RelationshipGroupConsistencyReport) proxy;
        }

        @Override
        public CountsConsistencyReport forCounts( CountsEntry countsEntry )
        {
            return (CountsConsistencyReport) proxy;
        }
    }

    Reporter NO_REPORT = new NoConsistencyReport();
}
