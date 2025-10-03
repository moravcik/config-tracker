import { NestedStack, NestedStackProps } from 'aws-cdk-lib';
import { SqsEventSource } from 'aws-cdk-lib/aws-lambda-event-sources';
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';
import { ITopic } from 'aws-cdk-lib/aws-sns';
import { SqsSubscription } from 'aws-cdk-lib/aws-sns-subscriptions';
import { Queue } from 'aws-cdk-lib/aws-sqs';
import { Construct } from 'constructs';
import { baseLambdaProps, resourcePrefix } from './config-tracker.app';

export class NotificationsNestedStack extends NestedStack {
  constructor(scope: Construct, id: string, props: NestedStackProps & { configChangesTopic: ITopic }) {
    super(scope, id, props);

    const notificationsQueue = new Queue(this, 'NotificationsQueue', {queueName: `${resourcePrefix}-config-changes-queue`});
    props.configChangesTopic.addSubscription(new SqsSubscription(notificationsQueue, {rawMessageDelivery: true}));

    const configNotificationHandler = new NodejsFunction(this, 'ConfigNotificationHandler', {
      entry: 'lib/lambda/config-notification-sqs.handler.ts',
      ...baseLambdaProps
    });
    configNotificationHandler.addEventSource(new SqsEventSource(notificationsQueue));
  }
}