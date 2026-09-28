
package com.ambulance.dispatch.service;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordService {

    private static final int ITERATIONS = 210000;
    private static final int KEY_LENGTH = 256;

    public String hashPassword(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        byte[] hash = generateHash(password, salt);

        return ITERATIONS + ":" +
                Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(hash);
    }

    public boolean verifyPassword(
            String password,
            String storedHash) {

        try {
            String[] parts = storedHash.split(":");

            if (parts.length != 3) {
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);

            if (iterations < 100000 || iterations > 1000000) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);

            byte[] actual = generateHash(
                    password, salt, iterations
            );

            return MessageDigest.isEqual(expected, actual);

        } catch (Exception ex) {
            return false;
        }
    }

    private byte[] generateHash(
            String password,
            byte[] salt) {

        return generateHash(password, salt, ITERATIONS);
    }

    private byte[] generateHash(
            String password,
            byte[] salt,
            int iterations) {

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                KEY_LENGTH
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            return factory.generateSecret(spec).getEncoded();

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Password hashing failed", ex
            );

        } finally {
            spec.clearPassword();
        }
    }
}