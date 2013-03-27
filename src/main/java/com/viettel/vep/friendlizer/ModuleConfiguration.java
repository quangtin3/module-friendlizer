/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.viettel.vep.friendlizer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Module configuration
 *
 * @author quanghx2@viettel.com.vn
 */
public class ModuleConfiguration {

    private static final String CONFIGURATION_PROPERTIES_FILE = "/Configuration.properties";
    private List<Module> modules;

    public ModuleConfiguration() throws IOException {
        Properties properties = loadProperties();
        modules = parseConfiguration(properties);
    }

    public Module[] getModules() {
        return modules.toArray(new Module[modules.size()]);
    }

    /**
     * Parsing properties in to Module configuration object
     *
     * @param properties properties to parse
     * @return list of Module configuration
     */
    private List<Module> parseConfiguration(Properties properties) {
        List<String> moduleKeys = new ArrayList<String>();

        for (Object objectKey : properties.keySet()) {
            if (objectKey instanceof String) {
                String key = (String) objectKey;
                if (key.startsWith("module.") && key.endsWith(".key")) {
                    moduleKeys.add(key.substring(0, key.lastIndexOf(".")));
                }
            }
        }

        for (String key : moduleKeys) {
            Module module = new Module();
            module.setKey(properties.getProperty(key + ".key"));
            module.setDescription(properties.getProperty(key + ".description"));
            module.setDependences(properties.getProperty(key + ".dependencies").split(" "));

            modules.add(module);
        }
        return modules;
    }

    /**
     * Loading configuration from a properties file in ClassPath
     *
     * @return loaded properties
     * @throws IOException if file not found in ClassPath or loading error
     */
    private Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        InputStream stream = getClass().getResourceAsStream(CONFIGURATION_PROPERTIES_FILE);

        if (stream == null) {
            throw new IOException("No configuration file from ClassPath: " + CONFIGURATION_PROPERTIES_FILE);
        }
        try {
            properties.load(stream);
            return properties;
        } catch (IOException ex) {
            throw new IOException("Cannot load properties file from ClassPath: " + CONFIGURATION_PROPERTIES_FILE);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Module properties
     */
    public class Module {

        private String key;
        private String description;
        private List<String> dependencies;

        public Module() {
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setDependences(String[] dependencies) {
            this.dependencies = Arrays.asList(dependencies);
        }

        public String[] getDependences() {
            if (dependencies != null) {
                return dependencies.toArray(new String[dependencies.size()]);
            } else {
                return new String[0];
            }
        }
    }
}
