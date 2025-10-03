#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib';
import { Duration } from 'aws-cdk-lib';
import { Architecture, Runtime } from 'aws-cdk-lib/aws-lambda';
import { NodejsFunctionProps } from 'aws-cdk-lib/aws-lambda-nodejs';
import { RetentionDays } from 'aws-cdk-lib/aws-logs';
import { ConfigTrackerStack } from './config-tracker.stack';

export const resourcePrefix = 'config-tracker-1-serverless-ts';

export const baseLambdaProps: NodejsFunctionProps = {
  runtime: Runtime.NODEJS_22_X,
  architecture: Architecture.ARM_64,
  memorySize: 512,
  timeout: Duration.seconds(10),
  logRetention: RetentionDays.ONE_WEEK,
  bundling: { minify: true, sourceMap: true, externalModules: [
    '@aws-sdk/client-dynamodb',
    '@aws-sdk/client-sns',
    '@aws-sdk/lib-dynamodb',
    '@aws-sdk/util-dynamodb'
  ]},
};

const configTrackerApp = new cdk.App();
new ConfigTrackerStack(configTrackerApp, `${resourcePrefix}-stack`, {});