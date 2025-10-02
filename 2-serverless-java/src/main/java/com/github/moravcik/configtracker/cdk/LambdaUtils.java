package com.github.moravcik.configtracker.cdk;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class LambdaUtils {

    public static final Map<String, String> BASE_LAMBDA_ENVIRONMENT = Map.of(
            // "JAVA_TOOL_OPTIONS", "-XX:+TieredCompilation -XX:TieredStopAtLevel=1",
            "MAIN_CLASS", "com.github.moravcik.configtracker.lib.Application"
    );

    public static Function.Builder createLambdaFunctionBuilder(Construct scope, String logicalId) {
        return Function.Builder.create(scope, logicalId)
                .runtime(Runtime.JAVA_21)
                .architecture(Architecture.ARM_64)
                .snapStart(SnapStartConf.ON_PUBLISHED_VERSIONS)
                .memorySize(1024)
                .timeout(Duration.seconds(30))
                .logRetention(software.amazon.awscdk.services.logs.RetentionDays.ONE_WEEK)
                .handler("org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest")
                .code(Code.fromAsset("target/lambda.jar"));
    }

    public static Map<String, String> mergeEnvironment(Map<String, String> base, Map<String, String> additional) {
        Map<String, String> merged = new HashMap<>(base);
        merged.putAll(additional);
        return merged;
    }
}