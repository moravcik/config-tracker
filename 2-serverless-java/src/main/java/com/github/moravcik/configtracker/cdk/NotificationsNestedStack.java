package com.github.moravcik.configtracker.cdk;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.NestedStack;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.sns.ITopic;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.Map;

import static com.github.moravcik.configtracker.cdk.ConfigTrackerApp.resourcePrefix;
import static com.github.moravcik.configtracker.cdk.LambdaUtils.*;

public class NotificationsNestedStack extends NestedStack {

    public NotificationsNestedStack(@NotNull Construct scope, @NotNull String id, @NotNull ITopic configChangesTopic) {
        super(scope, id);

        Queue notificationsQueue = Queue.Builder.create(this, "NotificationsQueue")
                .queueName(resourcePrefix + "-config-changes-queue")
                .build();

        configChangesTopic.addSubscription(SqsSubscription.Builder.create(notificationsQueue)
                .rawMessageDelivery(true)
                .build());

        Function configNotificationHandler = createLambdaFunctionBuilder(this, "ConfigNotificationHandler")
                .environment(mergeEnvironment(
                        BASE_LAMBDA_ENVIRONMENT,
                        Map.of("SPRING_CLOUD_FUNCTION_DEFINITION", "configNotificationSqsHandler")))
                .build();

        configNotificationHandler.addEventSource(SqsEventSource.Builder.create(notificationsQueue).build());
    }
}