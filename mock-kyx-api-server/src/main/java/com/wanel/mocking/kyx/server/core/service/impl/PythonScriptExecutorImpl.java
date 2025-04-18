package com.wanel.mocking.kyx.server.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanel.mocking.kyx.server.app.config.KyxConfiguration;
import com.wanel.mocking.kyx.server.core.service.PythonScriptExecutor;

import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.Py;
import org.python.util.PythonInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
            // These system properties MUST be set before initializing PythonInterpreter
            // Setting python.import.site=false prevents the error with site module
            System.setProperty("python.import.site", "false");
            System.setProperty("python.cachedir.skip", "true");
            System.setProperty("python.console.encoding", "UTF-8");
            
            // Add script directory to python.path to ensure it can be found
            File scriptFile = new File(kyxConfiguration.getPython().getScriptPath());
            String scriptDir = scriptFile.getParent() != null ? scriptFile.getParent() : ".";
            
            // Use the script directory and current directory in python.path
            String pythonPath = scriptDir + File.pathSeparator + System.getProperty("user.dir");
            System.setProperty("python.path", pythonPath);
            log.info("Setting python.path to: {}", pythonPath);
            
            // Set Jython registry properties - these are critical for proper initialization
            Properties props = new Properties();
            props.setProperty("python.import.site", "false");
            props.setProperty("python.cachedir.skip", "true");
            props.setProperty("python.path", pythonPath);
            props.setProperty("python.options.includeJavaStackInExceptions", "false");
            props.setProperty("python.options.showJavaExceptions", "true");
            
            // Initialize the PythonInterpreter with our configured properties
            // This MUST happen before creating the interpreter instance
            PythonInterpreter.initialize(System.getProperties(), props, new String[] {"-S"});
            
            // Now create the interpreter with our prepared environment
            this.interpreter = new PythonInterpreter();
            
            // Set pre-execution options to ensure scripting environment is properly configured
            interpreter.exec("import sys");
            interpreter.exec("sys.dont_write_bytecode = True");
            
            // Show the Python system path for debugging
            interpreter.exec("print('Python sys.path:', sys.path)");
            
            // Load the script
            if (scriptFile.exists()) {
                log.info("Loading Python script from: {}", scriptFile.getAbsolutePath());
                interpreter.execfile(scriptFile.getAbsolutePath());
                log.info("Python script loaded successfully");
                
                // Verify the function exists
                PyObject pyFunction = interpreter.get(kyxConfiguration.getPython().getFunctionName());
                if (pyFunction != null) {
                    log.info("Python function '{}' found and ready to use", kyxConfiguration.getPython().getFunctionName());
                } else {
                    log.error("Python function '{}' not found in script", kyxConfiguration.getPython().getFunctionName());
                }
            } else {
                log.error("Python script not found at: {}", scriptFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error initializing Python interpreter: {}", e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> executeFunction(String functionName, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
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
            log.info("Risk check result using Python: {}", result);
            
        } catch (Exception e) {
            log.error("Error executing Python function: {}", e.getMessage(), e);
            result.put("inRisk", false);
            result.put("riskDetail", "Error: " + e.getMessage());
        }
        
        return result;
    }
} 