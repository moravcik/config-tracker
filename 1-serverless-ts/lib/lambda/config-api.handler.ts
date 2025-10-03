import { PutCommand, ScanCommand, ScanCommandOutput } from '@aws-sdk/lib-dynamodb';
import type { NativeAttributeValue } from '@aws-sdk/util-dynamodb';
import Ajv, { ErrorObject, ValidateFunction } from 'ajv';
import { APIGatewayProxyEvent, Context } from 'aws-lambda';
import deepmerge from 'deepmerge';
import { Config, configSchema } from '../types/config.schema';
import { ConfigItem } from '../types/config-table-item';
import { errorResult, okResult } from '../utils/api-utils';
import {
  documentClient,
  getLatestConfigEntity,
  configTableName,
  stripDbKeysFromConfigTableItem
} from '../utils/dynamo-utils';

export async function handler(event: APIGatewayProxyEvent, context: Context) {
  context.callbackWaitsForEmptyEventLoop = false;

  const httpMethod = event.httpMethod.toUpperCase();
  const configId = event.pathParameters && event.pathParameters.configId;

  console.log(`config API: ${httpMethod}:${event.path}`, configId);

  if (httpMethod === 'POST' && !configId) {         // POST /config
    const config = JSON.parse(event.body!) as Config; // validated by API GW
    return await handleSave(config);

  } else if (httpMethod === 'GET' && !configId) {   // GET /config
    return await handleList();

  } else if (httpMethod === 'GET' && configId) {    // GET /config/{configId}
    return await handleGet(configId);

  } else if (httpMethod === 'PUT' && configId) {     // PUT /config/{configId}
    return await handleUpdate(event, configId, false);

  } else if (httpMethod === 'PATCH' && configId) {   // PATCH /config/{configId}
    return await handleUpdate(event, configId, true);
  } else {
    return errorResult('method/path not supported', 404);
  }
}

async function handleSave(config: Config, existingConfigId?: string) {
  const configId = existingConfigId ?? await generateConfigId();
  const timestamp = new Date().toISOString();

  const configItem: ConfigItem = {
    pk: `CONFIG#${configId}`,
    sk: timestamp,
    entityType: 'CONFIG',
    configId,
    timestamp,
    config,
  };
  const putOutput = await documentClient.send(
    new PutCommand({ TableName: configTableName, Item: configItem })
  );
  console.log("Config save output", putOutput);
  return okResult(stripDbKeysFromConfigTableItem(configItem));
}

async function handleList() {
  const configItems: ConfigItem[] = [];
  let LastEvaluatedKey: Record<string, NativeAttributeValue> | undefined = undefined;

  // fetch all items (paginated)
  do {
    const scanOutput: ScanCommandOutput = await documentClient.send(
      new ScanCommand({
        TableName: configTableName,
        ExclusiveStartKey: LastEvaluatedKey,
        FilterExpression: 'entityType = :et',
        ExpressionAttributeValues: { ':et': 'CONFIG' }
      })
    );
    LastEvaluatedKey = scanOutput.LastEvaluatedKey;
    configItems.push(...scanOutput.Items as ConfigItem[]);
  } while (LastEvaluatedKey);

  // resolve latest config per configId
  const itemsByConfigId = configItems.reduce((acc, item) => {
    if (!acc[item.configId] || acc[item.configId].timestamp < item.timestamp) {
      acc[item.configId] = item;
    }
    return acc;
  }, {} as Record<string, ConfigItem>);

  return okResult(
    Object.values(itemsByConfigId).map(stripDbKeysFromConfigTableItem)
  );
}

async function handleGet(configId: string){
  const latestConfigItems = await getLatestConfigEntity(configId);
  if (!latestConfigItems?.length) {
    return errorResult('Config not found', 404);
  } else {
    return okResult(stripDbKeysFromConfigTableItem(latestConfigItems[0]));
  }
}

async function handleUpdate(event: APIGatewayProxyEvent, configId: string, isPatch: boolean) {
  const existingConfigItems = await getLatestConfigEntity(configId);
  if (!existingConfigItems?.length) {
    return errorResult('Config not found', 404);
  }
  const latestConfig = existingConfigItems[0].config;
  const updateBody = JSON.parse(event.body!); // TODO validate

  const updatedConfig = isPatch
    ? deepmerge(
        latestConfig,
        updateBody as Partial<Config>,
        { arrayMerge: (target, source) => source }
      )
    : updateBody as Config;

  if (JSON.stringify(updatedConfig) === JSON.stringify(latestConfig)) {
    return errorResult('No update - Equal with latest Config version', 400);
  }
  const { valid, errors } = validateConfig(updatedConfig);
  if (!valid) {
    console.warn('Config schema validation failed', errors);
    return errorResult({ error: 'Config schema validation failed', details: errors }, 400);
  }
  return await handleSave(updatedConfig, configId);
}

async function generateConfigId(): Promise<string> {
  const { nanoid } = await import('nanoid');
  return nanoid();
}

let ajvConfigValidator: ValidateFunction;

function validateConfig(configCandidate: any): { valid: boolean, errors?: null | ErrorObject[] } {
  if (!ajvConfigValidator) {
    ajvConfigValidator = new Ajv().compile(configSchema);
  }
  const valid = ajvConfigValidator(configCandidate);
  return { valid, errors: ajvConfigValidator.errors };
}