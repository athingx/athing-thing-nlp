package com.github.athingx.athing.aliyun.nlp.component.util;

/**
 * 系统工具类
 */
public class SystemUtils {

    private static final String OS = System.getProperty("os.name");
    private static final String BITS = System.getProperty("sun.arch.data.model");

    public static String getArchBit() {
        return BITS;
    }

    public static boolean isArchBit64() {
        return BITS.equals("64");
    }

    public static boolean isArchBit32() {
        return BITS.equals("32");
    }

    public static String getOs() {
        return OS;
    }

    public static boolean isMacOsX() {
        return OS.equalsIgnoreCase("mac os x");
    }

    public static boolean isLinux() {
        return OS.equalsIgnoreCase("Linux");
    }

}
