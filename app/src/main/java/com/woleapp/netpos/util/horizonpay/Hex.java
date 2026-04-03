package com.woleapp.netpos.util.horizonpay;

/***************************************************************************************************
 *                          Copyright (C),  Shenzhen Horizon Technology Limited                    *
 *                                   http://www.horizonpay.cn                                      *
 ***************************************************************************************************
 * usage           :
 * Version         : 1
 * Author          : Ashur Liu
 * Date            : 2017/12/18
 * Modify          : create file
 **************************************************************************************************/
public final class Hex {

    private static final char[] DIGITS = new char[]{'0', '1', '2', '3', '4',
            '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private Hex() {
        throw new AssertionError();
    }

    public static String encode(int i) {
        char[] cbuf = new char[8];
        int charPos = cbuf.length - 1;
        do {
            cbuf[charPos--] = DIGITS[i & 0xF];
            i >>>= 4;
            cbuf[charPos--] = DIGITS[i & 0xF];
            i >>>= 4;
        } while (i != 0);
        return new String(cbuf, charPos + 1, cbuf.length - charPos - 1);
    }

    public static String encode(final byte b) {
        return encode(new byte[]{b});
    }

    public static String encode(final byte[] b) {
        if (b == null) {
            return "";
        }
        final StringBuilder hex = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            int hiNibble = b[i] >> 4 & 0xF;
            int loNibble = b[i] & 0xF;
            hex.append(DIGITS[hiNibble]).append(DIGITS[loNibble]);
        }
        return hex.toString();
    }

    public static byte[] decode(String hex) {
        if (hex == null) {
            return null;
        }
        int len = hex.length();
        if (len > 0) {
            hex = hex.toUpperCase();
        }
        byte[] r = new byte[len / 2];
        for (int i = 0; i < r.length; i++) {
            int digit1 = hex.charAt(i * 2), digit2 = hex.charAt(i * 2 + 1);
            if (digit1 >= '0' && digit1 <= '9')
                digit1 -= '0';
            else if (digit1 >= 'A' && digit1 <= 'F')
                digit1 -= 'A' - 10;
            if (digit2 >= '0' && digit2 <= '9')
                digit2 -= '0';
            else if (digit2 >= 'A' && digit2 <= 'F')
                digit2 -= 'A' - 10;

            r[i] = (byte) ((digit1 << 4) + digit2);
        }
        return r;
    }

    private static int toDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'F') {
            return c - 55;
        } else if (c >= 'a' && c <= 'f') {
            return c - 87;
        }
        throw new IllegalArgumentException("Illegal character: " + c);
    }

    public static String HexStr2Str(String hex) {
        StringBuilder buf = new StringBuilder();
        char[] charArray = hex.toCharArray();
        for (int i = 0; i < charArray.length; i = i + 2) {
            String st = "" + charArray[i] + "" + charArray[i + 1];
            char ch = (char) Integer.parseInt(st, 16);
            buf.append(ch);
        }
        return buf.toString();
    }

    private static byte int2Bcd(final int value) {
        if (value > 99) {
            return 0;
        }
        return (byte) (value / 10 * 16 + value % 10);
    }

    public static byte[] subByte(byte[] b, int off, int length) {
        byte[] b1 = new byte[length];
        System.arraycopy(b, off, b1, 0, length);
        return b1;
    }

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

}