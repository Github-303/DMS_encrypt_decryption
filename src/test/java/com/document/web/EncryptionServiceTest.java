package com.document.web;

import com.document.web.service.EncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class EncryptionServiceTest {

    @Autowired
    private EncryptionService encryptionService;

    @Test
    void encryptDecrypt_Success() throws Exception {
        String originalText = "Test content";
        SecretKey key = encryptionService.generateKey();

        String encrypted = encryptionService.encryptToBase64(
                originalText.getBytes(),
                key
        );

        byte[] decrypted = encryptionService.decryptFromBase64(
                encrypted,
                key
        );

        assertEquals(originalText, new String(decrypted));
    }

    @Test
    void generateKey_Correct() throws Exception {
        SecretKey key = encryptionService.generateKey();
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals(128/8, key.getEncoded().length);
    }
}
