package com.loopify.mainservice.utils;

import java.util.Random;

public class Utils {

    public static String generateNumericCode(int length) {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10)); // 0-9
        }
        return code.toString();
    }
}
