package com.aerosync.modrinth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class ModrinthModManifest {
    public static final String ENTRY_NAME = "aerosync/modrinth_mods.json";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private ModrinthModManifest() {
    }

    public record ModFile(String filename, String sha1, long size) {
    }

    public static String createJson(Path modsDir) throws IOException {
        JsonObject root = new JsonObject();
        JsonArray mods = new JsonArray();

        if (Files.exists(modsDir) && Files.isDirectory(modsDir)) {
            try (var stream = Files.list(modsDir)) {
                for (Path file : stream.filter(Files::isRegularFile).toList()) {
                    String filename = file.getFileName().toString();
                    if (!filename.toLowerCase().endsWith(".jar") || filename.toLowerCase().contains("aerosync")) {
                        continue;
                    }

                    JsonObject mod = new JsonObject();
                    mod.addProperty("filename", filename);
                    mod.addProperty("sha1", sha1(file));
                    mod.addProperty("size", Files.size(file));
                    mods.add(mod);
                }
            }
        }

        root.addProperty("format", 1);
        root.add("mods", mods);
        return GSON.toJson(root);
    }

    public static List<ModFile> parse(String json) {
        List<ModFile> mods = new ArrayList<>();
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        if (root == null || !root.has("mods") || !root.get("mods").isJsonArray()) {
            return mods;
        }

        for (var element : root.getAsJsonArray("mods")) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject mod = element.getAsJsonObject();
            if (!mod.has("filename") || !mod.has("sha1")) {
                continue;
            }

            String filename = mod.get("filename").getAsString();
            String sha1 = mod.get("sha1").getAsString();
            long size = mod.has("size") ? mod.get("size").getAsLong() : -1L;
            mods.add(new ModFile(filename, sha1, size));
        }
        return mods;
    }

    public static String sha1(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 digest is unavailable", e);
        }

        byte[] buffer = new byte[16384];
        try (InputStream input = Files.newInputStream(file)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        byte[] hash = digest.digest();
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }

    public static byte[] toBytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
