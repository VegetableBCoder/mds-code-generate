package com.mds.codegen.gen;

/** 命名小工具。 */
public final class Naming {

    private Naming() {
    }

    public static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String decapitalize(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /** 字段名 → getter（契合 Lombok 与 MBG 实体）。 */
    public static String getter(String field) {
        return "get" + capitalize(field);
    }

    /** 字段名 → setter。 */
    public static String setter(String field) {
        return "set" + capitalize(field);
    }
}
