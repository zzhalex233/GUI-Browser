package com.zzhalex233.guibrowser.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BrowserConfigLoader {
    private static final Logger LOGGER = Logger.getLogger(BrowserConfigLoader.class.getName());
    private static final String ESC_ACTION_KEY = "browser.escAction";
    private static final String OPEN_BROWSER_KEY_CODE_KEY = "browser.openBrowserKeyCode";

    private BrowserConfigLoader() {
    }

    public static BrowserConfig load(File file) {
        Objects.requireNonNull(file, "file");

        BrowserConfig loaded = tryLoadWithForge(file);
        if (loaded != null) {
            return loaded;
        }
        return loadWithProperties(file);
    }

    public static void save(File file, BrowserConfig config) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(config, "config");

        if (trySaveWithForge(file, config)) {
            return;
        }
        saveWithProperties(file, config);
    }

    private static BrowserConfig tryLoadWithForge(File file) {
        try {
            Class<?> configurationClass = Class.forName("net.minecraftforge.common.config.Configuration");
            Constructor<?> constructor = configurationClass.getConstructor(File.class);
            Object configuration = constructor.newInstance(file);
            invoke(configurationClass, configuration, "load");

            BrowserConfig defaults = BrowserConfig.defaults();
            String escActionName = invokeString(configurationClass, configuration, "getString",
                new Class<?>[]{String.class, String.class, String.class, String.class},
                new Object[]{"escAction", "browser", defaults.getEscAction().name(), "Browser ESC behavior"});
            int openBrowserKeyCode = invokeInt(configurationClass, configuration, "getInt",
                new Class<?>[]{String.class, String.class, int.class, int.class, int.class, String.class},
                new Object[]{"openBrowserKeyCode", "browser", defaults.getOpenBrowserKeyCode(), 1, Integer.MAX_VALUE, "Key code for opening the browser"});

            if (hasChanged(configurationClass, configuration)) {
                invoke(configurationClass, configuration, "save");
            }
            return buildConfig(escActionName, openBrowserKeyCode, defaults);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Failed to load browser config through Forge APIs from " + file + ". Falling back to properties.", e);
            return null;
        }
    }

    private static boolean trySaveWithForge(File file, BrowserConfig config) {
        try {
            Class<?> configurationClass = Class.forName("net.minecraftforge.common.config.Configuration");
            Constructor<?> constructor = configurationClass.getConstructor(File.class);
            Object configuration = constructor.newInstance(file);
            invoke(configurationClass, configuration, "load");
            invoke(configurationClass, configuration, "getString",
                new Class<?>[]{String.class, String.class, String.class, String.class},
                new Object[]{"escAction", "browser", config.getEscAction().name(), "Browser ESC behavior"});
            invoke(configurationClass, configuration, "getInt",
                new Class<?>[]{String.class, String.class, int.class, int.class, int.class, String.class},
                new Object[]{"openBrowserKeyCode", "browser", config.getOpenBrowserKeyCode(), 1, Integer.MAX_VALUE, "Key code for opening the browser"});
            invokeCategorySet(configurationClass, configuration, "browser", "escAction", config.getEscAction().name());
            invokeCategorySet(configurationClass, configuration, "browser", "openBrowserKeyCode", config.getOpenBrowserKeyCode());
            invoke(configurationClass, configuration, "save");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Failed to save browser config through Forge APIs to " + file + ". Falling back to properties.", e);
            return false;
        }
    }

    private static BrowserConfig loadWithProperties(File file) {
        BrowserConfig defaults = BrowserConfig.defaults();
        if (!file.exists()) {
            return defaults;
        }

        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            properties.load(reader);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read browser config from " + file + ". Falling back to defaults.", e);
            return defaults;
        }

        String escActionName = properties.getProperty(ESC_ACTION_KEY, defaults.getEscAction().name());
        String openBrowserKeyCodeText = properties.getProperty(OPEN_BROWSER_KEY_CODE_KEY, Integer.toString(defaults.getOpenBrowserKeyCode()));
        return buildConfig(escActionName, parseInt(openBrowserKeyCodeText, defaults.getOpenBrowserKeyCode()), defaults);
    }

    private static void saveWithProperties(File file, BrowserConfig config) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }

        Properties properties = new Properties();
        properties.setProperty(ESC_ACTION_KEY, config.getEscAction().name());
        properties.setProperty(OPEN_BROWSER_KEY_CODE_KEY, Integer.toString(config.getOpenBrowserKeyCode()));

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            properties.store(writer, "GUI Browser config");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save browser config to " + file + ".", e);
        }
    }

    private static BrowserConfig buildConfig(String escActionName, int openBrowserKeyCode, BrowserConfig defaults) {
        EscAction escAction;
        try {
            escAction = EscAction.valueOf(escActionName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid escAction '" + escActionName + "' in browser config. Using default " + defaults.getEscAction() + ".");
            escAction = defaults.getEscAction();
        }
        int sanitizedKeyCode = openBrowserKeyCode > 0 ? openBrowserKeyCode : defaults.getOpenBrowserKeyCode();
        if (sanitizedKeyCode != openBrowserKeyCode) {
            LOGGER.warning("Invalid openBrowserKeyCode '" + openBrowserKeyCode + "' in browser config. Using default " + defaults.getOpenBrowserKeyCode() + ".");
        }
        return new BrowserConfig(escAction, sanitizedKeyCode);
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    private static Object invoke(Class<?> type, Object instance, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws ReflectiveOperationException {
        Method method = type.getMethod(methodName, parameterTypes);
        return method.invoke(instance, arguments);
    }

    private static Object invoke(Class<?> type, Object instance, String methodName) throws ReflectiveOperationException {
        Method method = type.getMethod(methodName);
        return method.invoke(instance);
    }

    private static String invokeString(Class<?> type, Object instance, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws ReflectiveOperationException {
        Object result = invoke(type, instance, methodName, parameterTypes, arguments);
        return result == null ? null : result.toString();
    }

    private static int invokeInt(Class<?> type, Object instance, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws ReflectiveOperationException {
        Object result = invoke(type, instance, methodName, parameterTypes, arguments);
        return ((Number) result).intValue();
    }

    private static boolean hasChanged(Class<?> type, Object instance) throws ReflectiveOperationException {
        return (Boolean) invoke(type, instance, "hasChanged");
    }

    private static void invokeCategorySet(Class<?> type, Object instance, String category, String key, Object value) throws ReflectiveOperationException {
        Method getCategory = type.getMethod("getCategory", String.class);
        Object categoryObject = getCategory.invoke(instance, category);
        Method get = categoryObject.getClass().getMethod("get", String.class);
        Object property = get.invoke(categoryObject, key);
        if (value instanceof Integer integer) {
            Method set = property.getClass().getMethod("set", int.class);
            set.invoke(property, integer.intValue());
            return;
        }
        Method set = property.getClass().getMethod("set", String.class);
        set.invoke(property, value.toString());
    }
}
