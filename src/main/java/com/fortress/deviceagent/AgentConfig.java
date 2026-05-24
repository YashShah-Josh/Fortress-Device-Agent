package com.fortress.deviceagent;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class AgentConfig {
    public static final int DEFAULT_PORT = 4567;
    public static final String DEVICE_ID_PATH = "device-id";
    public static final String FORTRESS_DIR = ".fortress";
    public static final String SERIAL_CACHE_FILE = "device_serial";
    public static final String PRIVATE_KEY_FILE = "device_agent_private.pem";
    public static final String PUBLIC_KEY_FILE = "device_agent_public.pem";

    private AgentConfig() {
    }

    public static Path fortressHome() {
        String home = System.getProperty("user.home");
        return Paths.get(home, FORTRESS_DIR);
    }
}
