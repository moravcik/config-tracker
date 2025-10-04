import * as cdk from 'aws-cdk-lib';
import { CfnElement, CfnOutput, Stack, StackProps } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { ApiNestedStack } from './api.nested-stack';
import { NotificationsNestedStack } from './notifications.nested-stack';
import { StorageNestedStack } from './storage.nested-stack';

export class ConfigTrackerStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    const { configTable, configChangesTopic } = new StorageNestedStack(this, 'Storage', {});
    const { api, apiKey } = new ApiNestedStack(this, 'Api', { configTable });
    new NotificationsNestedStack(this, 'Notifications', { configChangesTopic });

    // Outputs
    new CfnOutput(this, 'RestApiUrl', { value: api.url });
    new CfnOutput(this, 'RestApiKeyCommand', { value: `aws apigateway get-api-key --api-key ${apiKey.keyId} --include-value --query 'value' --output text` });
  }

  // https://github.com/aws/aws-cdk/issues/19099
  getLogicalId(element: CfnElement): string {
    if (element.node.id.includes('NestedStackResource')) {
      return /([a-zA-Z0-9]+)\.NestedStackResource/.exec(element.node.id)![1] // will be the exact id of the stack
    }
    return super.getLogicalId(element)
  }
}
