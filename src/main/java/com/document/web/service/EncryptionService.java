package com.document.web.service;

import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Service
public class EncryptionService {
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 128; // Change to 128 instead of 256

    public SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE); // Use 128-bit key size
        return keyGen.generateKey();
    }

    public String encryptToBase64(byte[] content, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM + "/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(content);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public byte[] decryptFromBase64(String encryptedContent, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM + "/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedContent);
        return cipher.doFinal(encryptedBytes);
    }

    public String keyToString(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public SecretKey stringToKey(String keyStr) {
        byte[] decodedKey = Base64.getDecoder().decode(keyStr);
        return new SecretKeySpec(decodedKey, ALGORITHM);
    }
}