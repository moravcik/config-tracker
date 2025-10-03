import { FromSchema, JSONSchema } from 'json-schema-to-ts';

export const configSchema = {
  // "$schema": "http://json-schema.org/draft-04/schema#", // AJV doesn't support "$schema" property
  "type": "object",
  "properties": {
    "creditPolicy": {
      "type": "object",
      "properties": {
        "maxCreditLimit": { "type": "number" },
        "minCreditScore": { "type": "number" },
        "currency": {
          "type": "string",
          "enum": ["EUR", "USD", "GBP"]
        },
        "exceptions": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "segment": { "type": "string" },
              "maxCreditLimit": { "type": "number" },
              "requiresTwoManRule": { "type": "boolean" }
            },
            "required": ["segment"],
            "additionalProperties": false
          }
        }
      },
      "required": ["maxCreditLimit", "minCreditScore", "currency"],
      "additionalProperties": false
    },
    "approvalPolicy": {
      "type": "object",
      "properties": {
        "twoManRule": { "type": "boolean" },
        "autoApproveThreshold": { "type": "number" },
        "levels": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "role": {
                "type": "string",
                "enum": ["TEAM_LEAD", "HEAD_OF_CREDIT", "CFO"]
              },
              "limit": { "type": "number" }
            },
            "required": ["role", "limit"],
            "additionalProperties": false
          }
        }
      },
      "required": ["twoManRule", "autoApproveThreshold", "levels"],
      "additionalProperties": false
    },
    "riskScoring": {
      "type": "object",
      "properties": {
        "weights": {
          "type": "object",
          "properties": {
            "incomeToDebtRatio": { "type": "number" },
            "age": { "type": "number" },
            "historyLengthMonths": { "type": "number" },
            "delinquencyCount": { "type": "number" }
          },
          "required": ["incomeToDebtRatio", "historyLengthMonths", "delinquencyCount"],
          "additionalProperties": false
        },
        "thresholds": {
          "type": "object",
          "properties": {
            "low": { "type": "number" },
            "medium": { "type": "number" },
            "high": { "type": "number" }
          },
          "required": ["low", "medium", "high"],
          "additionalProperties": false
        }
      },
      "required": ["weights", "thresholds"],
      "additionalProperties": false
    }
  },
  "additionalProperties": false
} as const satisfies JSONSchema;

export type Config = FromSchema<typeof configSchema>;

// export interface Config {
//   creditPolicy?: CreditPolicyConfig;
//   approvalPolicy?: ApprovalPolicyConfig;
//   riskScoring?: RiskScoringConfig;
// }
//
// export interface CreditPolicyConfig {
//   maxCreditLimit: number;
//   minCreditScore: number;
//   currency: 'EUR' | 'USD' | 'GBP';
//   exceptions?: CreditPolicyExceptionConfig[];
// }
//
// export interface CreditPolicyExceptionConfig {
//   segment: string; // e.g., "VIP", "NewCustomer"
//   maxCreditLimit?: number;
//   requiresTwoManRule?: boolean;
// }
//
// export interface ApprovalPolicyConfig {
//   twoManRule: boolean;
//   autoApproveThreshold: number;
//   levels: ApprovalPolicyLevelConfig[];
// }
//
// export interface ApprovalPolicyLevelConfig {
//   role: 'TEAM_LEAD' | 'HEAD_OF_CREDIT' | 'CFO';
//   limit: number;
// }
//
//
// export interface RiskScoringConfig {
//   weights: {
//     incomeToDebtRatio: number;
//     age: number;
//     historyLengthMonths: number;
//     delinquencyCount: number;
//   };
//   thresholds: {
//     low: number;
//     medium: number;
//     high: number;
//   };
// }
