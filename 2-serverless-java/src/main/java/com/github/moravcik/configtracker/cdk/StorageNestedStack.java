package com.github.moravcik.configtracker.cdk;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.NestedStack;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.StartingPosition;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSource;
import software.amazon.awscdk.services.sns.Topic;
import software.constructs.Construct;

import java.util.Map;

import static com.github.moravcik.configtracker.cdk.ConfigTrackerApp.resourcePrefix;
import static com.github.moravcik.configtracker.cdk.LambdaUtils.*;

public class StorageNestedStack extends NestedStack {

    private final Table configTable;
    private final Topic configChangesTopic;

    public StorageNestedStack(@NotNull Construct scope, @NotNull String id) {
        super(scope, id);

        this.configTable = Table.Builder.create(this, "ConfigTable")
                .tableName(resourcePrefix + "-config-table")
                .partitionKey(Attribute.builder()
                        .name("pk")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("sk")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .stream(StreamViewType.NEW_IMAGE)
                .build();

        this.configChangesTopic = Topic.Builder.create(this, "ConfigChangesTopic")
                .topicName(resourcePrefix + "-config-changes")
                .build();

        Function configTableStreamHandler = createLambdaFunctionBuilder(this, "ConfigStreamHandler")
                .handler("com.github.moravcik.configtracker.lib.lambda.ConfigTableStreamHandler::handleRequest")
                .environment(mergeEnvironment(
                        BASE_LAMBDA_ENVIRONMENT,
                        Map.of("CONFIG_TABLE_NAME", configTable.getTableName(),
                                "CONFIG_CHANGES_TOPIC_ARN", configChangesTopic.getTopicArn())))
                .build();

        Alias configStreamAlias = Alias.Builder.create(this, "ConfigStreamAlias")
                .aliasName("live")
                .version(configTableStreamHandler.getCurrentVersion())
                .build();

        configTable.grantReadWriteData(configTableStreamHandler);
        configTable.grantStreamRead(configTableStreamHandler);
        configChangesTopic.grantPublish(configTableStreamHandler);
        configTable.grantReadWriteData(configStreamAlias);
        configTable.grantStreamRead(configStreamAlias);
        configChangesTopic.grantPublish(configStreamAlias);

        configStreamAlias.addEventSource(DynamoEventSource.Builder.create(configTable)
                .startingPosition(StartingPosition.LATEST)
                .batchSize(5)
                .retryAttempts(2)
                .build());
    }

    public Table getConfigTable() {
        return configTable;
    }

    public Topic getConfigChangesTopic() {
        return configChangesTopic;
    }
}