package com.wanel.mocking.kyx.server.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanel.mocking.kyx.server.app.config.KyxConfiguration;
import com.wanel.mocking.kyx.server.core.service.PythonScriptExecutor;

import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyFloat;
import org.python.core.PyBoolean;
import org.python.core.Py;
import org.python.util.PythonInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class PythonScriptExecutorImpl implements PythonScriptExecutor {

    private final KyxConfiguration kyxConfiguration;
    private final ObjectMapper objectMapper;
    private PythonInterpreter interpreter;
    private File scriptFile;
    private final AtomicLong lastModifiedTime = new AtomicLong(0);

    @Autowired
    public PythonScriptExecutorImpl(KyxConfiguration kyxConfiguration, ObjectMapper objectMapper) {
        this.kyxConfiguration = kyxConfiguration;
        this.objectMapper = objectMapper;
        initPythonInterpreter();
        loadScript();
    }

    private void initPythonInterpreter() {
        try {
            // These system properties MUST be set before initializing PythonInterpreter
            // Setting python.import.site=false prevents the error with site module
            System.setProperty("python.import.site", "false");
            System.setProperty("python.cachedir.skip", "true");
            System.setProperty("python.console.encoding", "UTF-8");
            
            // Get script file path
            scriptFile = getScriptFile();
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
        } catch (Exception e) {
            log.error("Error initializing Python interpreter: {}", e.getMessage(), e);
        }
    }
    
    private File getScriptFile() {
        // First, try to use the configured script path
        String configuredPath = kyxConfiguration.getPython().getScriptPath();
        File configFile = new File(configuredPath);
        
        if (configFile.exists()) {
            return configFile;
        }
        
        // If not found, try to find it in resources/scripts folder
        try {
            Path resourcePath = Paths.get("src/main/resources/scripts/kyx_script.py");
            if (Files.exists(resourcePath)) {
                return resourcePath.toFile();
            }
            
            // Finally, check if it's in the classpath
            String classpathResource = "/scripts/kyx_script.py";
            Path tempFile = Files.createTempFile("kyx_script", ".py");
            Files.copy(getClass().getResourceAsStream(classpathResource), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return tempFile.toFile();
        } catch (Exception e) {
            log.warn("Could not locate script in resources, falling back to configured path: {}", configuredPath);
            return configFile;
        }
    }
    
    private synchronized void loadScript() {
        try {
            if (scriptFile.exists()) {
                // Update last modified time
                lastModifiedTime.set(scriptFile.lastModified());
                
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
            log.error("Error loading Python script: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check for script file changes every 5 seconds and reload if modified
     */
    @Scheduled(fixedDelay = 5000)
    public void checkForScriptChanges() {
        if (scriptFile != null && scriptFile.exists()) {
            long currentModified = scriptFile.lastModified();
            
            if (currentModified > lastModifiedTime.get()) {
                log.info("Detected changes in Python script file, reloading...");
                loadScript();
                log.info("Python script reloaded successfully");
            }
        }
    }

    /**
     * Convert a Java object to an appropriate PyObject
     */
    private PyObject toPyObject(Object obj) {
        if (obj == null) {
            return Py.None;
        } else if (obj instanceof String) {
            return new PyString((String) obj);
        } else if (obj instanceof Integer) {
            return new PyInteger((Integer) obj);
        } else if (obj instanceof Long) {
            return new PyLong((Long) obj);
        } else if (obj instanceof Double || obj instanceof Float) {
            return new PyFloat(((Number) obj).doubleValue());
        } else if (obj instanceof Boolean) {
            return ((Boolean) obj) ? Py.True : Py.False;
        } else {
            // For complex objects, convert to string
            return new PyString(String.valueOf(obj));
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
                pyParams.__setitem__(new PyString(entry.getKey()), toPyObject(entry.getValue()));
            }
            
            PyObject pyFunction = interpreter.get(functionName);
            if (pyFunction == null) {
                log.error("Python function '{}' not found", functionName);
                result.put("inRisk", false);
                result.put("riskDetail", "Error: Python function not found");
                return result;
            }
            
            PyObject pyResult = pyFunction.__call__(pyParams);
            
            // Convert PyDictionary to Java Map
            if (pyResult instanceof PyDictionary) {
                PyDictionary pyDict = (PyDictionary) pyResult;
                for (Object key : pyDict.keys()) {
                    String keyStr = key.toString();
                    // Convert key to PyString for lookup
                    PyObject value = pyDict.__finditem__(new PyString(keyStr));
                    result.put(keyStr, value.__tojava__(Object.class));
                }
            } else {
                log.error("Python function did not return a dictionary");
                result.put("inRisk", false);
                result.put("riskDetail", "Error: Python function returned unexpected type");
            }
            
            log.info("Risk check result using Python: {}", result);
            
        } catch (Exception e) {
            log.error("Error executing Python function: {}", e.getMessage(), e);
            result.put("inRisk", false);
            result.put("riskDetail", "Error: " + e.getMessage());
        }
        
        return result;
    }
} 