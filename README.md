# Mock KYX API Services

This project contains a mock server implementation for KYX (Know Your Transaction/Address) API providers, including Chainalysis and GoPlus. The mock server allows for testing applications that integrate with these external security services.

## Project Structure

This repository consists of two main components:

### 1. `mock-kyx-api-server`

A Spring Boot application that implements a mock server for the Chainalysis and GoPlus API endpoints. The server allows you to simulate responses from these API providers for testing purposes, without needing to connect to the actual services.

The mock server supports:
- GoPlus address risk assessment
- Chainalysis KYA (Know Your Address) workflow
- Chainalysis KYT (Know Your Transaction) workflow

For more details, see the [mock-kyx-api-server README](mock-kyx-api-server/README.md).

### 2. `apiEndpoints`

Reference files containing API endpoint models and service implementations for:
- Chainalysis API structures
- GoPlus API structures

These files serve as documentation and reference for the actual API structures implemented in the mock server.

## Getting Started

To begin working with the mock KYX API server:

1. Navigate to the mock-kyx-api-server directory
2. Run the server using `mvn spring-boot:run` or build it with `mvn clean package`
3. Test the endpoints using the examples provided in the server's README

## Testing

The mock server includes risk assessment logic that flags addresses that start with "1" (after removing "0x" prefix) and transactions with amounts greater than 5000 as risky. This allows for testing both positive and negative risk detection scenarios. 