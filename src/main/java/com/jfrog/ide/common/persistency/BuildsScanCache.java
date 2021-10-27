package com.jfrog.ide.common.persistency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.xray.client.impl.services.details.DetailsResponseImpl;
import com.jfrog.xray.client.services.details.DetailsResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Represents the cache for the builds information.
 * The build infos and the Xray's 'details/build' responses are stored in different ZIP files.
 *
 * @author yahavi
 */
public class BuildsScanCache {
    public enum Type {BUILD_INFO, BUILD_SCAN_RESULTS}

    private static final String INVALID_CACHE_FMT = "Failed reading cache file for '%s/%s', zapping the old cache and starting a new one.";
    public static final int MAX_BUILDS = 100;
    // Each build should have 1 build info file and 1 Xray scan results file
    public static final int MAX_FILES = MAX_BUILDS * 2;

    private final Path buildsDir;
    private final Log log;

    public BuildsScanCache(String projectName, Path basePath, Log log) throws IOException {
        this.buildsDir = basePath.resolve(Base64.getEncoder().encodeToString(projectName.getBytes(StandardCharsets.UTF_8))).resolve(projectName);
        this.log = log;
        if (!Files.exists(buildsDir)) {
            Files.createDirectories(buildsDir);
            return;
        }
        cleanUpOldBuilds();
    }

    /**
     * This builds cache saves only 100 newest builds. This cleanup method deletes builds 101 and older.
     *
     * @throws IOException in case of any IO error.
     */
    private void cleanUpOldBuilds() throws IOException {
        String[] currentBuildScanCaches = Arrays.stream(Objects.requireNonNull(buildsDir.toFile().listFiles()))
                .map(File::getName)
                .sorted()
                .toArray(String[]::new);
        for (int i = MAX_FILES; i < currentBuildScanCaches.length; i++) {
            Path pathToDelete = buildsDir.resolve(currentBuildScanCaches[i]);
            log.debug("Deleting " + pathToDelete);
            Files.delete(buildsDir.resolve(pathToDelete));
        }
    }

    public void createDirectories() throws IOException {
        Files.createDirectories(buildsDir);
    }

    /**
     * Save build info or Xray scan results.
     *
     * @param content     - The content to save
     * @param buildName   - Build name
     * @param buildNumber - Build number
     * @param type        - BUILD_INFO or XRAY_BUILD_SCAN
     * @throws IOException in case of error during writing the cache file.
     */
    public void save(byte[] content, String buildName, String buildNumber, Type type) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(getBuildFile(buildName, buildNumber, type));
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            ZipEntry entry = new ZipEntry(type.toString());
            zos.putNextEntry(entry);
            IOUtils.write(content, zos);
        }
    }

    /**
     * Load build info or Xray scan results from cache.
     *
     * @param buildName   - Build name
     * @param buildNumber - Build number
     * @param type        - BUILD_INFO or XRAY_BUILD_SCAN
     * @return the cache content or null if doesn't exist.
     * @throws IOException in case of error during reading the cache file.
     */
    byte[] load(String buildName, String buildNumber, Type type) throws IOException {
        File buildFile = getBuildFile(buildName, buildNumber, type);
        if (!buildFile.exists()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(buildFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zis = new ZipInputStream(bis)) {
            zis.getNextEntry();
            return IOUtils.toByteArray(zis);
        }
    }

    /**
     * Load the build info cache of the input build.
     *
     * @param mapper      - The object mapper
     * @param buildName   - Build name
     * @param buildNumber - build number
     * @return {@link Build} or null if cache does not exist.
     */
    public Build loadBuildInfo(ObjectMapper mapper, String buildName, String buildNumber) {
        try {
            byte[] buffer = load(buildName, buildNumber, BuildsScanCache.Type.BUILD_INFO);
            if (buffer != null) {
                return mapper.readValue(buffer, Build.class);
            }
        } catch (IOException e) {
            log.warn(String.format(INVALID_CACHE_FMT, buildName, buildNumber) + ": " + ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }

    /**
     * Load the Xray 'details/build' cache of the input build.
     *
     * @param mapper      - The object mapper
     * @param buildName   - Build name
     * @param buildNumber - build number
     * @return {@link DetailsResponse} or null if cache does not exist.
     */
    public DetailsResponse loadScanResults(ObjectMapper mapper, String buildName, String buildNumber) {
        try {
            byte[] buffer = load(buildName, buildNumber, BuildsScanCache.Type.BUILD_SCAN_RESULTS);
            if (buffer != null) {
                return mapper.readValue(buffer, DetailsResponseImpl.class);
            }
        } catch (IOException e) {
            log.warn(String.format(INVALID_CACHE_FMT, buildName, buildNumber) + ": " + ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }

    private File getBuildFile(String buildName, String buildNumber, Type type) {
        String buildIdentifier = String.format("%s_%s", buildName, buildNumber);
        String fileName = type.toString() + "_" + DigestUtils.sha256Hex(buildIdentifier) + ".zip";
        return buildsDir.resolve(fileName).toFile();
    }
}
