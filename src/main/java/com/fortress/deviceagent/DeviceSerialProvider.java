package com.fortress.deviceagent;

import oshi.SystemInfo;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

public class DeviceSerialProvider {
    private final SystemInfo systemInfo = new SystemInfo();

    public String resolveSerial() throws IOException {
        String live = readLiveSerial().orElse(null);
        if (isUsable(live)) {
            DeviceSerialCache.write(live);
            return live;
        }

        String cached = DeviceSerialCache.read().orElse(null);
        if (isUsable(cached)) {
            return cached;
        }

        throw new IOException("Unable to resolve hardware serial number on this machine.");
    }

    private Optional<String> readLiveSerial() {
        HardwareAbstractionLayer hal = systemInfo.getHardware();
        ComputerSystem computer = hal.getComputerSystem();

        String serial = normalize(computer.getSerialNumber());
        if (isUsable(serial)) {
            return Optional.of(serial);
        }

        String board = normalize(computer.getBaseboard().getSerialNumber());
        if (isUsable(board)) {
            return Optional.of(board);
        }

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("linux")) {
            return readLinuxMachineId();
        }
        if (os.contains("mac")) {
            return Optional.ofNullable(normalize(computer.getHardwareUUID())).filter(this::isUsable);
        }
        return Optional.empty();
    }

    private Optional<String> readLinuxMachineId() {
        Path machineId = Path.of("/etc/machine-id");
        if (!Files.isRegularFile(machineId)) {
            return Optional.empty();
        }
        try {
            String raw = Files.readString(machineId, StandardCharsets.UTF_8).trim();
            if (raw.isEmpty()) {
                return Optional.empty();
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Optional.of("MID-" + HexFormat.of().formatHex(hash).substring(0, 24).toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.contains("to be filled") || lower.equals("unknown")
                || lower.equals("not specified") || lower.equals("default string")
                || lower.equals("system serial number") || lower.equals("none")) {
            return null;
        }
        return trimmed;
    }

    private boolean isUsable(String serial) {
        return serial != null && !serial.isBlank();
    }
}
