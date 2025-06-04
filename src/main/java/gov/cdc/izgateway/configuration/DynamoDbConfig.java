package gov.cdc.izgateway.configuration;

import gov.cdc.izgateway.common.HealthService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

import java.net.URI;
import java.util.ServiceConfigurationError;

/**
 * DynamoDB configuration for IZ Gateway applications.
 * <p>
 * This configuration creates the necessary DynamoDB clients for accessing DynamoDB repositories.
 * It can be customized via application properties and supports both local and AWS DynamoDB instances.
 * <p>
 * Configuration properties:
 * - amazon.dynamodb.endpoint: Optional endpoint override for local DynamoDB
 * - amazon.dynamodb.table: Optional table name, default is izgw-hub
 * 
 * @author IZ Gateway Team
 */
@Configuration
@ConditionalOnClass({DynamoDbClient.class, DynamoDbEnhancedClient.class})
@Slf4j
@Getter
public class DynamoDbConfig {

    @Value("${amazon.dynamodb.endpoint:}")
    private String dynamodbEndpoint;

    @Value("${amazon.dynamodb.table:izgw-hub}")
    private String dynamodbTable;

    /**
     * Creates a DynamoDB client, endpoint override is optional.
     * 
     * @return Configured DynamoDB client
     * @throws ServiceConfigurationError if validation is enabled and fails
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
        var builder = DynamoDbClient.builder()
                .region(DefaultAwsRegionProviderChain.builder().build().getRegion())
                .credentialsProvider(credentialsProvider);

        // Verify credentials can be resolved
        @SuppressWarnings("unused")
        AwsCredentials credentials = credentialsProvider.resolveCredentials();

        HealthService.setDatabase(dynamodbEndpoint + dynamodbTable);

        // Configure endpoint override if specified (typically for local development)
        if (StringUtils.isNotBlank(dynamodbEndpoint)) {
            builder.endpointOverride(URI.create(dynamodbEndpoint));
            log.info("DynamoDB Client initialized to {}", dynamodbEndpoint);
        } else {
            log.info("DynamoDB Client initialized to AWS default endpoint");
        }

        DynamoDbClient ddbClient = builder.build();

        ListTablesResponse listTablesResponse;
        try {
            ListTablesRequest lgtr = ListTablesRequest.builder().build();
            listTablesResponse = ddbClient.listTables(lgtr);
        } catch (Exception e) {
            log.error("Cannot list tables in DynamoDB {}", StringUtils.defaultIfEmpty(dynamodbEndpoint, "AWS"));
            throw new ServiceConfigurationError("Cannot list tables", e);
        }

        if (!listTablesResponse.hasTableNames()) {
            log.error("No tables exist in DynamoDB {}", StringUtils.defaultIfEmpty(dynamodbEndpoint, "AWS"));
            throw new ServiceConfigurationError("No tables exist in " + StringUtils.defaultIfEmpty(dynamodbEndpoint, "AWS"));
        }

        if (!listTablesResponse.tableNames().contains(dynamodbTable)) {
            log.error("Configured table does not exist in DynamoDB: {}", dynamodbTable);
            throw new ServiceConfigurationError("Configured table does not exist in DynamoDB: " + dynamodbTable);
        }

        return ddbClient;
    }

    /**
     * Create an enhanced DynamoDB Client
     * @param ddbc The basic client to build the enhanced client from
     * @return	The enhanced client
     */
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient ddbc) {
        return DynamoDbEnhancedClient.builder()
                .extensions(VersionedRecordExtension.builder().build())
                .dynamoDbClient(ddbc)
                .build();
    }
}
