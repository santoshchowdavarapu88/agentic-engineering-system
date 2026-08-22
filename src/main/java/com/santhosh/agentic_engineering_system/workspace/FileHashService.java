package com.santhosh.agentic_engineering_system.workspace;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FileHashService {

    public Map<String, String> manifest(Path root) {
        Map<String, String> result = new LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> !Files.isSymbolicLink(path))
                    .sorted()
                    .forEach(path -> result.put(
                            relative(root, path),
                            sha256(path)
                    ));
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to create repository hash manifest",
                    exception
            );
        }
        return Map.copyOf(result);
    }

    public String sha256(Path file) {
        try (InputStream input = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new WorkspaceException("Unable to hash file " + file, exception);
        }
    }

    private String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }
}
