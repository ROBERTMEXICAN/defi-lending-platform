package com.example.defilending.auth;

import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;

@Service
public class Web3SignatureService {

    /**
     * Verifies an Ethereum personal_sign signature.
     * MetaMask prefixes the message: "\x19Ethereum Signed Message:\n" + message.length + message
     *
     * @param message   the plain text message that was signed
     * @param signature the 0x-prefixed hex signature from MetaMask
     * @param address   the wallet address that claims to have signed
     * @return true if the recovered address matches
     */
    public boolean verify(String message, String signature, String address) {
        try {
            byte[] msgBytes = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] prefixedHash = org.web3j.crypto.Hash.sha3(
                    prefixMessage(msgBytes)
            );

            byte[] sigBytes = Numeric.hexStringToByteArray(signature);
            // signature = r (32) + s (32) + v (1)
            byte[] r = Arrays.copyOfRange(sigBytes, 0, 32);
            byte[] s = Arrays.copyOfRange(sigBytes, 32, 64);
            byte v = sigBytes[64];
            // Normalize v: MetaMask may send 27/28 or 0/1
            if (v < 27) v += 27;

            Sign.SignatureData sigData = new Sign.SignatureData(v, r, s);
            BigInteger recoveredKey = Sign.signedMessageHashToKey(prefixedHash, sigData);
            String recoveredAddress = "0x" + Keys.getAddress(recoveredKey);

            return recoveredAddress.equalsIgnoreCase(address);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] prefixMessage(byte[] message) {
        String prefix = "Ethereum Signed Message:\n" + message.length;
        byte[] prefixBytes = prefix.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = new byte[prefixBytes.length + message.length];
        System.arraycopy(prefixBytes, 0, result, 0, prefixBytes.length);
        System.arraycopy(message, 0, result, prefixBytes.length, message.length);
        return result;
    }
}
