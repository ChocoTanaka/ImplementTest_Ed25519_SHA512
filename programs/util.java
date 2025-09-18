package com.example.sighner;

public class util {
	static class HexUtil {
        private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
        
        /**
         * Convert byte array to hexadecimal string
         */
        public static String byteArrayToHexString(byte[] bytes) {
            if (bytes == null) {
                return null;
            }
            
            char[] hexChars = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xFF;
                hexChars[i * 2] = HEX_CHARS[v >>> 4];
                hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
            }
            return new String(hexChars);
        }
        
        /**
         * Convert hexadecimal string to byte array
         */
        public static byte[] hexStringToByteArray(String hex) {
            if (hex == null || hex.length() % 2 != 0) {
                return null;
            }
            
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                int index = i * 2;
                int v = hexCharToInt(hex.charAt(index)) << 4;
                v |= hexCharToInt(hex.charAt(index + 1));
                bytes[i] = (byte) v;
            }
            return bytes;
        }
        
        private static int hexCharToInt(char c) {
            if (c >= '0' && c <= '9') {
                return c - '0';
            } else if (c >= 'a' && c <= 'f') {
                return c - 'a' + 10;
            } else if (c >= 'A' && c <= 'F') {
                return c - 'A' + 10;
            }
            throw new IllegalArgumentException("Invalid hex character: " + c);
        }
    }
}
