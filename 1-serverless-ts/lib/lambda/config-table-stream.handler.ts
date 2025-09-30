import { AttributeValue } from '@aws-sdk/client-dynamodb';
import { PublishCommand, SNSClient } from '@aws-sdk/client-sns';
import { BatchWriteCommand } from '@aws-sdk/lib-dynamodb';
import { unmarshall } from '@aws-sdk/util-dynamodb';
import { Context, DynamoDBStreamEvent, DynamoDBRecord, StreamRecord } from 'aws-lambda';
import { ConfigChangeItem, ConfigPathChangeItem, ConfigTableItem } from '../types/config-table-item';
import { calculateDifferences } from '../utils/object-utils';
import {
  configTableName,
  documentClient,
  getLatestConfigEntity,
  stripDbKeysFromConfigTableItem
} from '../utils/dynamo-utils';

const snsClient = new SNSClient();
const configChangesTopicArn = process.env.CONFIG_CHANGES_TOPIC_ARN!;

export async function handler(event: DynamoDBStreamEvent, context: Context) {
  context.callbackWaitsForEmptyEventLoop = false;
  await Promise.all(event.Records.map(processRecord));
}

async function processRecord(record: DynamoDBRecord) {
  console.log("Processing DynamoDB record", record);
  const { eventName, dynamodb: { NewImage, Keys }} = record as { eventName: string, dynamodb: StreamRecord };
  if (eventName === 'INSERT' && NewImage) {
    const item = unmarshall(NewImage as Record<string, AttributeValue>) as ConfigTableItem;

    switch (item.entityType) {
      case 'CONFIG':
        await handleInsertConfig(item.configId);
        break;
      case 'CONFIG_CHANGE':
        await handleInsertConfigChange(item as ConfigChangeItem);
        break;
      default:
        console.log(`No handler for entityType ${item.entityType}`);
    }
  }
}

async function handleInsertConfig(configId: string) {
  const [latest, secondLatest] = await getLatestConfigEntity(configId, 2);
  if (!latest || !secondLatest) return;

  const pathChanges = calculateDifferences(secondLatest.config, latest.config);
  console.log(`Config ${configId} has changes:`, pathChanges);

  // Prepare config change and patch change items
  const configChangeItems = [
    {
      pk: `CONFIG_CHANGE#${configId}`,
      sk: latest.timestamp,
      entityType: 'CONFIG_CHANGE',
      configId,
      timestamp: latest.timestamp,
      pathChanges
    } as ConfigChangeItem,
    ...pathChanges
      .map(({type, path, oldValue, newValue}) => ({
        pk: `CONFIG_PATH_CHANGE#${configId}`,
        sk: `${type}#${latest.timestamp}#${path}`,
        entityType: 'CONFIG_PATH_CHANGE',
        configId,
        type,
        path,
        oldValue,
        newValue,
        timestamp: latest.timestamp
      } as ConfigPathChangeItem))
  ];

  // Batch write config change items
  const output = await documentClient.send(new BatchWriteCommand({
    RequestItems: {
      [configTableName]: configChangeItems.map(Item => ({ PutRequest: { Item }}))
    }
  }));
  console.log('Batch write config changes output:', output);
}

async function handleInsertConfigChange(item: ConfigChangeItem) {
  const configChangeToSend = stripDbKeysFromConfigTableItem(item);
  // Publish to SNS topic
  await snsClient.send(new PublishCommand({
    TopicArn: configChangesTopicArn,
    Message: JSON.stringify(configChangeToSend, null, 2),
  }));
}
