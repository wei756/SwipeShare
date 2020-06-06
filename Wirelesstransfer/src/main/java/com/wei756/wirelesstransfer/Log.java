package com.wei756.wirelesstransfer;

public class Log {
    public static void v(String tag, String msg) {
        System.out.println("V/" + tag + ": " + msg);
    }
    public static void d(String tag, String msg) {
        System.out.println("D/" + tag + ": " + msg);
    }
    public static void i(String tag, String msg) {
        System.out.println("I/" + tag + ": " + msg);
    }
    public static void w(String tag, String msg) {
        System.out.println("W/" + tag + ": " + msg);
    }
    public static void e(String tag, String msg) {
        System.out.println("E/" + tag + ": " + msg);
    }
}
