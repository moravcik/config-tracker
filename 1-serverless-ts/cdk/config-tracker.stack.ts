import { CfnElement, Stack, StackProps } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { ApiNestedStack } from './api.nested-stack';
import { NotificationsNestedStack } from './notifications.nested-stack';
import { StorageNestedStack } from './storage.nested-stack';

export class ConfigTrackerStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    const { configTable, configChangesTopic } = new StorageNestedStack(this, 'Storage', {});
    new ApiNestedStack(this, 'Api', { configTable });
    new NotificationsNestedStack(this, 'Notifications', { configChangesTopic });
  }

  // https://github.com/aws/aws-cdk/issues/19099
  getLogicalId(element: CfnElement): string {
    if (element.node.id.includes('NestedStackResource')) {
      return /([a-zA-Z0-9]+)\.NestedStackResource/.exec(element.node.id)![1] // will be the exact id of the stack
    }
    return super.getLogicalId(element)
  }
}
