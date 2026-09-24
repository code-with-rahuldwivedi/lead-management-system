package com.example.leads.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneUtilTest {

    @Test
    void normalisesCommonFormats() {
        assertEquals("9876543210", PhoneUtil.normalize("+91 98765-43210"));
        assertEquals("9876543210", PhoneUtil.normalize("09876543210"));
    }

    @Test
    void validatesIndianMobiles() {
        assertTrue(PhoneUtil.isValid("9876543210"));
        assertFalse(PhoneUtil.isValid("5876543210"));
        assertFalse(PhoneUtil.isValid("98765"));
    }
}