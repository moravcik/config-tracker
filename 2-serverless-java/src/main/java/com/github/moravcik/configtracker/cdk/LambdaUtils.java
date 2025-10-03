package com.github.moravcik.configtracker.cdk;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class LambdaUtils {

    public static final Map<String, String> BASE_LAMBDA_ENVIRONMENT = Map.of(
            "JAVA_TOOL_OPTIONS", "-Xshare:on -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom"
    );

    public static Function.Builder createLambdaFunctionBuilder(Construct scope, String logicalId) {
        return Function.Builder.create(scope, logicalId)
                .runtime(Runtime.JAVA_21)
                .architecture(Architecture.ARM_64)
                .snapStart(SnapStartConf.ON_PUBLISHED_VERSIONS)
                .memorySize(2048)
                .timeout(Duration.seconds(30))
                .logRetention(software.amazon.awscdk.services.logs.RetentionDays.ONE_WEEK)
                .code(Code.fromAsset("target/lambda.jar"))
                .currentVersionOptions(VersionOptions.builder()
                        .description("Auto-published version")
                        .build());
    }

    public static Map<String, String> mergeEnvironment(Map<String, String> base, Map<String, String> additional) {
        Map<String, String> merged = new HashMap<>(base);
        merged.putAll(additional);
        return merged;
    }
}