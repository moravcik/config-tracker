import { NestedStack, NestedStackProps } from 'aws-cdk-lib';
import {
  AccessLogFormat,
  ApiKeySourceType,
  IApiKey,
  JsonSchema,
  LambdaIntegration,
  LogGroupLogDestination,
  MethodLoggingLevel,
  Period,
  RestApi
} from 'aws-cdk-lib/aws-apigateway';
import { ITable } from 'aws-cdk-lib/aws-dynamodb';
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';
import { LogGroup } from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';
import { configSchema } from '../lib/types/config.schema';
import { baseLambdaProps, resourcePrefix } from './config-tracker.app';

export class ApiNestedStack extends NestedStack {

  public readonly api: RestApi;
  public readonly apiKey: IApiKey;

  constructor(scope: Construct, id: string, props: NestedStackProps & { configTable: ITable }) {
    super(scope, id, props);

    // Lambda functions
    const configApiHandler = new NodejsFunction(this, 'ConfigApiHandler', {
      entry: 'lib/lambda/config-api.handler.ts',
      environment: { CONFIG_TABLE_NAME: props.configTable.tableName },
      ...baseLambdaProps,
    });
    props.configTable.grantReadWriteData(configApiHandler);

    const configChangeApiHandler = new NodejsFunction(this, 'ConfigChangeApiHandler', {
      entry: 'lib/lambda/config-change-api.handler.ts',
      environment: { CONFIG_TABLE_NAME: props.configTable.tableName },
      ...baseLambdaProps,
    });
    props.configTable.grantReadData(configChangeApiHandler);

    // CloudWatch log group for API Gateway
    const apiLogGroup = new LogGroup(this, 'RestApiLogGroup', {
      logGroupName: `/aws/apigateway/${resourcePrefix}-config-api`
    });

    // API Gateway with logging enabled
    this.api = new RestApi(this, 'RestApi', {
      restApiName: `${resourcePrefix}-config-api`,
      description: 'Config Tracker API',
      apiKeySourceType: ApiKeySourceType.HEADER,
      defaultCorsPreflightOptions: { allowOrigins: ['*'] },
      cloudWatchRole: true,
      deployOptions: {
        stageName: 'prod',
        loggingLevel: MethodLoggingLevel.INFO,
        dataTraceEnabled: true,
        metricsEnabled: true,
        accessLogDestination: new LogGroupLogDestination(apiLogGroup),
        accessLogFormat: AccessLogFormat.jsonWithStandardFields()
      }
    });

    const configModel = this.api.addModel('ConfigModel', {
      contentType: 'application/json',
      modelName: 'Config',
      schema: configSchema as any as JsonSchema,
    });

    const apiKeyRequired = true;

    const configApiIntegration = new LambdaIntegration(configApiHandler);
    const configChangeApiIntegration = new LambdaIntegration(configChangeApiHandler);

    // Config API
    const configResource = this.api.root.addResource('config');

    // GET /config
    configResource.addMethod('GET', configApiIntegration, { apiKeyRequired });

    // POST /config
    configResource.addMethod('POST', configApiIntegration, {
      apiKeyRequired,
      requestModels: { 'application/json': configModel },
      requestValidatorOptions: { validateRequestBody: true }
    });

    const configIdResource = configResource.addResource('{configId}');

    // GET /config/{configId}
    configIdResource.addMethod('GET', configApiIntegration, { apiKeyRequired });

    // PUT /config/{configId}
    configIdResource.addMethod('PUT', configApiIntegration, {
      apiKeyRequired,
      requestModels: { 'application/json': configModel },
      requestValidatorOptions: { validateRequestBody: true }
    });

    // PATCH /config/{configId}
    configIdResource.addMethod('PATCH', configApiIntegration, { apiKeyRequired });

    // Config Change API
    const configChangeResource = configIdResource.addResource('change');

    // GET /config/{configId}/change;
    configChangeResource.addMethod('GET', configChangeApiIntegration, { apiKeyRequired });

    // API Key and Usage Plan
    this.apiKey = this.api.addApiKey('ApiKey', { description: 'Config Tracker API Key' });
    const apiUsagePlan = this.api.addUsagePlan('UsagePlan', {
      name: 'Config API Usage Plan',
      throttle: { rateLimit: 100, burstLimit: 200 },
      quota: { limit: 10000, period: Period.DAY },
    });
    apiUsagePlan.addApiStage({ stage: this.api.deploymentStage });
    apiUsagePlan.addApiKey(this.apiKey);

  }
}
