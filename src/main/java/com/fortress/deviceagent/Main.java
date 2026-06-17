package com.fortress.deviceagent;

public final class Main {
    public static void main(String[] args) throws Exception {
        int port = AgentConfig.DEFAULT_PORT;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
            }
            if ("--help".equals(args[i])) {
                printHelp();
                return;
            }
        }

        AutoStartRegistrar.ensureRegistered();

        DeviceHttpServer httpServer = new DeviceHttpServer(port);
        httpServer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(httpServer::stop));
        Thread.currentThread().join();
    }

    private static void printHelp() {
        System.out.println("Fortress Device Agent");
        System.out.println("  --port <number>   HTTP port (default 4567)");
        System.out.println("  --help            Show this message");
    }
}
