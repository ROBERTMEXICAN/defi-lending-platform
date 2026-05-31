package com.example.defilending.auth;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory nonce storage.
 * Each wallet address gets a fresh nonce per login attempt.
 * After successful verification the nonce is removed (one-time use).
 */
@Component
public class NonceStore {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    public String generateNonce(String address) {
        String nonce = UUID.randomUUID().toString();
        store.put(address.toLowerCase(), nonce);
        return nonce;
    }

    public String getNonce(String address) {
        return store.get(address.toLowerCase());
    }

    public void removeNonce(String address) {
        store.remove(address.toLowerCase());
    }
}
