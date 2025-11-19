package gov.cdc.izgateway.repository;

import java.util.Set;
import java.util.TreeSet;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * This is a converter that converts empty sets to nulls for storage in DynamoDB,
 * and converts nulls back to empty sets when reading from DynamoDB.
 * 
 * When using Set<String> in an attribute on a DynamoDB entity, annotate the attribute with 
 * @DynamoDbConvertedBy(EmptySetToNullConverter.class)
 * 
 * @author Audacious Inquiry
 *
 */
public class EmptySetToNullConverter implements AttributeConverter<Set<String>> {
    @Override
    public AttributeValue transformFrom(Set<String> input) {
        if (input == null || input.isEmpty()) {
            return AttributeValue.builder().nul(true).build();
        }
        return AttributeValue.builder().ss(input).build();
    }

    @Override
    public Set<String> transformTo(AttributeValue attributeValue) {
        if (attributeValue == null || attributeValue.nul() != null && attributeValue.nul()) {
            return new TreeSet<>();
        }
        return new TreeSet<>(attributeValue.ss());
    }

    @Override
    public EnhancedType<Set<String>> type() {
        return EnhancedType.setOf(String.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.SS;
    }
}