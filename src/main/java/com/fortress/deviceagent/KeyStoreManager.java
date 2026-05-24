package com.fortress.deviceagent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyStoreManager {
    private final Path privateKeyPath;
    private final Path publicKeyPath;
    private KeyPair keyPair;

    public KeyStoreManager() {
        Path home = AgentConfig.fortressHome();
        this.privateKeyPath = home.resolve(AgentConfig.PRIVATE_KEY_FILE);
        this.publicKeyPath = home.resolve(AgentConfig.PUBLIC_KEY_FILE);
    }

    public synchronized KeyPair getKeyPair()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        if (keyPair != null) {
            return keyPair;
        }
        Files.createDirectories(AgentConfig.fortressHome());
        if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
            keyPair = loadKeyPair();
            return keyPair;
        }
        keyPair = generateKeyPair();
        persistKeyPair(keyPair);
        return keyPair;
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private KeyPair loadKeyPair() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String privatePem = Files.readString(privateKeyPath, StandardCharsets.UTF_8);
        String publicPem = Files.readString(publicKeyPath, StandardCharsets.UTF_8);
        PrivateKey privateKey = readPrivateKey(privatePem);
        PublicKey publicKey = readPublicKey(publicPem);
        return new KeyPair(publicKey, privateKey);
    }

    private void persistKeyPair(KeyPair pair) throws IOException {
        Files.writeString(privateKeyPath, toPem("PRIVATE KEY", pair.getPrivate().getEncoded()), StandardCharsets.UTF_8);
        Files.writeString(publicKeyPath, toPem("PUBLIC KEY", pair.getPublic().getEncoded()), StandardCharsets.UTF_8);
    }

    private PrivateKey readPrivateKey(String pem)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] decoded = decodePem(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private PublicKey readPublicKey(String pem)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] decoded = decodePem(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private byte[] decodePem(String pem) throws IOException {
        String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private String toPem(String label, byte[] encoded) {
        String body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n";
    }

    public Path getPublicKeyPath() {
        return publicKeyPath;
    }

    public String getPublicKeyPem()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        getKeyPair();
        return Files.readString(publicKeyPath, StandardCharsets.UTF_8);
    }
}
