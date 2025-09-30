### Scenario:

You’ve joined the Mid-Office team. You’re tasked with creating a configuration change tracker that logs, stores, and retrieves changes to domain-specific rules (e.g., credit limits, approval policies). It should also notify an external monitoring service of any critical changes in cofiguration.

#### Requirements:
- Java 17+ (Java 21 preferred)
- Spring Boot 3.x
- REST API with at least:
  - Create new config change (add/update/delete)
  - List changes by time or type
  - Retrieve specific change by ID
- In-memory persistence (no DB required)
- Input validation with clear error handling
- One simulated external integration (notification or logging)
- Health check endpoint
- Unit + integration tests
- Clear README with rationale, assumptions, and how to run it
- Add infrastructure as as code

#### Config example:
```json
{
  "config": {
    "creditPolicy": {
      "maxCreditLimit": 50000,
      "minCreditScore": 620,
      "currency": "EUR",
      "exceptions": [{
        "segment": "VIP",
        "maxCreditLimit": 150000,
        "requiresTwoManRule": true
      }]
    },
    "approvalPolicy": {
      "twoManRule": true,
      "autoApproveThreshold": 2000,
      "levels": [
        { "role": "TEAM_LEAD", "limit": 10000 },
        { "role": "HEAD_OF_CREDIT", "limit": 50000 }
      ]
    },
    "riskScoring": {
      "weights": {
        "incomeToDebtRatio": 0.4,
        "age": 0.1,
        "historyLengthMonths": 0.2,
        "delinquencyCount": 0.3
      },
      "thresholds": {
        "low": 700,
        "medium": 650,
        "high": 600
      }
    }
  },
  "metadata": {
    "version": 7,
    "effectiveFrom": "2025-09-20T00:00:00Z"
  }
}
```
