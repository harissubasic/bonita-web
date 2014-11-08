/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.console.common.server.preferences.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bonitasoft.console.common.server.preferences.constants.WebBonitaConstantsUtils;

/**
 * @author Yang zhiheng
 *
 */
public class ConsoleProperties {

    /**
     * Default name of the preferences file
     */
    public static final String PROPERTIES_FILENAME = "console-config.properties";

    /**
     * Document max size
     */
    public static final String ATTACHMENT_MAX_SIZE = "form.attachment.max.size";

    /**
     * Custom page debug mode
     */
    public static final String CUSTOM_PAGE_DEBUG = "custom.page.debug";

    /**
     * Instances attribute
     */
    private static Map<Long, ConsoleProperties> INSTANCES = new ConcurrentHashMap<Long, ConsoleProperties>();

    /**
     * Logger
     */
    private static final Logger LOGGER = Logger.getLogger(ConsoleProperties.class.getName());

    /**
     * The loaded properties
     */
    protected Properties properties = new Properties();

    /**
     * The properties file
     */
    protected File propertiesFile;

    /**
     * @return the {@link SecurityProperties} instance
     */
    protected static ConsoleProperties getInstance(final long tenantId) {
        ConsoleProperties tenancyProperties = INSTANCES.get(tenantId);
        if (tenancyProperties == null) {
            tenancyProperties = new ConsoleProperties(new File(WebBonitaConstantsUtils.getInstance(tenantId).getConfFolder(), PROPERTIES_FILENAME));
            INSTANCES.put(tenantId, tenancyProperties);
        }
        return tenancyProperties;
    }

    ConsoleProperties(final File propertiesFile) {
        // Read properties file.
        this.propertiesFile = propertiesFile;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(this.propertiesFile);
            properties.load(inputStream);
        } catch (final IOException e) {
            logSevere(e, "Bonita console properties file " + this.propertiesFile.getPath() + " could not be loaded.");
        } finally {
            closeInputStream(inputStream);
        }
    }

    public String getProperty(final String propertyName) {
        if (properties == null) {
            return null;
        }
        return properties.getProperty(propertyName);
    }

    public String getProperty(final String propertyName, final String defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        return properties.getProperty(propertyName, defaultValue);
    }

    public void removeProperty(final String propertyName) throws IOException {
        if (properties != null) {
            properties.remove(propertyName);
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(propertiesFile);
                properties.store(outputStream, null);
            } catch (final IOException e) {
                logSevere(e, "Bonita console properties file " + propertiesFile.getPath() + " could not be loaded.");
            } finally {
                closeOuptutStream(outputStream);
            }
        }
    }

    public void setProperty(final String propertyName, final String propertyValue) throws IOException {
        if (properties != null) {
            properties.setProperty(propertyName, propertyValue);
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(propertiesFile);
                properties.store(outputStream, null);
            } catch (final IOException e) {
                logSevere(e, "Bonita console properties file " + propertiesFile.getPath() + " could not be loaded.");
            } finally {
                closeOuptutStream(outputStream);
            }
        }
    }

    public long getMaxSize() {
        final String maxSize = this.getProperty(ATTACHMENT_MAX_SIZE);
        if (maxSize != null) {
            return Long.valueOf(maxSize);
        }
        return 15;
    }

    public boolean isPageInDebugMode() {
        final String debugMode = this.getProperty(CUSTOM_PAGE_DEBUG);
        return Boolean.parseBoolean(debugMode);
    }

    private void closeInputStream(final InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (final IOException e) {
                logSevere(e, "Bonita console properties file stream " + propertiesFile.getPath() + " could not be closed.");
            }
        }
    }

    private void closeOuptutStream(final OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (final IOException e) {
                logSevere(e, "Bonita console properties file stream " + propertiesFile.getPath() + " could not be closed.");
            }
        }
    }

    private void logSevere(final IOException e, final String message) {
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.log(Level.SEVERE, message, e);
        }
    }
}
