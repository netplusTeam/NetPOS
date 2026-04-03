package com.woleapp.netpos.util.horizonpay;

import android.util.Log;

import com.horizonpay.utils.FormatUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/***************************************************************************************************
 *                                  Copyright (C), Horizon Inc.                                      *
 *                                    http://www.Horizonpay.cn                                          *
 ***************************************************************************************************
 * File Name     : SoftwareDukpt
 * Description   : use the dukpt 16 
 * Version       : 1
 * Author        : bruce
 * Date          : 2024/12/16
 * Modification  : Created file
 ***************************************************************************************************/
public class SoftwareDukpt {

    private static String TAG = "SoftwareDukpt";

    private static int enCounter = 0;

    private static String sKSN = "";

    private static String sIPEK = "";

    private static boolean isInit = false;

    public static int getEnCounter() {
        return enCounter;
    }

    public static void setEnCounter(int enCounter) {
        SoftwareDukpt.enCounter = enCounter;
    }


    public static String getsKSN() {
        return sKSN;
    }

    public static void setsKSN(String sKSN) {
        SoftwareDukpt.sKSN = sKSN;
    }

    public static String getsIPEK() {
        return sIPEK;
    }

    public static void setsIPEK(String sIPEK) {
        SoftwareDukpt.sIPEK = sIPEK;
    }

    /***
     * get the pinblock
     * @param sPan
     * @param sPin
     * @return
     */
    public static String getPinblock(String sPan, String sPin) {

        if (!isInit) {
            loadDUKPT();
            isInit = true;
        }


        String workingKey = getSessionKey(getsIPEK(), getCurrentKsn());
        String result = DesEncryptDukpt(workingKey, sPan, sPin);
        Log.d(TAG, "getPinblock: result:" + result);
        return result;
    }

    /**
     * load dukpt key first
     */
    private static void loadDUKPT() {

        setsIPEK("3F2216D8297BCE9C");
        setsKSN("0000000002DDDDE00000");

        setEnCounter(1);
    }

    private static String XORorANDorORfunction(String A, String B, String symbol) {

        byte[] a = Hex.decode(A);
        byte[] b = Hex.decode(B);
        Log.d(TAG, "XORorANDorORfunction: a:" + A);
        Log.d(TAG, "XORorANDorORfunction: b:" + B);
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {


            if ("|".equals(symbol)) {
                result[i] = (byte) (a[i] | b[i]);
            } else if ("&".equals(symbol)) {
                result[i] = (byte) (a[i] & b[i]);
            } else if ("^".equals(symbol)) {
                result[i] = (byte) (a[i] ^ b[i]);
            }
        }


        Log.d(TAG, "XORorANDorORfunction: symbol" + symbol);
        Log.d(TAG, "XORorANDorORfunction: " + Hex.encode(result));

        return Hex.encode(result).substring(0, A.length());
    }


    private static String desEncrypt(String data, String key) {
        byte[] keyData = Hex.decode(key);
        byte[] desData = Hex.decode(data);
        byte[] byteFina = null;
        Cipher cipher;

        try {
            cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            DESKeySpec desKey = new DESKeySpec(keyData);
            //
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);
            cipher.init(Cipher.ENCRYPT_MODE, securekey);
            byteFina = cipher.doFinal(desData);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error initializing SqlMap class. Cause: " + e);
        } finally {
            cipher = null;
        }
        return Hex.encode(byteFina).substring(0, 16);
    }

    /**
     * encryptPinBlock get PIN block
     *
     * @param IPEK
     * @param KSN  密码
     * @return Session key string
     */
    private static String getSessionKey(String IPEK, String KSN) {
//        IPEK = "9F8011E7E71E483B";
        String initialIPEK = IPEK;

        String sessionkey = "";
        String ksn = FormatUtils.appendArray(KSN, 20, true, '0');

        String newKSN = XORorANDorORfunction(ksn, "0000FFFFFFFFFFE00000", "&");

        String counterKSN = ksn.substring(ksn.length() - 5);
        counterKSN = FormatUtils.appendArray(counterKSN, 16, true, '0');
        String newKSNtoleft16 = newKSN.substring(newKSN.length() - 16);
        String counterKSNbin = Integer.toBinaryString(Integer.parseInt(counterKSN));
        String binarycount = counterKSNbin;
        for (int i = 0; i < counterKSNbin.length(); i++) {
            int len = binarycount.length();
            String result = "";
            if (binarycount.substring(0, 1).equals("1")) {
                result = FormatUtils.appendArray("1", len, false, '0');
                binarycount = binarycount.substring(1);
            } else {
                binarycount = binarycount.substring(1);
                continue;
            }

            String counterKSN2 = Integer.toHexString(Integer.parseInt(result, 2)).toUpperCase();
            counterKSN2 = FormatUtils.appendArray(counterKSN2, 16, true, '0');
            String newKSN2 = XORorANDorORfunction(newKSNtoleft16, counterKSN2, "|");
            sessionkey = BlackBoxLogic(newKSN2, initialIPEK);
            newKSNtoleft16 = newKSN2;
            initialIPEK = sessionkey;
        }
        String checkWorkingKey = XORorANDorORfunction(sessionkey, "00000000000000FF00000000000000FF", "^");
        Log.d(TAG, "getSessionKey: working key:" + checkWorkingKey);
        return checkWorkingKey;
    }

    private static String BlackBoxLogic(String ksn, String ipek) {
        if (ipek.length() < 32) {

            String msg = XORorANDorORfunction(ipek, ksn, "^");
            String desreslt = desEncrypt(msg, ipek);
            String rsesskey = XORorANDorORfunction(desreslt, ipek, "^");
            return rsesskey;
        }
        String current_sk = ipek;
        String ksn_mod = ksn;
        String leftIpek = XORorANDorORfunction(current_sk, "FFFFFFFFFFFFFFFF0000000000000000", "&").substring(0, 16);
        String rightIpek = XORorANDorORfunction(current_sk, "0000000000000000FFFFFFFFFFFFFFFF", "&").substring(16);
        String message = XORorANDorORfunction(rightIpek, ksn_mod, "^");
        String desresult = desEncrypt(message, leftIpek);
        String rightSessionKey = XORorANDorORfunction(desresult, rightIpek, "^");

        String resultCurrent_sk = XORorANDorORfunction(current_sk, "C0C0C0C000000000C0C0C0C000000000", "^");
        String leftIpek2 = XORorANDorORfunction(resultCurrent_sk, "FFFFFFFFFFFFFFFF0000000000000000", "&").substring(0, 16);
        String rightIpek2 = XORorANDorORfunction(resultCurrent_sk, "0000000000000000FFFFFFFFFFFFFFFF", "&").substring(16);
        String message2 = XORorANDorORfunction(rightIpek2, ksn_mod, "^");
        String desresult2 = desEncrypt(message2, leftIpek2);
        String leftSessionKey = XORorANDorORfunction(desresult2, rightIpek2, "^");

        return leftSessionKey + rightSessionKey;

    }

    /**
     * encryptPinBlock get PIN block
     *
     * @param PAN
     * @param PIN
     * @return Pin block String
     */
    private static String encryptPinBlock(String PAN, String PIN) {
        if (PAN.isEmpty() || PAN.length() < 14 || PIN.length() < 4) {
            return null;
        }

        String pan = PAN.substring((PAN.length() - 13), PAN.length() - 1);
        pan = FormatUtils.appendArray(pan, 16, true, '0');
        String pin = Hex.encode((byte) PIN.length()) + FormatUtils.appendArray(PIN, 16, false, 'F');
        Log.d(TAG, "encryptPinBlock: " + pin);
        pin = pin.substring(0, 16);
        String pinblock = XORorANDorORfunction(pan, pin, "^");
        Log.d(TAG, "encryptPinBlock: " + pinblock);
        return pinblock;
    }

    private static String DesEncryptDukpt(String workingKey, String pan, String clearPin) {
        String pinBlock = XORorANDorORfunction(workingKey, encryptPinBlock(pan, clearPin), "^");
        byte[] keyData = Hex.decode(workingKey);

        byte[] byteFina = null;
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            DESKeySpec desKey = new DESKeySpec(keyData);
            //
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);
            cipher.init(Cipher.ENCRYPT_MODE, securekey);
            byteFina = cipher.doFinal(Hex.decode(pinBlock));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error initializing SqlMap class. Cause: " + e);
        } finally {
            cipher = null;
        }

        String data = Hex.encode(byteFina).substring(0, 16);
        String reslut = XORorANDorORfunction(workingKey, data, "^");
        return reslut;
    }

    private static String updateKSN(int encCntr, String KSN) {
        /**
         * @method: updateKSN
         * @description: Update the key serial number register (KSN) with the current value of the
         * //  encryption counter. Note that the left-most 59 bits of the KSN are the
         * //  59 right-most bits of the initial serial number itself, and the 21
         * //  rightmost bits of KSN are the curent value of encryption counter.
         * @date: 3/8/2021 11:47 PM
         * @author: Atos
         * @param: [encCntr, KSN]
         * @return: java.lang.String
         */


        Log.d(TAG, "Ksn Counter: >>>>>>>>" + encCntr);
        byte[] ksn = Hex.decode(KSN);
        if (encCntr > 99999) {
            encCntr = 1;
            setEnCounter(encCntr);
        }

        String aa = String.valueOf(encCntr);
        aa = FormatUtils.appendArray(aa, 5, true, '0');
        aa = "E" + aa;
        byte[] bb = Hex.decode(aa);
        ksn[9] = bb[2];
        ksn[8] = bb[1];
        ksn[7] = bb[0];
        Log.d(TAG, "updateKSN: " + Hex.encode(ksn));
        return Hex.encode(ksn);
    }

    public static String getCurrentKsn() {
        return updateKSN(getEnCounter(), getsKSN());
    }

    public static String increaseKsn() {
        int i = getEnCounter();
        i++;
        setEnCounter(i);
        return updateKSN(getEnCounter(), getsKSN());
    }

}
