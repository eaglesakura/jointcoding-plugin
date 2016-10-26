package com.eaglesakura.jc.parser.model;

/**
 * Model系の定数を定義する
 */
public interface Constants {
    /**
     * byte型を示すクラス名
     */
    public static final String TYPENAME_LANG_BYTE = "jc.lang.s8";

    /**
     * short型を示すクラス名
     */
    public static final String TYPENAME_LANG_SHORT = "jc.lang.s16";

    /**
     * int型を示すクラス名
     */
    public static final String TYPENAME_LANG_INT = "jc.lang.s32";

    /**
     * long型を示すクラス名
     */
    public static final String TYPENAME_LANG_LONG = "jc.lang.s64";

    /**
     * float型を示すクラス名
     */
    public static final String TYPENAME_LANG_FLOAT = "jc.lang.float";

    /**
     * double型を示すクラス名
     */
    public static final String TYPENAME_LANG_DOUBLE = "jc.lang.double";

    /**
     * boolean型を示すクラス名
     */
    public static final String TYPENAME_LANG_BOOLEAN = "jc.lang.boolean";

    /**
     * char型を示すクラス名
     */
    public static final String TYPENAME_LANG_CHAR = "jc.lang.char";

    /**
     * String型を示すクラス名
     */
    public static final String TYPENAME_LANG_STRING = "jc.lang.string";

    /**
     * void型を示すクラス名
     */
    public static final String TYPENAME_LANG_VOID = "jc.lang.void";

    /**
     * Class型を示すクラス名
     */
    public static final String TYPENAME_LANG_CLASSOBJECT = "jc.lang.class";

    /**
     * 全てのオブジェクトの継承元を示す
     * java.lang.Object型がそれにあたる
     */
    public static final String TYPENAME_LANG_ROOTCLASS = "jc.lang.rootclass";
}