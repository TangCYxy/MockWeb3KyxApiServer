# Sample Python script for KYX risk checking

def kyxCheck(params):
    """
    Check if a transaction is risky based on the given parameters.
    
    Risk criteria:
    1. The amount is greater than 5000
    2. The address (from or to) starts with "1" after removing "0x" prefix
    
    Parameters:
    params (dict): A dictionary containing transaction parameters
                  - fromAddress: The sender address
                  - toAddress: The recipient address (optional)
                  - targetAddress: The address to check (optional)
                  - tokenName: The name of the token being transferred (optional)
                  - tokenAmount: The amount of tokens being transferred (optional)
                  - chainId: The blockchain chain ID
                  - txHash: The transaction hash (optional)
    
    Returns:
    dict: A dictionary containing risk assessment results
         - inRisk: Boolean indicating if the transaction is risky
         - riskDetail: String describing the risk details (if any)
    """
    # Get parameters
    from_address = params.get("fromAddress", "")
    to_address = params.get("toAddress", "")
    target_address = params.get("targetAddress", "")
    token_name = params.get("tokenName", "")
    token_amount = params.get("tokenAmount", 0)
    chain_id = params.get("chainId", 0)
    tx_hash = params.get("txHash", "")
    
    # Addresses to check (could be from, to, or target)
    addresses_to_check = []
    if from_address:
        addresses_to_check.append(from_address)
    if to_address:
        addresses_to_check.append(to_address)
    if target_address and target_address not in addresses_to_check:
        addresses_to_check.append(target_address)
    
    # Risk criteria 1: Amount > 5000
    amount_risk = token_amount and token_amount > 5000
    
    # Risk criteria 2: Address starts with "1" after removing "0x" prefix
    address_risk = False
    risk_address = ""
    for addr in addresses_to_check:
        if addr and addr.startswith("0x"):
            addr = addr[2:]  # Remove 0x prefix
        if addr and addr.startswith("1"):
            address_risk = True
            risk_address = addr
            break
    
    # Determine if risk exists and provide details
    is_risky = amount_risk or address_risk
    risk_detail = ""
    
    if is_risky:
        risk_detail = "money laundry or fraud"
        
        if amount_risk:
            risk_detail += f" - Large amount transaction: {token_amount}"
        
        if address_risk:
            risk_detail += f" - Suspicious address pattern: {risk_address}"
    
    # Return risk assessment
    return {
        "inRisk": is_risky,
        "riskDetail": risk_detail
    } 