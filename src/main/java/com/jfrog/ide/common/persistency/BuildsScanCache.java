package com.jfrog.ide.common.persistency;

import org.apache.commons.io.IOUtils;
import org.jfrog.build.api.util.Log;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author yahavi
 **/
public class BuildsScanCache {

    public enum Type {
        BUILD_INFO,
        XRAY_BUILD_SCAN;
    }

    public static final int MAX_BUILDS = 100;
    public static final int MAX_FILES = MAX_BUILDS * 2;
    private String[] currentBuildScanCaches = new String[]{};
    private final Path buildsDir;
    private final Log logger;

    public BuildsScanCache(String projectName, Path basePath, Log logger) throws IOException {
        this.buildsDir = basePath.resolve(Base64.getEncoder().encodeToString(projectName.getBytes(StandardCharsets.UTF_8))).resolve(projectName);
        this.logger = logger;
        if (!Files.exists(buildsDir)) {
            Files.createDirectories(buildsDir);
            return;
        }
        this.currentBuildScanCaches = Arrays.stream(buildsDir.toFile().listFiles())
                .map(File::getName)
                .sorted()
                .toArray(String[]::new);
        cleanUpOldBuilds();
    }

    private void cleanUpOldBuilds() throws IOException {
        for (int i = MAX_FILES; i < currentBuildScanCaches.length; i++) {
            Files.delete(Paths.get(currentBuildScanCaches[i]));
        }
    }

    public void save(byte[] content, String buildName, String buildNumber, Type type) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(getBuildFile(buildName, buildNumber, type));
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            ZipEntry entry = new ZipEntry(type.toString());
            zos.putNextEntry(entry);
            IOUtils.write(content, zos);
        }
    }

    public byte[] load(String buildName, String buildNumber, Type type) throws IOException {
        File buildFile = getBuildFile(buildName, buildNumber, type);
        if (!buildFile.exists()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(getBuildFile(buildName, buildNumber, type));
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zis = new ZipInputStream(bis)) {
            zis.getNextEntry();
            return IOUtils.toByteArray(zis);
        }
    }

    private File getBuildFile(String buildName, String buildNumber, Type type) {
        String buildIdentifier = String.format("%s_%s", buildName, buildNumber);
        String fileName = type.toString() + Base64.getEncoder().encodeToString(buildIdentifier.getBytes(StandardCharsets.UTF_8)) + ".zip";
        return buildsDir.resolve(fileName).toFile();
    }

}
