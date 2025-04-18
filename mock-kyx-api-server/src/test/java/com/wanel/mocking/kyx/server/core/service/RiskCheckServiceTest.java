package com.wanel.mocking.kyx.server.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wanel.mocking.kyx.server.app.config.KyxConfiguration;
import com.wanel.mocking.kyx.server.app.config.KyxConfiguration.Python;
import com.wanel.mocking.kyx.server.bean.RiskCheckResult;
import com.wanel.mocking.kyx.server.bean.TransactionCheckRequest;
import com.wanel.mocking.kyx.server.core.service.impl.RiskCheckServiceImpl;

@ExtendWith(MockitoExtension.class)
public class RiskCheckServiceTest {

    @Mock
    private PythonScriptExecutor pythonScriptExecutor;
    
    @Mock
    private KyxConfiguration kyxConfiguration;
    
    @Mock
    private Python pythonConfig;
    
    @InjectMocks
    private RiskCheckServiceImpl riskCheckService;
    
    @BeforeEach
    public void setUp() {
        when(kyxConfiguration.getPython()).thenReturn(pythonConfig);
        when(pythonConfig.getFunctionName()).thenReturn("kyxCheck");
    }
    
    @Test
    public void testCheckRisk_WhenTransactionIsRisky_ShouldReturnRiskResult() {
        // Arrange
        TransactionCheckRequest request = TransactionCheckRequest.builder()
                .fromAddress("0x123")
                .toAddress("0x456")
                .tokenName("ETH")
                .tokenAmount(2000.0)
                .chainId(1)
                .build();
        
        Map<String, Object> pythonResult = new HashMap<>();
        pythonResult.put("inRisk", true);
        pythonResult.put("riskDetail", "Large amount transaction");
        
        when(pythonScriptExecutor.executeFunction(eq("kyxCheck"), any())).thenReturn(pythonResult);
        
        // Act
        RiskCheckResult result = riskCheckService.checkRisk(request);
        
        // Assert
        assertTrue(result.isInRisk());
        assertEquals("Large amount transaction", result.getRiskDetail());
    }
    
    @Test
    public void testCheckRisk_WhenTransactionIsNotRisky_ShouldReturnSafeResult() {
        // Arrange
        TransactionCheckRequest request = TransactionCheckRequest.builder()
                .fromAddress("0x123")
                .toAddress("0x456")
                .tokenName("ETH")
                .tokenAmount(100.0)
                .chainId(1)
                .build();
        
        Map<String, Object> pythonResult = new HashMap<>();
        pythonResult.put("inRisk", false);
        pythonResult.put("riskDetail", "");
        
        when(pythonScriptExecutor.executeFunction(eq("kyxCheck"), any())).thenReturn(pythonResult);
        
        // Act
        RiskCheckResult result = riskCheckService.checkRisk(request);
        
        // Assert
        assertFalse(result.isInRisk());
        assertEquals("", result.getRiskDetail());
    }
} 