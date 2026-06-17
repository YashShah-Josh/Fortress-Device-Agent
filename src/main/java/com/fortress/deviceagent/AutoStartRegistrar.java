package com.fortress.deviceagent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Ensures the packaged agent is registered to start automatically after login/reboot.
 * DMG installers cannot run post-install scripts, so this runs on every agent startup.
 */
public final class AutoStartRegistrar {
    private static final String LAUNCH_AGENT_LABEL = "com.fortress.device-agent";
    private static final String WINDOWS_RUN_KEY_NAME = "FortressDeviceAgent";
    private static final Path LINUX_SYSTEMD_UNIT =
            Paths.get("/etc/systemd/system/fortress-device-agent.service");

    private AutoStartRegistrar() {
    }

    public static void ensureRegistered() {
        Optional<String> executable = packagedExecutable();
        if (executable.isEmpty()) {
            return;
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac")) {
                registerMac(executable.get());
            } else if (os.contains("win")) {
                registerWindows(executable.get());
            } else if (os.contains("linux")) {
                registerLinux(executable.get());
            }
        } catch (Exception ex) {
            System.err.println("Auto-start registration skipped: " + ex.getMessage());
        }
    }

    private static Optional<String> packagedExecutable() {
        return ProcessHandle.current()
                .info()
                .command()
                .filter(AutoStartRegistrar::isPackagedExecutable);
    }

    private static boolean isPackagedExecutable(String command) {
        String normalized = command.toLowerCase();
        if (normalized.contains("java")) {
            return false;
        }
        return normalized.contains("fortressdeviceagent")
                || normalized.contains("fortress-device-agent");
    }

    private static void registerMac(String executable) throws IOException, InterruptedException {
        Path plist = Paths.get(
                System.getProperty("user.home"),
                "Library",
                "LaunchAgents",
                LAUNCH_AGENT_LABEL + ".plist"
        );
        String plistBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0">
                <dict>
                    <key>Label</key>
                    <string>%s</string>
                    <key>ProgramArguments</key>
                    <array>
                        <string>%s</string>
                    </array>
                    <key>RunAtLoad</key>
                    <true/>
                    <key>KeepAlive</key>
                    <true/>
                    <key>StandardOutPath</key>
                    <string>/tmp/fortress-device-agent.log</string>
                    <key>StandardErrorPath</key>
                    <string>/tmp/fortress-device-agent.err</string>
                </dict>
                </plist>
                """.formatted(LAUNCH_AGENT_LABEL, escapeXml(executable));

        Files.createDirectories(plist.getParent());
        boolean plistChanged = !Files.exists(plist)
                || !plistBody.equals(Files.readString(plist, StandardCharsets.UTF_8));
        if (plistChanged) {
            Files.writeString(plist, plistBody, StandardCharsets.UTF_8);
        }

        String guiDomain = "gui/" + currentUserId();
        String launchTarget = guiDomain + "/" + LAUNCH_AGENT_LABEL;
        boolean loaded = isLaunchAgentLoaded(launchTarget);

        if (loaded && !plistChanged) {
            return;
        }
        if (!loaded && !startedByLaunchd()) {
            // Manual launch: persist plist for the next login without starting a duplicate now.
            return;
        }
        if (loaded) {
            runQuiet("launchctl", "bootout", launchTarget);
        }
        runChecked("launchctl", "bootstrap", guiDomain, plist.toString());
    }

    private static void registerWindows(String executable) throws Exception {
        String value = "\"" + executable + "\"";
        Preferences runKey = Preferences.userRoot()
                .node("Software\\Microsoft\\Windows\\CurrentVersion\\Run");
        String existing = runKey.get(WINDOWS_RUN_KEY_NAME, "");
        if (!value.equals(existing)) {
            runKey.put(WINDOWS_RUN_KEY_NAME, value);
            runKey.flush();
        }
    }

    private static void registerLinux(String executable) throws IOException, InterruptedException {
        if (Files.exists(LINUX_SYSTEMD_UNIT)) {
            runQuiet("systemctl", "enable", "fortress-device-agent.service");
            return;
        }

        Path autostart = Paths.get(
                System.getProperty("user.home"),
                ".config",
                "autostart",
                "fortress-device-agent.desktop"
        );
        String desktopEntry = """
                [Desktop Entry]
                Type=Application
                Name=Fortress Device Agent
                Exec=%s
                X-GNOME-Autostart-enabled=true
                """.formatted(executable);

        Files.createDirectories(autostart.getParent());
        if (!Files.exists(autostart)
                || !desktopEntry.equals(Files.readString(autostart, StandardCharsets.UTF_8))) {
            Files.writeString(autostart, desktopEntry, StandardCharsets.UTF_8);
        }
    }

    private static boolean isLaunchAgentLoaded(String launchTarget) {
        try {
            runChecked("launchctl", "print", launchTarget);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean startedByLaunchd() {
        return ProcessHandle.current()
                .parent()
                .flatMap(handle -> handle.info().command())
                .map(command -> command.toLowerCase().contains("launchd"))
                .orElse(false);
    }

    private static String currentUserId() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("id", "-u").start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        int exitCode = process.waitFor();
        if (exitCode != 0 || output.isEmpty()) {
            throw new IOException("Unable to resolve user id for launchctl bootstrap");
        }
        return output;
    }

    private static void runChecked(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            throw new IOException(
                    "Command failed (" + exitCode + "): "
                            + String.join(" ", command)
                            + (error.isEmpty() ? "" : " — " + error)
            );
        }
    }

    private static void runQuiet(String... command) {
        try {
            new ProcessBuilder(command).start().waitFor();
        } catch (Exception ignored) {
            // Best-effort cleanup before re-registering.
        }
    }

    private static String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
