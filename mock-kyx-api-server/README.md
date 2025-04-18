# Mock KYX API Server

A mock server for simulating KYX (Know Your Transaction) API providers: GoPlus and Chainalysis.

## Overview

This service mocks external KYX API providers to enable testing of applications that integrate with these services. It accepts HTTP API requests in the same format as the real providers and returns configurable responses based on a Python script that determines risk assessment.

## Features

- Mock server for GoPlus and Chainalysis API providers
- Configurable via Nacos configuration server
- Custom Python script execution for risk assessment
- Spring Boot-based RESTful API
- Support for KYA (Know Your Address) and KYT (Know Your Transaction) checks

## Risk Assessment Logic

The mock server uses the following criteria to determine if a transaction or address is risky:
1. If the transaction amount is greater than 5000
2. If any address (from, to, or target) starts with "1" after removing the "0x" prefix

When a risk is detected, the response will include "money laundry or fraud" in the risk details.

## API Endpoints

The mock server directly exposes the same endpoints as the original providers, allowing you to simply change the host URL in your configuration to point to this server instead of the real providers.

### GoPlus Provider API

GoPlus only provides a single GET endpoint for address risk assessment:

```
GET /address/{address}
```

### Chainalysis Provider API

Chainalysis uses a multi-step process for both KYA and KYT checks:

#### KYA (Know Your Address) Flow:

Step 1: Register an address for monitoring
```
POST /api/kyt/v2/users/{userId}/withdrawal-attempts
```
```json
{
  "targetAddress": "0x123abc...",
  "chainId": 1,
  "assetName": "ETH"
}
```

Step 2: Check registration status
```
GET /api/kyt/v2/withdrawal-attempts/{externalId}
```

Step 3: Get alerts for the address
```
GET /api/kyt/v2/withdrawal-attempts/{externalId}/alerts
```

#### KYT (Know Your Transaction) Flow:

Step 1: Register a transaction for monitoring
```
POST /api/kyt/v2/users/{userId}/transfers
```
```json
{
  "fromAddress": "0x123abc...",
  "toAddress": "0x456def...",
  "chainId": 1,
  "tokenName": "ETH",
  "tokenAmount": 100.0,
  "txHash": "0x789..." 
}
```

Step 2: Check registration status
```
GET /api/kyt/v2/transfers/{externalId}
```

Step 3: Get alerts for the transaction
```
GET /api/kyt/v2/transfers/{externalId}/alerts
```

#### Additional Monitoring:

Get all active alerts:
```
GET /api/kyt/v1/alerts?createdAt_lte={endTime}&createdAt_gte={startTime}&limit={limit}&offset={offset}
```

## Usage

To use this mock server instead of the real providers, simply update the host URL in your application's configuration to point to this server. For example:

```
# Original configuration
goplus.url=https://api.gopluslabs.io/api/v1/...

# Updated configuration for mock server
goplus.url=http://localhost:8080/...

# Original Chainalysis configuration
chainalysis.host=https://api.chainalysis.com

# Updated configuration for mock server
chainalysis.host=http://localhost:8080
```

### Example API Tests

#### GoPlus Tests

Test a risky address (returns positive risk):
```bash
curl -X GET "http://localhost:8080/address/0x123456789abcdef"
```

Expected response:
```json
{"code":1,"message":"ok","result":{"cybercrime":null,"money_laundering":"1","number_of_malicious_contracts_created":null,"gas_abuse":null,"financial_crime":null,"darkweb_transactions":null,"reinit":null,"phishing_activities":null,"fake_kyc":null,"blacklist_doubt":null,"fake_standard_interface":null,"data_source":"Mock KYX Server","stealing_attack":null,"blackmail_activities":null,"sanctioned":null,"malicious_mining_activities":null,"mixer":null,"honeypot_related_address":null,"inRisk":true},"inRisk":true}
```

Test a non-risky address (returns no risk):
```bash
curl -X GET "http://localhost:8080/address/0x2abcdef0123456789"
```

Expected response:
```json
{"code":1,"message":"ok","result":{"cybercrime":"0","money_laundering":"0","number_of_malicious_contracts_created":"0","gas_abuse":"0","financial_crime":"0","darkweb_transactions":"0","reinit":"0","phishing_activities":"0","fake_kyc":"0","blacklist_doubt":"0","fake_standard_interface":"0","data_source":"Mock KYX Server","stealing_attack":"0","blackmail_activities":"0","sanctioned":"0","malicious_mining_activities":"0","mixer":"0","honeypot_related_address":"0","inRisk":false},"inRisk":false}
```

#### Chainalysis Tests

##### KYA (Know Your Address) Flow:

Step 1: Register an address (returns an externalId to use in subsequent requests):
```bash
curl -X POST "http://localhost:8080/api/kyt/v2/users/testuser/withdrawal-attempts" -H "Content-Type: application/json" -d '{"targetAddress": "0x123456789abcdef", "chainId": 1, "assetName": "ETH"}'
```

Example response:
```json
{"updatedAt":"2025-04-18T10:05:44.452790Z","asset":"ETH","network":"ethereum","address":"0x123456789abcdef","attemptIdentifier":"197ee5d1-b959-47ad-95d8-21fe0f6be1de","usdAmount":null,"assetAmount":1,"externalId":"32b63d61-7495-494c-805a-a151906dc176"}
```

Step 2: Check registration status (using the externalId from Step 1):
```bash
curl -X GET "http://localhost:8080/api/kyt/v2/withdrawal-attempts/32b63d61-7495-494c-805a-a151906dc176"
```

Example response:
```json
{"updatedAt":"2025-04-18T10:05:44.452790Z","asset":null,"network":null,"address":null,"attemptIdentifier":null,"usdAmount":null,"assetAmount":null,"externalId":"32b63d61-7495-494c-805a-a151906dc176"}
```

Step 3: Get risk alerts (this is where the risk check is actually performed):
```bash
curl -X GET "http://localhost:8080/api/kyt/v2/withdrawal-attempts/32b63d61-7495-494c-805a-a151906dc176/alerts"
```

Example response (high risk detected):
```json
{"alerts":[{"alertLevel":"HIGH","category":"money_laundering_fraud","service":"Mock KYX Server","externalId":"32b63d61-7495-494c-805a-a151906dc176","alertAmount":1000,"exposureType":"DIRECT"}]}
```

If no risk is detected, the response will be:
```json
{"alerts":[]}
```

Example testing a risky address (starting with "0x1" to trigger a risk detection):
```bash
curl -X POST "http://localhost:8080/api/kyt/v2/users/testuser/withdrawal-attempts" -H "Content-Type: application/json" -d '{"targetAddress": "0x1234abcdef5678", "chainId": 1, "assetName": "ETH"}'
```
Then follow up with status check and alerts retrieval using the externalId from the response.

##### KYT (Know Your Transaction) Flow:

Step 1: Register a transaction (returns an externalId to use in subsequent requests):
```bash
curl -X POST "http://localhost:8080/api/kyt/v2/users/testuser/transfers" -H "Content-Type: application/json" -d '{"fromAddress": "0x2abcdef0123456789", "toAddress": "0x2def0123456789abcdef", "chainId": 1, "tokenName": "ETH", "tokenAmount": 1.0}'
```

Example response:
```json
{"updatedAt":null,"asset":"ETH","network":"ethereum","transferReference":"tx:0x2def0123456789abcdef","tx":"ef979a37-93e7-4ba0-9ae9-2b0dd7e8a8fd","idx":0,"usdAmount":1000.0,"assetAmount":1.0,"timestamp":"2025-04-18T10:06:44.495059Z","outputAddress":"0x2def0123456789abcdef","externalId":"c14de666-f5ee-4e30-829e-c65652dfaf00"}
```

Step 2: Check registration status (using the externalId from Step 1):
```bash
curl -X GET "http://localhost:8080/api/kyt/v2/transfers/c14de666-f5ee-4e30-829e-c65652dfaf00"
```

Step 3: Get risk alerts (this is where the risk check is actually performed):
```bash
curl -X GET "http://localhost:8080/api/kyt/v2/transfers/c14de666-f5ee-4e30-829e-c65652dfaf00/alerts"
```

Example response (no risk detected):
```json
{"alerts":[]}
```

Example testing a risky transaction (with address starting with "0x1" to trigger risk detection):
```bash
curl -X POST "http://localhost:8080/api/kyt/v2/users/testuser/transfers" -H "Content-Type: application/json" -d '{"fromAddress": "0x1234abcdef5678", "toAddress": "0xdef456789abc", "chainId": 1, "tokenName": "ETH", "tokenAmount": 1.0}'
```
Then check alerts using the received externalId to see the risk detection response:
```json
{"alerts":[{"alertLevel":"HIGH","category":"money_laundering_fraud","service":"Mock KYX Server","externalId":"[EXTERNAL_ID]","alertAmount":1000,"exposureType":"DIRECT"}]}
```

For transactions considered risky (e.g., with addresses starting with "1"), the alert response would contain alerts.

## Configuration

### Nacos Configuration

The server is configured to connect to Nacos:
- Server: http://192.168.3.227:8848/
- Namespace: wanel
- Group: DEFAULT_GROUP
- Data ID: mock-kyx-api-server.yml

### Application Properties

Key configuration properties:
```yaml
kyx:
  python:
    script-path: /path/to/kyx_script.py
    function-name: kyxCheck
  providers:
    - name: goplus
      enabled: true
    - name: chainalysis
      enabled: true
```

## Python Script

The Python script defines a function named `kyxCheck` that:
- Accepts a dictionary of transaction parameters
- Returns a dictionary with risk assessment results

Example python function signature:
```python
def kyxCheck(params):
    # params contains parameters like: fromAddress, toAddress, targetAddress, tokenName, tokenAmount, chainId, txHash, etc.
    # return a dictionary with keys: inRisk, riskDetail
```

## Getting Started

### Running the Application

```bash
mvn spring-boot:run
```

## Customizing Responses

To customize the risk assessment logic, modify the Python script in `src/main/resources/scripts/kyx_script.py` or provide your own script path in the configuration. 