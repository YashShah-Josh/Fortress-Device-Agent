package com.fortress.deviceagent;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.concurrent.Executors;

public class DeviceHttpServer {
    private final int port;
    private final DeviceSerialProvider serialProvider;
    private final KeyStoreManager keyStoreManager;
    private final DeviceSigner signer;
    private HttpServer server;

    public DeviceHttpServer(int port) {
        this.port = port;
        this.serialProvider = new DeviceSerialProvider();
        this.keyStoreManager = new KeyStoreManager();
        this.signer = new DeviceSigner();
    }

    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/" + AgentConfig.DEVICE_ID_PATH, new DeviceIdHandler());
        server.createContext("/health", exchange -> writeJson(exchange, 200, "{\"status\":\"ok\"}"));
        server.setExecutor(Executors.newFixedThreadPool(2));
        server.start();
        System.out.println("Fortress Device Agent listening on http://127.0.0.1:" + port + "/" + AgentConfig.DEVICE_ID_PATH);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private final class DeviceIdHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange.getResponseHeaders());
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            try {
                String serial = serialProvider.resolveSerial();
                long timestamp = System.currentTimeMillis();
                KeyPair keyPair = keyStoreManager.getKeyPair();
                String signature = signer.sign(serial, timestamp, keyPair.getPrivate());
                String publicKeyPem = keyStoreManager.getPublicKeyPem();
                String body = String.format(
                        "{\"deviceSerial\":\"%s\",\"timestamp\":%d,\"signature\":\"%s\",\"publicKey\":\"%s\"}",
                        jsonEscape(serial),
                        timestamp,
                        jsonEscape(signature),
                        jsonEscape(publicKeyPem)
                );
                writeJson(exchange, 200, body);
            } catch (Exception ex) {
                writeJson(exchange, 503, "{\"error\":\"" + jsonEscape(ex.getMessage()) + "\"}");
            }
        }
    }

    private static void addCors(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        headers.set("Cache-Control", "no-store");
    }

    private static void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        addCors(exchange.getResponseHeaders());
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

}
