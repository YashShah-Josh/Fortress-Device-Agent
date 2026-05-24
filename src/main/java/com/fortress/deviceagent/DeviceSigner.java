package com.fortress.deviceagent;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

public class DeviceSigner {
    public String sign(String deviceSerial, long timestamp, PrivateKey privateKey) throws Exception {
        String payload = canonicalPayload(deviceSerial, timestamp);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    public static String canonicalPayload(String deviceSerial, long timestamp) {
        return deviceSerial + "|" + timestamp;
    }
}
