package com.wanel.mocking.kyx.server.core.service;

import java.util.Map;

/**
 * Service interface for executing Python scripts
 */
public interface PythonScriptExecutor {

    /**
     * Execute a Python function with parameters and get the result
     * 
     * @param functionName The name of the Python function to execute
     * @param params The parameters to pass to the function
     * @return The result of the Python function execution
     */
    Map<String, Object> executeFunction(String functionName, Map<String, Object> params);
} 