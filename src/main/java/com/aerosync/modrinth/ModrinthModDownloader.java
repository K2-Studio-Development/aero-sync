package com.aerosync.modrinth;

import com.aerosync.AeroSyncMod;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class ModrinthModDownloader {
    private static final String API_BASE_URL = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "AeroSync/1.0.0 (Minecraft Fabric mod)";
    private static final Gson GSON = new Gson();

    private ModrinthModDownloader() {
    }

    public record DownloadResult(int installed, int skipped, List<String> missing) {
    }

    public static DownloadResult installFromManifest(String manifestJson, Path gameDir) throws IOException, InterruptedException {
        List<ModrinthModManifest.ModFile> mods = ModrinthModManifest.parse(manifestJson);
        Path modsDir = gameDir.resolve("mods");
        Files.createDirectories(modsDir);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        int installed = 0;
        int skipped = 0;
        List<String> missing = new ArrayList<>();

        for (ModrinthModManifest.ModFile mod : mods) {
            Path targetPath = modsDir.resolve(safeFilename(mod.filename()));
            if (Files.exists(targetPath) && mod.sha1().equalsIgnoreCase(ModrinthModManifest.sha1(targetPath))) {
                skipped++;
                continue;
            }

            JsonObject version = findVersionByHash(client, mod.sha1());
            if (version == null) {
                missing.add(mod.filename());
                AeroSyncMod.LOGGER.warn("Modrinth did not find mod by SHA-1: {}", mod.filename());
                continue;
            }

            JsonObject file = selectFile(version, mod.sha1());
            if (file == null || !file.has("url")) {
                missing.add(mod.filename());
                AeroSyncMod.LOGGER.warn("Modrinth version has no downloadable file for: {}", mod.filename());
                continue;
            }

            downloadAndVerify(client, file.get("url").getAsString(), targetPath, mod.sha1());
            installed++;
            AeroSyncMod.LOGGER.info("Installed mod from Modrinth: {}", targetPath.getFileName());
        }

        return new DownloadResult(installed, skipped, missing);
    }

    private static JsonObject findVersionByHash(HttpClient client, String sha1) throws IOException, InterruptedException {
        String encodedHash = URLEncoder.encode(sha1, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE_URL + "/version_file/" + encodedHash + "?algorithm=sha1"))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Modrinth API returned HTTP " + response.statusCode());
        }

        return GSON.fromJson(response.body(), JsonObject.class);
    }

    private static JsonObject selectFile(JsonObject version, String sha1) {
        if (!version.has("files") || !version.get("files").isJsonArray()) {
            return null;
        }

        JsonArray files = version.getAsJsonArray("files");
        JsonObject primary = null;
        JsonObject first = null;
        for (var element : files) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject file = element.getAsJsonObject();
            if (first == null) {
                first = file;
            }
            if (file.has("primary") && file.get("primary").getAsBoolean()) {
                primary = file;
            }
            if (file.has("hashes")) {
                JsonObject hashes = file.getAsJsonObject("hashes");
                if (hashes.has("sha1") && sha1.equalsIgnoreCase(hashes.get("sha1").getAsString())) {
                    return file;
                }
            }
        }

        return primary != null ? primary : first;
    }

    private static void downloadAndVerify(HttpClient client, String url, Path targetPath, String expectedSha1) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Modrinth CDN returned HTTP " + response.statusCode());
        }

        Path tempFile = Files.createTempFile("aerosync_modrinth_", ".jar");
        try (InputStream input = response.body()) {
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        String actualSha1 = ModrinthModManifest.sha1(tempFile);
        if (!expectedSha1.equalsIgnoreCase(actualSha1)) {
            Files.deleteIfExists(tempFile);
            throw new IOException("Downloaded mod hash mismatch for " + targetPath.getFileName());
        }

        Files.createDirectories(targetPath.getParent());
        Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String safeFilename(String filename) {
        return Path.of(filename).getFileName().toString();
    }
}
