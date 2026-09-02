package com.uccd3223.group13.foodhero.util;

import java.util.Locale;

public class CurrencyUtils {
    public static String format(double amount) {
        return String.format(Locale.US, "RM %.2f", amount);
    }
}
