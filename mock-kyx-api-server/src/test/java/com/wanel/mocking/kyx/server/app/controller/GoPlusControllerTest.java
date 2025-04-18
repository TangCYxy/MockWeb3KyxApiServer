package com.wanel.mocking.kyx.server.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanel.mocking.kyx.server.bean.RiskCheckResult;
import com.wanel.mocking.kyx.server.bean.TransactionCheckRequest;
import com.wanel.mocking.kyx.server.core.service.RiskCheckService;

@WebMvcTest(GoPlusController.class)
public class GoPlusControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private RiskCheckService riskCheckService;
    
    @Test
    public void testCheckTransaction_WhenRequestIsValid_ShouldReturnSuccess() throws Exception {
        // Arrange
        TransactionCheckRequest request = TransactionCheckRequest.builder()
                .fromAddress("0x123")
                .toAddress("0x456")
                .tokenName("ETH")
                .tokenAmount(100.0)
                .chainId(1)
                .build();
        
        RiskCheckResult result = RiskCheckResult.builder()
                .inRisk(false)
                .riskDetail("")
                .build();
        
        when(riskCheckService.checkRisk(any(TransactionCheckRequest.class))).thenReturn(result);
        
        // Act & Assert
        mockMvc.perform(post("/api/goplus/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.isRisky").value(false))
                .andExpect(jsonPath("$.data.reason").value(""));
    }
    
    @Test
    public void testCheckTransaction_WhenTransactionIsRisky_ShouldReturnRiskInfo() throws Exception {
        // Arrange
        TransactionCheckRequest request = TransactionCheckRequest.builder()
                .fromAddress("0x123")
                .toAddress("0x456")
                .tokenName("ETH")
                .tokenAmount(2000.0)
                .chainId(1)
                .build();
        
        RiskCheckResult result = RiskCheckResult.builder()
                .inRisk(true)
                .riskDetail("Large amount transaction")
                .build();
        
        when(riskCheckService.checkRisk(any(TransactionCheckRequest.class))).thenReturn(result);
        
        // Act & Assert
        mockMvc.perform(post("/api/goplus/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.isRisky").value(true))
                .andExpect(jsonPath("$.data.reason").value("Large amount transaction"));
    }
    
    @Test
    public void testCheckTransaction_WhenRequestIsInvalid_ShouldReturnBadRequest() throws Exception {
        // Arrange
        TransactionCheckRequest request = TransactionCheckRequest.builder()
                .fromAddress("") // Invalid - empty address
                .toAddress("0x456")
                .tokenName("ETH")
                .tokenAmount(100.0)
                .chainId(1)
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/goplus/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
} 