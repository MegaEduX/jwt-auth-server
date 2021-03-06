package models;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Created by MegaEduX on 20/10/15.
 */

// /!\  NOT an Entity!
public class Password {
    private static int kIterationCount = 1024;

    //  Partially stolen from https://www.owasp.org/index.php/Hashing_Java

    public String digest;
    public String salt;

    public Password(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

        byte[] bSalt = new byte[8];

        random.nextBytes(bSalt);

        byte[] bDigest = getHash(kIterationCount, password, bSalt);

        this.digest = byteToBase64(bDigest);
        this.salt = byteToBase64(bSalt);
    }

    public Password(String digest, String salt) {
        this.digest = digest;
        this.salt = salt;
    }

    public boolean validate(String password) throws IOException, NoSuchAlgorithmException {
        byte[] digest = base64ToByte(this.digest);
        byte[] salt = base64ToByte(this.salt);

        byte[] hash = getHash(kIterationCount, password, salt);

        return Arrays.equals(digest, hash);
    }

    private byte[] getHash(int iterationNb, String password, byte[] salt)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        digest.reset();
        digest.update(salt);

        byte[] input = digest.digest(password.getBytes("UTF-8"));

        for (int i = 0; i < iterationNb; i++) {
            digest.reset();
            input = digest.digest(input);
        }

        return input;
    }

    private byte[] base64ToByte(String data) throws IOException {
        return Base64.getDecoder().decode(data);
    }

    private String byteToBase64(byte[] data){
        return Base64.getEncoder().encodeToString(data);
    }
}
