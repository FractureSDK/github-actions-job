package dev.vospek.leviathan.config;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import dev.vospek.leviathan.config.annotations.Experimental;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

public abstract class ConfigModule extends LeviathanConfig {

    private static final Set<ConfigModule> LOADED_MODULES = new HashSet<>();

    protected final LeviathanGlobalConfig globalConfig;

    public ConfigModule() {
        this.globalConfig = LeviathanConfig.globalConfig();
    }

    public static void initModules() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<Field> enabledExperimentalModules = new ArrayList<>();
        List<Field> deprecatedModules = new ArrayList<>();

        Class<?>[] classes = LeviathanConfig.getClasses(LeviathanConfig.CONFIG_MODULE_PACKAGE).toArray(new Class[0]);
        ObjectArrays.quickSort(classes, Comparator.comparing(Class::getSimpleName));
        for (Class<?> clazz : classes) {
            ConfigModule module = (ConfigModule) clazz.getConstructor().newInstance();
            module.onLoaded();

            LOADED_MODULES.add(module);
            for (Field field : getAnnotatedStaticFields(clazz, Experimental.class)) {
                if (!(field.get(null) instanceof Boolean enabled)) continue;
                if (enabled) {
                    enabledExperimentalModules.add(field);
                }
            }
            for (Field field : getAnnotatedStaticFields(clazz, Deprecated.class)) {
                if (!(field.get(null) instanceof Boolean enabled)) continue;
                if (enabled) {
                    deprecatedModules.add(field);
                }
            }
        }

        if (!enabledExperimentalModules.isEmpty()) {
            LeviathanConfig.LOGGER.warn("You have following experimental module(s) enabled: {}, please proceed with caution!", formatModules(enabledExperimentalModules));
        }

        if (!deprecatedModules.isEmpty()) {
            LeviathanConfig.LOGGER.warn("The following enabled module(s) has been deprecated: {}, please proceed with caution!", formatModules(deprecatedModules));
        }
    }

    private static List<String> formatModules(List<Field> modules) {
        return modules.stream().map(f -> f.getDeclaringClass().getSimpleName() + "." + f.getName()).toList();
    }

    public static void loadAfterBootstrap() {
        for (ConfigModule module : LOADED_MODULES) {
            module.onPostLoaded();
        }

        // Save config to disk
        try {
            LeviathanConfig.globalConfig().saveConfig();
        } catch (Exception e) {
            LeviathanConfig.LOGGER.error("Failed to save config file!", e);
        }

        // Initialize Observability system after config validation
        dev.vospek.leviathan.observability.ObservabilityBootstrap.initialize();
    }

    private static List<Field> getAnnotatedStaticFields(Class<?> clazz, Class<? extends Annotation> annotation) {
        List<Field> fields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(annotation) && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                fields.add(field);
            }
        }

        return fields;
    }

    public static void clearModules() {
        LOADED_MODULES.clear();
    }

    public abstract void onLoaded();

    public void onPostLoaded() {
    }
}
