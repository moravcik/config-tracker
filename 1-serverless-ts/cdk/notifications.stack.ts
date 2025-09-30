import { Duration, NestedStack, NestedStackProps } from 'aws-cdk-lib';
import { SqsEventSource } from 'aws-cdk-lib/aws-lambda-event-sources';
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { SqsSubscription } from 'aws-cdk-lib/aws-sns-subscriptions';
import { Queue } from 'aws-cdk-lib/aws-sqs';
import { Construct } from 'constructs';
import { baseLambdaProps, resourcePrefix } from '.';

export class NotificationsStack extends NestedStack {
  public readonly configChangesTopic: Topic;

  constructor(scope: Construct, id: string, props?: NestedStackProps) {
    super(scope, id, props);

    this.configChangesTopic = new Topic(this, 'ConfigChangesTopic', {
      topicName: `${resourcePrefix}-config-changes-notification`,
    });

    const notificationsQueue = new Queue(this, 'NotificationsQueue', {queueName: `${resourcePrefix}-config-changes-queue`});
    this.configChangesTopic.addSubscription(new SqsSubscription(notificationsQueue, {rawMessageDelivery: true}));

    const configNotificationHandler = new NodejsFunction(this, 'ConfigNotificationHandler', {
      entry: 'lib/lambda/config-notification-sqs.handler.ts',
      ...baseLambdaProps
    });
    configNotificationHandler.addEventSource(new SqsEventSource(notificationsQueue));
  }
}