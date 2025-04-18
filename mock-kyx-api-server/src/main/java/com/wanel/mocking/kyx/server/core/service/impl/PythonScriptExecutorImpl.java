package com.wanel.mocking.kyx.server.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanel.mocking.kyx.server.app.config.KyxConfiguration;
import com.wanel.mocking.kyx.server.core.service.PythonScriptExecutor;

import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PythonScriptExecutorImpl implements PythonScriptExecutor {

    private final KyxConfiguration kyxConfiguration;
    private final ObjectMapper objectMapper;
    private PythonInterpreter interpreter;

    @Autowired
    public PythonScriptExecutorImpl(KyxConfiguration kyxConfiguration, ObjectMapper objectMapper) {
        this.kyxConfiguration = kyxConfiguration;
        this.objectMapper = objectMapper;
        initPythonInterpreter();
    }

    private void initPythonInterpreter() {
        try {
            this.interpreter = new PythonInterpreter();
            File scriptFile = new File(kyxConfiguration.getPython().getScriptPath());
            if (scriptFile.exists()) {
                interpreter.execfile(scriptFile.getAbsolutePath());
                log.info("Python script loaded successfully from: {}", scriptFile.getAbsolutePath());
            } else {
                log.error("Python script not found at: {}", scriptFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error initializing Python interpreter", e);
        }
    }

    @Override
    public Map<String, Object> executeFunction(String functionName, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (interpreter == null) {
                initPythonInterpreter();
            }
            
            if (interpreter == null) {
                log.error("Python interpreter is not initialized");
                result.put("inRisk", false);
                result.put("riskDetail", "Error: Python interpreter not initialized");
                return result;
            }
            
            PyDictionary pyParams = new PyDictionary();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                pyParams.__setitem__(new PyString(entry.getKey()), objectMapper.convertValue(entry.getValue(), PyObject.class));
            }
            
            PyObject pyFunction = interpreter.get(functionName);
            if (pyFunction == null) {
                log.error("Python function '{}' not found", functionName);
                result.put("inRisk", false);
                result.put("riskDetail", "Error: Python function not found");
                return result;
            }
            
            PyObject pyResult = pyFunction.__call__(pyParams);
            result = objectMapper.convertValue(pyResult, Map.class);
            
        } catch (Exception e) {
            log.error("Error executing Python function", e);
            result.put("inRisk", false);
            result.put("riskDetail", "Error: " + e.getMessage());
        }
        
        return result;
    }
} 