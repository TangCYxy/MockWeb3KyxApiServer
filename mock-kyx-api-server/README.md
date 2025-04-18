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

### GoPlus Provider API

GoPlus only provides a single GET endpoint for address risk assessment:

```
GET /api/goplus/address/{address}
```

### Chainalysis Provider API

Chainalysis uses a multi-step process for both KYA and KYT checks:

#### KYA (Know Your Address) Flow:

Step 1: Register an address for monitoring
```
POST /api/chainalysis/kya/register
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
GET /api/chainalysis/kya/register/{externalId}
```

Step 3: Get alerts for the address
```
GET /api/chainalysis/kya/alerts/{externalId}
```

#### KYT (Know Your Transaction) Flow:

Step 1: Register a transaction for monitoring
```
POST /api/chainalysis/kyt/register
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

Step 2: Get alerts for the transaction
```
GET /api/chainalysis/kyt/alerts/{externalId}
```

#### Additional Monitoring:

Get all active alerts:
```
GET /api/chainalysis/monitoring
```

Legacy transaction check:
```
POST /api/chainalysis/check
```

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