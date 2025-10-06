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

    private final RestApi api;
    private final IApiKey apiKey;

    public ApiNestedStack(@NotNull Construct scope, @NotNull String id, @NotNull ITable configTable) {
        super(scope, id);

        // Lambda functions with versions and aliases
        Function configApiHandler = createLambdaFunctionBuilder(this, "ConfigApiHandler")
                .handler("com.github.moravcik.configtracker.lib.lambda.ConfigApiHandler::handleRequest")
                .environment(mergeEnvironment(
                        BASE_LAMBDA_ENVIRONMENT,
                        Map.of("CONFIG_TABLE_NAME", configTable.getTableName())))
                .build();

        configTable.grantReadWriteData(configApiHandler);

        Function configChangeApiHandler = createLambdaFunctionBuilder(this, "ConfigChangeApiHandler")
                .handler("com.github.moravcik.configtracker.lib.lambda.ConfigChangeApiHandler::handleRequest")
                .environment(mergeEnvironment(
                        BASE_LAMBDA_ENVIRONMENT,
                        Map.of("CONFIG_TABLE_NAME", configTable.getTableName())))
                .build();

        configTable.grantReadData(configChangeApiHandler);

        LogGroup apiLogGroup = LogGroup.Builder.create(this, "RestApiLogGroup")
                .logGroupName("/aws/apigateway/" + resourcePrefix + "-config-api")
                .build();

        this.api = RestApi.Builder.create(this, "RestApi")
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

        // JSON Schema Model - inlined, as there is no option to import existing schema file in Java implementation
        Model configModel = api.addModel("ConfigModel", ModelOptions.builder()
                .contentType("application/json")
                .modelName("Config")
                .schema(JsonSchema.builder()
                        .type(JsonSchemaType.OBJECT)
                        .properties(Map.of(
                                "creditPolicy", JsonSchema.builder()
                                        .type(JsonSchemaType.OBJECT)
                                        .properties(Map.of(
                                                "maxCreditLimit", JsonSchema.builder().type(JsonSchemaType.NUMBER).build(),
                                                "minCreditScore", JsonSchema.builder().type(JsonSchemaType.NUMBER).build(),
                                                "currency", JsonSchema.builder().type(JsonSchemaType.STRING).enumValue(List.of("EUR", "USD", "GBP")).build(),
                                                "exceptions", JsonSchema.builder()
                                                        .type(JsonSchemaType.ARRAY)
                                                        .items(JsonSchema.builder()
                                                                .type(JsonSchemaType.OBJECT)
                                                                .properties(Map.of(
                                                                        "segment", JsonSchema.builder().type(JsonSchemaType.STRING).build(),
                                                                        "maxCreditLimit", JsonSchema.builder().type(JsonSchemaType.NUMBER).build(),
                                                                        "requiresTwoManRule", JsonSchema.builder().type(JsonSchemaType.BOOLEAN).build()
                                                                ))
                                                                .required(List.of("segment"))
                                                                .additionalProperties(false)
                                                                .build())
                                                        .build()
                                        ))
                                        .required(List.of("maxCreditLimit", "minCreditScore", "currency"))
                                        .additionalProperties(false)
                                        .build(),
                                "approvalPolicy", JsonSchema.builder()
                                        .type(JsonSchemaType.OBJECT)
                                        .properties(Map.of(
                                                "twoManRule", JsonSchema.builder().type(JsonSchemaType.BOOLEAN).build(),
                                                "autoApproveThreshold", JsonSchema.builder().type(JsonSchemaType.NUMBER).build(),
                                                "levels", JsonSchema.builder()
                                                        .type(JsonSchemaType.ARRAY)
                                                        .items(JsonSchema.builder()
                                                                .type(JsonSchemaType.OBJECT)
                                                                .properties(Map.of(
                                                                        "role", JsonSchema.builder().type(JsonSchemaType.STRING).enumValue(List.of("TEAM_LEAD", "HEAD_OF_CREDIT", "CFO")).build(),
                                                                        "limit", JsonSchema.builder().type(JsonSchemaType.NUMBER).build()
                                                                ))
                                                                .required(List.of("role", "limit"))
                                                                .additionalProperties(false)
                                                                .build())
                                                        .build()
                                        ))
                                        .required(List.of("twoManRule", "autoApproveThreshold", "levels"))
                                        .additionalProperties(false)
                                        .build(),
                                "riskScoring", JsonSchema.builder()
                                        .type(JsonSchemaType.OBJECT)
                                        .properties(Map.of(
                                                "weights", JsonSchema.builder()
                                                        .type(JsonSchemaType.OBJECT)
                                                        .properties(Map.of(
                                                                "incomeToDebtRatio", JsonSchema.builder().type(JsonSchemaType.NUMBER).build(),
                                                                "age", JsonSchema.builder().type(JsonSchemaType.NUMBER).build(),
                                                                "historyLengthMonths", JsonSchema.builder().type(JsonSchemaType.NUMBER).build(),
                                                                "delinquencyCount", JsonSchema.builder().type(JsonSchemaType.NUMBER).build()
                                                        ))
                                                        .required(List.of("incomeToDebtRatio", "historyLengthMonths", "delinquencyCount"))
                                                        .additionalProperties(false)
                                                        .build(),
                                                "thresholds", JsonSchema.builder()
                                                        .type(JsonSchemaType.OBJECT)
                                                        .properties(Map.of(
                                                                "low", JsonSchema.builder().type(JsonSchemaType.NUMBER).build(),
                                                                "medium", JsonSchema.builder().type(JsonSchemaType.NUMBER).build(),
                                                                "high", JsonSchema.builder().type(JsonSchemaType.NUMBER).build()
                                                        ))
                                                        .required(List.of("low", "medium", "high"))
                                                        .additionalProperties(false)
                                                        .build()
                                        ))
                                        .required(List.of("weights", "thresholds"))
                                        .additionalProperties(false)
                                        .build()
                        ))
                        .additionalProperties(false)
                        .build())
                .build());

        LambdaIntegration configApiIntegration = new LambdaIntegration(configApiHandler);
        LambdaIntegration configChangeApiIntegration = new LambdaIntegration(configChangeApiHandler);

        MethodOptions apiKeyRequiredOption = MethodOptions.builder().apiKeyRequired(true).build();
        MethodOptions apiKeyWithValidationOption = MethodOptions.builder()
                .apiKeyRequired(true)
                .requestValidator(api.addRequestValidator("RequestValidator", RequestValidatorOptions.builder()
                        .validateRequestBody(true)
                        .validateRequestParameters(false)
                        .build()))
                .requestModels(Map.of("application/json", configModel))
                .build();

        // Config API
        Resource configResource = api.getRoot().addResource("config");
        configResource.addMethod("GET", configApiIntegration, apiKeyRequiredOption);
        configResource.addMethod("POST", configApiIntegration, apiKeyWithValidationOption);

        Resource configIdResource = configResource.addResource("{configId}");
        configIdResource.addMethod("GET", configApiIntegration, apiKeyRequiredOption);
        configIdResource.addMethod("PUT", configApiIntegration, apiKeyWithValidationOption);
        configIdResource.addMethod("PATCH", configApiIntegration, apiKeyRequiredOption);

        // Config Change API
        Resource configChangeResource = configIdResource.addResource("change");
        configChangeResource.addMethod("GET", configChangeApiIntegration, apiKeyRequiredOption);

        // API Key and Usage Plan
        this.apiKey = api.addApiKey("ApiKey", ApiKeyOptions.builder().description("Config Tracker API Key").build());
        UsagePlan apiUsagePlan = api.addUsagePlan("UsagePlan", UsagePlanProps.builder()
                .name("Config API Usage Plan")
                .throttle(ThrottleSettings.builder().rateLimit(100).burstLimit(200).build())
                .quota(QuotaSettings.builder().limit(10000).period(Period.DAY).build())
                .build());
        apiUsagePlan.addApiStage(UsagePlanPerApiStage.builder().stage(api.getDeploymentStage()).build());
        apiUsagePlan.addApiKey(apiKey);
    }

    public String getApiUrl() {
        return api.getUrl();
    }

    public String getApiKeyId() {
        return apiKey.getKeyId();
    }



}