package org.mult.daap.client.daap.request;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Hasher {
    private static final String hexchars = "0123456789ABCDEF";

    private static String DigestToString(byte[] digest) {
        StringBuilder string = new StringBuilder();
        // int length = digest.length;
        for (byte tmp : digest) {
            string.append(hexchars.charAt((tmp >> 4) & 0x0f));
            string.append(hexchars.charAt(tmp & 0x0f));
        }
        return string.toString();
    }

    static String GenerateHash(String url) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("md5");
        md.update(url.getBytes());
        return DigestToString(md.digest());
    }
}