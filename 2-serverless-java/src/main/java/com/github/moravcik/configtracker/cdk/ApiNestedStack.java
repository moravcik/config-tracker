package com.github.moravcik.configtracker.cdk;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.NestedStack;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.dynamodb.ITable;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

import static com.github.moravcik.configtracker.cdk.ConfigTrackerApp.resourcePrefix;
import static com.github.moravcik.configtracker.cdk.LambdaUtils.*;

public class ApiNestedStack extends NestedStack {

    public ApiNestedStack(@NotNull Construct scope, @NotNull String id, @NotNull ITable configTable) {
        super(scope, id);

        // Lambda functions
        Function configApiHandler = createLambdaFunctionBuilder(this, "ConfigApiHandler")
                .environment(mergeEnvironment(
                        BASE_LAMBDA_ENVIRONMENT,
                        Map.of(
                                "SPRING_CLOUD_FUNCTION_DEFINITION", "configApiHandler",
                                "CONFIG_TABLE_NAME", configTable.getTableName()))
                        )
                .build();

        configTable.grantReadWriteData(configApiHandler);

        Function configChangeApiHandler = createLambdaFunctionBuilder(this, "ConfigChangeApiHandler")
                .environment(mergeEnvironment(
                        BASE_LAMBDA_ENVIRONMENT,
                        Map.of(
                                "SPRING_CLOUD_FUNCTION_DEFINITION", "configChangeApiHandler",
                                "CONFIG_TABLE_NAME", configTable.getTableName()))
                        )
                .build();

        configTable.grantReadData(configChangeApiHandler);

        LogGroup apiLogGroup = LogGroup.Builder.create(this, "RestApiLogGroup")
                .logGroupName("/aws/apigateway/" + resourcePrefix + "-config-api")
                .build();

        RestApi api = RestApi.Builder.create(this, "RestApi")
                .restApiName(resourcePrefix + "-config-api")
                .description("Config Tracker API")
                .apiKeySourceType(ApiKeySourceType.HEADER)
                .defaultCorsPreflightOptions(CorsOptions.builder()
                        .allowOrigins(List.of("*"))
                        .build())
                .cloudWatchRole(true)
                .deployOptions(StageOptions.builder()
                        .stageName("prod")
                        .loggingLevel(MethodLoggingLevel.INFO)
                        .dataTraceEnabled(true)
                        .metricsEnabled(true)
                        .accessLogDestination(new LogGroupLogDestination(apiLogGroup))
                        .accessLogFormat(AccessLogFormat.jsonWithStandardFields())
                        .build())
                .build();

        LambdaIntegration configApiIntegration = new LambdaIntegration(configApiHandler);
        LambdaIntegration configChangeApiIntegration = new LambdaIntegration(configChangeApiHandler);

        MethodOptions apiKeyRequiredOption = MethodOptions.builder().apiKeyRequired(true).build();

        // Config API
        Resource configResource = api.getRoot().addResource("config");
        configResource.addMethod("GET", configApiIntegration, apiKeyRequiredOption);
        configResource.addMethod("POST", configApiIntegration, apiKeyRequiredOption);

        Resource configIdResource = configResource.addResource("{configId}");
        configIdResource.addMethod("GET", configApiIntegration, apiKeyRequiredOption);
        configIdResource.addMethod("PUT", configApiIntegration, apiKeyRequiredOption);
        configIdResource.addMethod("PATCH", configApiIntegration, apiKeyRequiredOption);

        // Config Change API
        Resource configChangeResource = configIdResource.addResource("change");
        configChangeResource.addMethod("GET", configChangeApiIntegration, apiKeyRequiredOption);

        // API Key and Usage Plan
        IApiKey apiKey = api.addApiKey("ApiKey", ApiKeyOptions.builder().description("Config Tracker API Key").build());
        UsagePlan apiUsagePlan = api.addUsagePlan("UsagePlan", UsagePlanProps.builder()
                .name("Config API Usage Plan")
                .throttle(ThrottleSettings.builder().rateLimit(100).burstLimit(200).build())
                .quota(QuotaSettings.builder().limit(10000).period(Period.DAY).build())
                .build());
        apiUsagePlan.addApiStage(UsagePlanPerApiStage.builder().stage(api.getDeploymentStage()).build());
        apiUsagePlan.addApiKey(apiKey);
    }



}