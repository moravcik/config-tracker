import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { DynamoDBDocumentClient, QueryCommand } from '@aws-sdk/lib-dynamodb';
import { ConfigItem, ConfigTableItem } from '../types/config-table-item';

export const documentClient = DynamoDBDocumentClient.from(
  new DynamoDBClient(),
  { marshallOptions: { removeUndefinedValues: true }}
);

export const configTableName = process.env.CONFIG_TABLE_NAME!;

export async function getLatestConfigEntity(configId: string, limit = 1): Promise<ConfigItem[]> {
  const { Items } = await documentClient.send(new QueryCommand({
    TableName: configTableName,
    KeyConditionExpression: 'pk = :pk',
    ExpressionAttributeValues: { ':pk': `CONFIG#${configId}` },
    ScanIndexForward: false, // descending order of "timestamp" - latest first
    Limit: limit,
  }));
  console.log(`Latest (${limit}) config by configId (${configId})`, Items);
  return Items as ConfigItem[];
}

export const stripDbKeysFromConfigTableItem = ({ pk, sk, entityType, ...ci }: ConfigTableItem) => ci;
