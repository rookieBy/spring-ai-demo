package com.wifiin.newsay.ai.common.utils;

import cn.hutool.core.util.StrUtil;

import java.util.UUID;

/**
 * String Utilities
 */
public class StringUtils {

    public static boolean isBlank(String str) {
        return StrUtil.isBlank(str);
    }

    public static boolean isNotBlank(String str) {
        return StrUtil.isNotBlank(str);
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String defaultIfEmpty(String str, String defaultStr) {
        return isBlank(str) ? defaultStr : str;
    }
}
