//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.woleapp.netpos.util.horizonpay;

import com.horizonpay.utils.BaseUtils;

import java.util.Arrays;

public class ConvertUtils {
    private static final char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private ConvertUtils() {
    }

    public static String bytes2HexString(byte[] bytes, String split) {
        if (bytes != null && bytes.length > 0) {
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < bytes.length; ++i) {
                sb.append(hexDigits[bytes[i] >> 4 & 15]);
                sb.append(hexDigits[bytes[i] & 15]);
                if (i != bytes.length - 1 && split != null) {
                    sb.append(split);
                }
            }

            return sb.toString();
        } else {
            return "";
        }
    }

    public static String bytes2HexString(byte[] bytes) {
        return bytes2HexString(bytes, (String)null);
    }

    public static byte[] hexString2Bytes(String hexString) {
        String tmp = formatHexString(hexString);
        if (tmp == null) {
            return null;
        } else {
            int len = tmp.length();
            char[] hexBytes = tmp.toCharArray();
            byte[] ret = new byte[len >> 1];

            for(int i = 0; i < len; i += 2) {
                ret[i >> 1] = (byte)(hex2Dec(hexBytes[i]) << 4 | hex2Dec(hexBytes[i + 1]));
            }

            return ret;
        }
    }

    public static byte[] long2BytesHex(long value, int len) {
        byte[] ret = new byte[len];
        Arrays.fill(ret, (byte)0);
        long tmp = value;

        for(int i = 0; i < ret.length; ++i) {
            ret[ret.length - 1 - i] = (byte)((int)(tmp & 255L));
            tmp >>= 8;
            if (tmp <= 0L) {
                break;
            }
        }

        return ret;
    }

    public static long bytesHex2Long(byte[] bytes, int offset, int len) {
        long ret = 0L;
        if (bytes != null && bytes.length > 0) {
            if (offset + len > bytes.length) {
                return ret;
            } else {
                for(int i = 0; i < len; ++i) {
                    ret <<= 8;
                    ret |= (long)(bytes[offset + i] & 255);
                }

                return ret;
            }
        } else {
            return ret;
        }
    }

    public static byte[] long2BytesBCD(long value, int len) {
        byte[] ret = new byte[len];
        Arrays.fill(ret, (byte)0);
        long tmp = value;

        for(int i = 0; i < ret.length; ++i) {
            ret[ret.length - 1 - i] = int2Bcd((int)(tmp % 100L));
            tmp /= 100L;
            if (tmp <= 0L) {
                break;
            }
        }

        return ret;
    }

    public static long bytesBCD2Long(byte[] bytes, int offset, int len) {
        long ret = 0L;
        if (bytes != null && bytes.length > 0) {
            if (offset + len > bytes.length) {
                return ret;
            } else {
                for(int i = 0; i < len; ++i) {
                    ret *= 100L;
                    ret += (long)bcd2Int(bytes[offset + i]);
                }

                return ret;
            }
        } else {
            return ret;
        }
    }

    public static int dp2px(float dpValue) {
        float scale = BaseUtils.getApp().getResources().getDisplayMetrics().density;
        return (int)(dpValue * scale + 0.5F);
    }

    public static int px2dp(float pxValue) {
        float scale = BaseUtils.getApp().getResources().getDisplayMetrics().density;
        return (int)(pxValue / scale + 0.5F);
    }

    public static int sp2px(float spValue) {
        float fontScale = BaseUtils.getApp().getResources().getDisplayMetrics().scaledDensity;
        return (int)(spValue * fontScale + 0.5F);
    }

    public static int px2sp(float pxValue) {
        float fontScale = BaseUtils.getApp().getResources().getDisplayMetrics().scaledDensity;
        return (int)(pxValue / fontScale + 0.5F);
    }

    public static String formatHexString(String hexString) {
        if (hexString != null && hexString.length() > 0) {
            String ret = hexString.toUpperCase();
            char[] retChars = ret.toCharArray();
            boolean formatCorrect = true;

            for(char c : retChars) {
                if ((c < '0' || c > '9') && (c < 'A' || c > 'F')) {
                    formatCorrect = false;
                    break;
                }
            }

            if (formatCorrect) {
                return ret.length() % 2 != 0 ? "0" + ret : ret;
            } else {
                ret = ret.replaceAll("0X", "");
                retChars = ret.toCharArray();
                StringBuilder sb = new StringBuilder();

                for(char c : retChars) {
                    if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F') {
                        sb.append(c);
                    }
                }

                if (sb.length() % 2 != 0) {
                    sb.insert(0, '0');
                }

                return sb.toString();
            }
        } else {
            return null;
        }
    }

    public static String formatNumString(String str) {
        if (str != null && str.length() > 0) {
            char[] retChars = str.toCharArray();
            boolean formatCorrect = true;

            for(char c : retChars) {
                if (c < '0' || c > '9') {
                    formatCorrect = false;
                    break;
                }
            }

            if (formatCorrect) {
                return str;
            } else {
                StringBuilder sb = new StringBuilder();

                for(char c : retChars) {
                    if (c >= '0' && c <= '9') {
                        sb.append(c);
                    }
                }

                return sb.toString();
            }
        } else {
            return null;
        }
    }

    public static byte[] ascString2BcdByte(String ascString) {
        byte[] a = new byte[(ascString.length() + 2) / 4];
        int i = 0;
        char[] b = ascString.toCharArray();

        for(int j = 0; i < ascString.length(); ++i) {
            if (b[i] < '0' && b[i] > '9') {
                return a;
            }

            if (b[i] != '3') {
                return a;
            }

            a[j] = (byte)(b[i + 1] - 48 & 15);
            if (ascString.length() > 3) {
                byte d = (byte)(b[i + 3] - 48 & 15);
                a[j] = (byte)(a[j] << 4 | d);
                i += 3;
            } else {
                ++i;
            }

            ++j;
        }

        return a;
    }

    public static byte[] intString2BcdByte(String intString) {
        byte[] a = new byte[(intString.length() + 1) / 2];
        int i = 0;
        char[] b = intString.toCharArray();

        for(int j = 0; i < intString.length(); ++i) {
            if (b[i] < '0' && b[i] > '9') {
                return a;
            }

            a[j] = (byte)(b[i] - 48 & 15);
            if (i > 1) {
                byte d = (byte)(b[i + 1] - 48 & 15);
                a[j] = (byte)(a[j] << 4 | d);
                i += 2;
            } else {
                ++i;
            }

            ++j;
        }

        return a;
    }

    private static int hex2Dec(char hexChar) {
        if (hexChar >= '0' && hexChar <= '9') {
            return hexChar - 48;
        } else if (hexChar >= 'A' && hexChar <= 'F') {
            return hexChar - 65 + 10;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static int bcd2Int(byte b) {
        int tmpb = b & 255;
        return tmpb / 16 * 10 + tmpb % 16;
    }

    private static byte int2Bcd(int value) {
        return value > 99 ? 0 : (byte)(value / 10 * 16 + value % 10);
    }

    public static int bytesToInt(byte[] b) {
        String s = new String(b);
        return Integer.parseInt(s);
    }
}
