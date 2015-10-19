package models;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.Transactional;

import play.data.validation.Constraints;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.persistence.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by MegaEduX on 19/10/15.
 */

@Entity
public class User extends Model {
    private static int kIterationCount = 1024;

    @Entity
    private class Password {
        //  Partially stolen from https://www.owasp.org/index.php/Hashing_Java

        @Id
        public Long id;

        @Constraints.Required
        public String digest;

        @Constraints.Required
        public String salt;

        public Password(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

            byte[] bSalt = new byte[8];

            random.nextBytes(bSalt);

            byte[] bDigest = getHash(User.kIterationCount, password, bSalt);

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

            byte[] hash = getHash(User.kIterationCount, password, salt);

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
            BASE64Decoder decoder = new BASE64Decoder();
            return decoder.decodeBuffer(data);
        }

        private String byteToBase64(byte[] data){
            BASE64Encoder encoder = new BASE64Encoder();

            return encoder.encode(data);
        }
    }

    @Id
    public Long id;

    @Constraints.Required
    public String username;

    @Constraints.Required
    public Password password;

    @Constraints.Required
    public String emailAddress;

    public HashMap<String, String> additional;

    public User(String username, String password, String emailAddress) {
        this.username = username;

        try {
            this.password = new Password(password);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        this.emailAddress = emailAddress;
    }

    @Transactional
    public void save() {
        //  Uh.
    }
}
