package com.zzhalex233.guibrowser.client.persistence;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JsonPersistence {

    private static final Logger LOGGER = Logger.getLogger(JsonPersistence.class.getName());

    private JsonPersistence() {
    }

    public static File getDataDir(File gameDir) {
        return new File(gameDir, "guibrowser");
    }

    public static File getWorldDataDir(File gameDir, String worldId) {
        return new File(new File(gameDir, "guibrowser"), worldId);
    }

    public static void saveJson(File file, JsonElement data) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8))) {
            writer.write(data.toString());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write " + file, e);
            return;
        }
        if (file.exists()) {
            file.delete();
        }
        if (!tmp.renameTo(file)) {
            LOGGER.warning("Failed to rename " + tmp + " to " + file);
        }
    }

    public static JsonElement loadJson(File file) {
        if (!file.exists()) {
            return JsonNull.INSTANCE;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return new JsonParser().parse(sb.toString());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read " + file, e);
            return JsonNull.INSTANCE;
        }
    }
}
