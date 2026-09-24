package com.example.leads.util;

public final class PhoneUtil {

    private PhoneUtil() { }

    public static String normalize(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return digits;
    }

    public static boolean isValid(String normalized) {
        return normalized != null && normalized.matches("^[6-9]\\d{9}$");
    }
}