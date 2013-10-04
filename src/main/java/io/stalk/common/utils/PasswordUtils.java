package io.stalk.common.utils;

import java.security.MessageDigest;

public class PasswordUtils {

    public static String encrypt(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(password.getBytes("UTF-8"));
            byte[] digested = md.digest();
            return new String(Base64Coder.encode(digested));
        } catch (Exception e) {
            return password;
        }

    }


}
