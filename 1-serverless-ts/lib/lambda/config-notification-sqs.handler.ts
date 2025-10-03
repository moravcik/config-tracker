import { Context, SQSEvent } from 'aws-lambda';

export function handler(event: SQSEvent, context: Context) {
  event.Records.forEach(record => {
    const configChangeNotification = JSON.parse(record.body);
    console.log("Config change notification", configChangeNotification);
    // TODO process the config change notification, e.g., send email or trigger other workflows
  });
}
