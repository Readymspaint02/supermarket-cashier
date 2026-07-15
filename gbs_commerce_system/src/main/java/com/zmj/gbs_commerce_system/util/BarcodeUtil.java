package com.zmj.gbs_commerce_system.util;

import java.util.Random;

public class BarcodeUtil {

    private static final String PREFIX = "690";
    private static final Random random = new Random();

    public static String generateEAN13() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < 9; i++) {
            sb.append(random.nextInt(10));
        }
        String code12 = sb.toString();
        int checkDigit = calculateCheckDigit(code12);
        return code12 + checkDigit;
    }

    public static boolean validateEAN13(String barcode) {
        if (barcode == null || barcode.length() != 13) {
            return false;
        }
        try {
            int expectedCheck = calculateCheckDigit(barcode.substring(0, 12));
            int actualCheck = Integer.parseInt(barcode.substring(12));
            return expectedCheck == actualCheck;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int calculateCheckDigit(String code12) {
        int sumOdd = 0;
        int sumEven = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Integer.parseInt(code12.substring(i, i + 1));
            if ((i + 1) % 2 == 1) {
                sumOdd += digit;
            } else {
                sumEven += digit;
            }
        }
        int total = sumOdd + sumEven * 3;
        int checkDigit = (10 - (total % 10)) % 10;
        return checkDigit;
    }

    public static String generateProductCode(Long productId) {
        String idStr = String.format("%09d", productId);
        String code12 = PREFIX + idStr.substring(idStr.length() - 9);
        int checkDigit = calculateCheckDigit(code12);
        return code12 + checkDigit;
    }
}