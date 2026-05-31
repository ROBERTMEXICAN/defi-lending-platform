package com.example.defilending.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final NonceStore nonceStore;
    private final Web3SignatureService signatureService;
    private final JwtService jwtService;

    public AuthController(NonceStore nonceStore,
                          Web3SignatureService signatureService,
                          JwtService jwtService) {
        this.nonceStore = nonceStore;
        this.signatureService = signatureService;
        this.jwtService = jwtService;
    }

    /**
     * Step 1: frontend requests a nonce for the given wallet address.
     * GET /api/auth/nonce/{address}
     */
    @GetMapping("/nonce/{address}")
    public ResponseEntity<Map<String, String>> nonce(@PathVariable String address) {
        String nonce = nonceStore.generateNonce(address);
        String message = buildMessage(address, nonce);
        return ResponseEntity.ok(Map.of("nonce", nonce, "message", message));
    }

    /**
     * Step 2: frontend sends the signed message back.
     * POST /api/auth/verify  { "address": "0x...", "signature": "0x..." }
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyRequest req) {
        String nonce = nonceStore.getNonce(req.address());
        if (nonce == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Nonce not found. Request a new nonce first."));
        }

        String message = buildMessage(req.address(), nonce);
        boolean valid = signatureService.verify(message, req.signature(), req.address());

        if (!valid) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid signature"));
        }

        // One-time use — remove nonce after successful verification
        nonceStore.removeNonce(req.address());

        String token = jwtService.generateToken(req.address());
        return ResponseEntity.ok(Map.of("token", token, "address", req.address().toLowerCase()));
    }

    private String buildMessage(String address, String nonce) {
        return "Sign in to DeFi Lending Platform\nAddress: " + address.toLowerCase() + "\nNonce: " + nonce;
    }

    public record VerifyRequest(String address, String signature) {}
}
