#!/usr/bin/env python3
"""
Test script for kyx_script.py
This script tests the risk check functionality independently of the Java application.
"""

import sys
import json
from pathlib import Path

# Determine script location and import kyx_script
try:
    # Try loading from src/main/resources/scripts first (if available)
    scripts_dir = Path("src/main/resources/scripts")
    if scripts_dir.exists() and (scripts_dir / "kyx_script.py").exists():
        sys.path.append(str(scripts_dir.absolute()))
        from kyx_script import kyxCheck
        print(f"Loaded kyx_script.py from {scripts_dir}")
    else:
        # Try loading from current directory
        from kyx_script import kyxCheck
        print("Loaded kyx_script.py from current directory")
except ImportError as e:
    print(f"Error importing kyx_script.py: {e}")
    sys.exit(1)

def test_case(name, params, expected_risk):
    """Run a test case and print the results"""
    print(f"\n=== Test Case: {name} ===")
    print(f"Input: {json.dumps(params, indent=2)}")
    
    result = kyxCheck(params)
    
    print(f"Result: {json.dumps(result, indent=2)}")
    
    if result.get("inRisk") == expected_risk:
        print(f"✓ TEST PASSED - Risk detection {'matched' if expected_risk else 'not triggered'} as expected")
    else:
        print(f"✗ TEST FAILED - Expected risk: {expected_risk}, Got: {result.get('inRisk')}")
    
    return result

# Define test cases
test_cases = [
    {
        "name": "Normal transaction - No risk",
        "params": {
            "fromAddress": "0xabcdef1234567890",
            "toAddress": "0xfedcba0987654321",
            "tokenName": "ETH",
            "tokenAmount": 100.0,
            "chainId": 1
        },
        "expected_risk": False
    },
    {
        "name": "Large amount transaction - Should be risky",
        "params": {
            "fromAddress": "0xabcdef1234567890",
            "toAddress": "0xfedcba0987654321",
            "tokenName": "ETH",
            "tokenAmount": 6000.0,
            "chainId": 1
        },
        "expected_risk": True
    },
    {
        "name": "Address starting with '1' - Should be risky",
        "params": {
            "fromAddress": "0x1abcdef1234567890",
            "toAddress": "0xfedcba0987654321",
            "tokenName": "ETH",
            "tokenAmount": 100.0,
            "chainId": 1
        },
        "expected_risk": True
    },
    {
        "name": "KYA check with risky address",
        "params": {
            "targetAddress": "0x1abcdef1234567890",
            "chainId": 1
        },
        "expected_risk": True
    },
    {
        "name": "KYT check with high amount",
        "params": {
            "fromAddress": "0xabcdef1234567890",
            "toAddress": "0xfedcba0987654321",
            "tokenName": "ETH",
            "tokenAmount": 8000.0,
            "chainId": 1,
            "txHash": "0xabcdef1234567890"
        },
        "expected_risk": True
    }
]

def main():
    """Run all test cases"""
    print("=== Testing kyx_script.py ===")
    print("This script verifies that the risk check functionality works as expected.")
    
    passed = 0
    failed = 0
    
    for tc in test_cases:
        result = test_case(tc["name"], tc["params"], tc["expected_risk"])
        if result.get("inRisk") == tc["expected_risk"]:
            passed += 1
        else:
            failed += 1
    
    print(f"\n=== Test Summary ===")
    print(f"Total tests: {len(test_cases)}")
    print(f"Passed: {passed}")
    print(f"Failed: {failed}")
    
    if failed > 0:
        print("\nSome tests failed! Please check the risk detection logic.")
        sys.exit(1)
    else:
        print("\nAll tests passed! The risk detection logic is working correctly.")

if __name__ == "__main__":
    main() 