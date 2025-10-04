# Config Tracker API - Java Lambda Solution

## Prerequisites

- Java 21+
- Maven 3.8+
- Node.js v22+
- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) configured with AWS credentials
- AWS CDK CLI: `npm install -g aws-cdk`

## Setup
```bash
cdk bootstrap  # One-time for AWS account/region combination
```

## Development

```bash
mvn test            # Run tests
mvn package         # Build and test
cdk ls              # List stacks, good to starts with this command, compiles and runs CDK code
cdk diff            # Preview changes compared to deployed AWS resources
cdk deploy          # Deploy to AWS, both infrastructure and code
cdk destroy         # Clean up AWS resources
```

After code change run:
```bash
mvn package && cdk deploy
```

After successful deployment you will find output values of API Url and Get API Key command, similar to this:
```bash
Outputs:
config-tracker-2-serverless-java-stack.RestApiKeyCommand = aws apigateway get-api-key --api-key ch3v02lbsa --include-value  --query 'value' --output text
config-tracker-2-serverless-java-stack.RestApiUrl = https://p7tfbik6a6.execute-api.eu-west-1.amazonaws.com/prod/
```

You can use the AWS command from above output to retrieve the API Key value:
```bash
aws apigateway get-api-key --api-key ch3v02lbsa --include-value  --query 'value' --output text
```

## API Usage

Import the [postman_collection.json](../postman_collection.json) from parent directory into [Postman](https://www.postman.com/) and test all API endpoints.

It is preconfigured with API Gateway URL and API key of existing AWS deployment.

You test requests in order in which they are defined. Switch environment in top right corner.

![postman](../doc/images/postman.png)

Alternatively you can test the API endpoints in terminal using curl:
```bash
# Get API key and endpoint from CDK output or AWS console
export API_KEY="your-api-key"
export API_URL="your-api-gateway-url"

# List configurations
curl "$API_URL/config" -H "x-api-key: $API_KEY"
```

See parent README for additional information.
