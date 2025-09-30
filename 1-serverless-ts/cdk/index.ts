#!/usr/bin/env node
import { CfnElement, Duration, Stack, StackProps } from 'aws-cdk-lib';
import * as cdk from 'aws-cdk-lib';
import { Architecture, Runtime } from 'aws-cdk-lib/aws-lambda';
import { Construct } from 'constructs';
import { ApiStack } from './api.stack';
import { NotificationsStack } from './notifications.stack';
import { StorageStack } from './storage.stack';

export const resourcePrefix = 'config-tracker-1-serverless-ts';

export const baseLambdaProps = {
  runtime: Runtime.NODEJS_22_X,
  architecture: Architecture.ARM_64,
  memorySize: 512,
  timeout: Duration.seconds(10),
  bundling: { minify: true, sourceMap: true, externalModules: [
    '@aws-sdk/client-dynamodb',
    '@aws-sdk/client-sns',
    '@aws-sdk/lib-dynamodb',
    '@aws-sdk/util-dynamodb'
  ]},
};

class ConfigTrackerStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    const { configChangesTopic } = new NotificationsStack(this, 'Notifications');
    const { configTable } = new StorageStack(this, 'Storage', { configChangesTopic });
    new ApiStack(this, 'Api', { configTable });

  }

  // https://github.com/aws/aws-cdk/issues/19099
  getLogicalId(element: CfnElement): string {
    if (element.node.id.includes('NestedStackResource')) {
      return /([a-zA-Z0-9]+)\.NestedStackResource/.exec(element.node.id)![1] // will be the exact id of the stack
    }
    return super.getLogicalId(element)
  }
}

const index = new cdk.App();
new ConfigTrackerStack(index, `${resourcePrefix}-stack`, {});