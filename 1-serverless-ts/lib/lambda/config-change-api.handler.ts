import { BatchGetCommand, QueryCommand, QueryCommandOutput } from '@aws-sdk/lib-dynamodb';
import type { NativeAttributeValue } from '@aws-sdk/util-dynamodb';
import { APIGatewayProxyEvent, Context } from 'aws-lambda';
import { ConfigChangeItem, ConfigPathChangeItem, ConfigTableKeys } from '../types/config-table-item';
import { errorResult, okResult } from '../utils/api-utils';
import { configTableName, documentClient, stripDbKeysFromConfigTableItem } from '../utils/dynamo-utils';

const allowedParams = ['type', 'path', 'timestampFrom', 'timestampTo'];

export async function handler(event: APIGatewayProxyEvent, context: Context) {
  context.callbackWaitsForEmptyEventLoop = false;

  const httpMethod = event.httpMethod.toUpperCase();
  const configId = event.pathParameters && event.pathParameters.configId;
  const { type, path, timestampFrom, timestampTo } = event.queryStringParameters || {};

  console.log(`config changes API: ${httpMethod}:${event.path}`, configId, event.queryStringParameters);

  const invalidParams = Object.keys(event.queryStringParameters || {})
    .filter(p => !allowedParams.includes(p));

  if (invalidParams.length) {
    return errorResult(`Invalid query parameters: ${invalidParams.join(', ')}`, 400);
  }

  let configChangeItems: ConfigChangeItem[] = [];

  if (httpMethod === 'GET' && configId) {
    if (type || path) { // GET /config/{configId}/change?type=...&path=...?&timestampFrom=...&timestampTo=...
      configChangeItems = await queryByConfigPathChanges(configId, type, path, timestampFrom, timestampTo);

    } else { // GET /config/{configId}/change?timestampFrom=...&timestampTo=...
      configChangeItems = await queryConfigChanges(configId, timestampFrom, timestampTo);
    }
  }
  return okResult(
    configChangeItems.map(stripDbKeysFromConfigTableItem)
  );
}

async function queryConfigChanges(configId: string, timestampFrom?: string, timestampTo?: string): Promise<ConfigChangeItem[]> {
  const configChangeItems: ConfigChangeItem[] = [];
  let LastEvaluatedKey: Record<string, NativeAttributeValue> | undefined = undefined;
  do {
    const queryOutput: QueryCommandOutput = await documentClient.send(
      new QueryCommand({
        TableName: configTableName,
        ExclusiveStartKey: LastEvaluatedKey,
        KeyConditionExpression: 'pk = :pk'
          + (timestampFrom && timestampTo ? ' AND (sk BETWEEN :tsFrom AND :tsTo)' : '')
          + (timestampFrom && !timestampTo ? ' AND sk >= :tsFrom' : '')
          + (!timestampFrom && timestampTo ? ' AND sk <= :tsTo' : ''),
        ExpressionAttributeValues: {
          ':pk': `CONFIG_CHANGE#${configId}`,
          ...(timestampFrom ? { ':tsFrom': timestampFrom } : {}),
          ...(timestampTo ? { ':tsTo': timestampTo } : {})
        },
      })
    );
    LastEvaluatedKey = queryOutput.LastEvaluatedKey;
    configChangeItems.push(...queryOutput.Items as ConfigChangeItem[]);
  } while (LastEvaluatedKey);
  return configChangeItems;
}

async function queryByConfigPathChanges(configId: string, type?: string, path?: string, timestampFrom?: string, timestampTo?: string): Promise<ConfigChangeItem[]> {
  const configPathChangeItems: ConfigPathChangeItem[] = [];
  let LastEvaluatedKey: Record<string, NativeAttributeValue> | undefined = undefined;
  do {
    const filterExpressions = [
      path ? 'contains(#path, :path)' : undefined,
      timestampFrom && timestampTo ? '(#timestamp BETWEEN :tsFrom AND :tsTo)' : undefined,
      timestampFrom && !timestampTo ? '#timestamp >= :tsFrom' : undefined,
      !timestampFrom && timestampTo ? '#timestamp <= :tsTo' : undefined,
    ].filter(Boolean);

    const expressionAttributeNameEntries = [
      path ? ['#path', 'path'] : undefined,
      (timestampFrom || timestampTo) ? ['#timestamp', 'timestamp'] : undefined,
    ].filter(Boolean) as [string, string][];

    const queryOutput: QueryCommandOutput = await documentClient.send(
      new QueryCommand({
        TableName: configTableName,
        ExclusiveStartKey: LastEvaluatedKey,
        KeyConditionExpression: 'pk = :pk'
          + (type ? ' AND begins_with(sk, :type)' : ''),
        FilterExpression: filterExpressions.length
          ? filterExpressions.join(' AND ')
          : undefined,
        ExpressionAttributeNames: expressionAttributeNameEntries.length
          ? Object.fromEntries(expressionAttributeNameEntries)
          : undefined,
        ExpressionAttributeValues: {
          ':pk': `CONFIG_PATH_CHANGE#${configId}`,
          ...(type ? { ':type': `${type}#` } : {}),
          ...(path ? { ':path': path } : {}),
          ...(timestampFrom ? { ':tsFrom': timestampFrom } : {}),
          ...(timestampTo ? { ':tsTo': timestampTo } : {})
        }
      })
    );
    LastEvaluatedKey = queryOutput.LastEvaluatedKey;
    configPathChangeItems.push(...queryOutput.Items as ConfigPathChangeItem[]);
  } while (LastEvaluatedKey);

  // Aggregate ConfigChangeItem keys from configPathChangeItems
  const configChangeKeys = [...configPathChangeItems.reduce(
    // sk of ConfigChangeItem is the timestamp
    (acc, pc) => (acc.add(pc.timestamp), acc),
    new Set<string>()
  )].map(sk => ({ pk: `CONFIG_CHANGE#${configId}`, sk }) as ConfigTableKeys);

  if (!configChangeKeys.length) return [];

  // Batch get ConfigChangeItems by keys
  const batchGetOutput = await documentClient.send(new BatchGetCommand({
    RequestItems: { [configTableName]: { Keys: configChangeKeys }}
  }));
  return (batchGetOutput.Responses ? batchGetOutput.Responses[configTableName] as ConfigChangeItem[] : []);
}