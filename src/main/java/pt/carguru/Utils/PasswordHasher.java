package pt.carguru.Utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    public static String hash(String plain) { return BCrypt.hashpw(plain, BCrypt.gensalt(12)); }
    public static boolean verify(String plain, String hashed) {
        try { return BCrypt.checkpw(plain, hashed); } catch (Exception e) { return false; }
    }
}
