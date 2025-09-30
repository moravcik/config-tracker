import { NestedStack, NestedStackProps, RemovalPolicy } from 'aws-cdk-lib';
import { AttributeType, BillingMode, ProjectionType, StreamViewType, Table } from 'aws-cdk-lib/aws-dynamodb';
import { FilterCriteria, FilterRule, StartingPosition } from 'aws-cdk-lib/aws-lambda';
import { DynamoEventSource } from 'aws-cdk-lib/aws-lambda-event-sources';
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';
import { ITopic } from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';
import { baseLambdaProps, resourcePrefix } from './config-tracker.app';

export class StorageNestedStack extends NestedStack {
  public readonly configTable: Table;

  constructor(scope: Construct, id: string, props: NestedStackProps & { configChangesTopic: ITopic }) {
    super(scope, id, props);

    this.configTable = new Table(this, 'ConfigTable', {
      tableName: `${resourcePrefix}-config-table`,
      partitionKey: { name: 'pk', type: AttributeType.STRING },
      sortKey: { name: 'sk', type: AttributeType.STRING },
      billingMode: BillingMode.PAY_PER_REQUEST,
      removalPolicy: RemovalPolicy.DESTROY,
      stream: StreamViewType.NEW_IMAGE,
    });

    const configTableStreamHandler = new NodejsFunction(this, 'ConfigStreamHandler', {
      entry: 'lib/lambda/config-table-stream.handler.ts',
      environment: {
        CONFIG_TABLE_NAME: this.configTable.tableName,
        CONFIG_CHANGES_TOPIC_ARN: props.configChangesTopic.topicArn,
      },
      ...baseLambdaProps,
    });
    this.configTable.grantReadWriteData(configTableStreamHandler);
    this.configTable.grantStreamRead(configTableStreamHandler);
    props.configChangesTopic.grantPublish(configTableStreamHandler);

    configTableStreamHandler.addEventSource(new DynamoEventSource(this.configTable, {
      startingPosition: StartingPosition.LATEST,
      batchSize: 5,
      retryAttempts: 2,
      filters: [
        FilterCriteria.filter({
          eventName: FilterRule.isEqual('INSERT'),
          dynamodb: { NewImage: { entityType: { S: FilterRule.or('CONFIG', 'CONFIG_CHANGE') }}},
        }),
      ],
    }));
  }
}
