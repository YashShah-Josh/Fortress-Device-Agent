package com.fortress.deviceagent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class DeviceSerialCache {
    private DeviceSerialCache() {
    }

    public static Path cachePath() {
        return AgentConfig.fortressHome().resolve(AgentConfig.SERIAL_CACHE_FILE);
    }

    public static Optional<String> read() throws IOException {
        Path path = cachePath();
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        String value = Files.readString(path, StandardCharsets.UTF_8).trim();
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    public static void write(String serial) throws IOException {
        Path home = AgentConfig.fortressHome();
        Files.createDirectories(home);
        Files.writeString(cachePath(), serial, StandardCharsets.UTF_8);
    }
}
