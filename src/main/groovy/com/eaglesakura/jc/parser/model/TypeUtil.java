package com.eaglesakura.jc.parser.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 形名の相互変換を行う
 */
public class TypeUtil {

    /**
     *
     */
    private static final Map<String, String> sJavaConvertMap = new HashMap<String, String>();

    /**
     * メソッド識別子変換マッピング
     */
    private static final Map<String, String> sMethodSignatureMap = new HashMap<String, String>();

    /**
     * JNIの型名変換マッピング
     */
    private static final Map<String, String> sJniTypeMap = new HashMap<String, String>();

    /**
     * JNIEnvメソッド名変換マッピング
     */
    private static final Map<String, String> sJniEnvMethodNameMap = new HashMap<String, String>();

    static {
        // デフォルトのコンバートテーブルを作成する
        sJavaConvertMap.put("byte", Constants.TYPENAME_LANG_BYTE);
        sJavaConvertMap.put("short", Constants.TYPENAME_LANG_SHORT);
        sJavaConvertMap.put("int", Constants.TYPENAME_LANG_INT);
        sJavaConvertMap.put("long", Constants.TYPENAME_LANG_LONG);
        sJavaConvertMap.put("float", Constants.TYPENAME_LANG_FLOAT);
        sJavaConvertMap.put("double", Constants.TYPENAME_LANG_DOUBLE);
        sJavaConvertMap.put("boolean", Constants.TYPENAME_LANG_BOOLEAN);
        sJavaConvertMap.put("char", Constants.TYPENAME_LANG_CHAR);
        sJavaConvertMap.put("void", Constants.TYPENAME_LANG_VOID);
        sJavaConvertMap.put(String.class.getName(), Constants.TYPENAME_LANG_STRING);
        sJavaConvertMap.put(Object.class.getName(), Constants.TYPENAME_LANG_ROOTCLASS);
        sJavaConvertMap.put("java.lang.Class", Constants.TYPENAME_LANG_CLASSOBJECT);

        // Javaメソッドの変換を取得する
        sMethodSignatureMap.put(Constants.TYPENAME_LANG_BYTE, "B");
        sMethodSignatureMap.put(Constants.TYPENAME_LANG_SHORT, "S");
        sMethodSignatureMap.put(Constants.TYPENAME_LANG_INT, "I");
        sMethodSignatureMap.put(Constants.TYPENAME_LANG_LONG, "J");
        sMethodSignatureMap.put(Constants.TYPENAME_LANG_FLOAT, "F");
        sMethodSignatureMap.put(Constants.TYPENAME_LANG_DOUBLE, "D");
        sMethodSignatureMap.put(Constants.TYPENAME_LANG_BOOLEAN, "Z");
        sMethodSignatureMap.put(Constants.TYPENAME_LANG_CHAR, "C");
        sMethodSignatureMap.put(Constants.TYPENAME_LANG_VOID, "V");
        sMethodSignatureMap.put(Constants.TYPENAME_LANG_STRING, "Ljava/lang/String;");
        sMethodSignatureMap.put(Constants.TYPENAME_LANG_CLASSOBJECT, "Ljava/lang/Class;");

        // JNIの型への変換を行う
        sJniTypeMap.put(Constants.TYPENAME_LANG_BYTE, "jbyte");
        sJniTypeMap.put(Constants.TYPENAME_LANG_SHORT, "jshort");
        sJniTypeMap.put(Constants.TYPENAME_LANG_INT, "jint");
        sJniTypeMap.put(Constants.TYPENAME_LANG_LONG, "jlong");
        sJniTypeMap.put(Constants.TYPENAME_LANG_FLOAT, "jfloat");
        sJniTypeMap.put(Constants.TYPENAME_LANG_DOUBLE, "jdouble");
        sJniTypeMap.put(Constants.TYPENAME_LANG_BOOLEAN, "jboolean");
        sJniTypeMap.put(Constants.TYPENAME_LANG_CHAR, "jchar");
        sJniTypeMap.put(Constants.TYPENAME_LANG_VOID, "void");
        sJniTypeMap.put(Constants.TYPENAME_LANG_STRING, "jstring");
        sJniTypeMap.put(Constants.TYPENAME_LANG_CLASSOBJECT, "jclass");

        // JNIのメソッド名テーブル
        sJniEnvMethodNameMap.put(Constants.TYPENAME_LANG_BYTE, "CallByteMethod");
        sJniEnvMethodNameMap.put(Constants.TYPENAME_LANG_SHORT, "CallShortMethod");
        sJniEnvMethodNameMap.put(Constants.TYPENAME_LANG_INT, "CallIntMethod");
        sJniEnvMethodNameMap.put(Constants.TYPENAME_LANG_LONG, "CallLongMethod");
        sJniEnvMethodNameMap.put(Constants.TYPENAME_LANG_FLOAT, "CallFloatMethod");
        sJniEnvMethodNameMap.put(Constants.TYPENAME_LANG_DOUBLE, "CallDoubleMethod");
        sJniEnvMethodNameMap.put(Constants.TYPENAME_LANG_BOOLEAN, "CallBooleanMethod");
        sJniEnvMethodNameMap.put(Constants.TYPENAME_LANG_CHAR, "CallCharMethod");
        sJniEnvMethodNameMap.put(Constants.TYPENAME_LANG_VOID, "CallVoidMethod");
    }

    /**
     * JNIEnv->CallXXXXMethodの名称を取得する
     *
     * @param resultJointTypeName 戻り値の変換後形名
     * @param arrayDimension      配列次元
     * @param isStatic            static
     * @return メソッド名
     */
    public static String getJniEnvMethodName(String resultJointTypeName, int arrayDimension, boolean isStatic) {
        if (arrayDimension > 0) {
            // 配列であればOjbectしかない
            if (isStatic) {
                return "CallStaticObjectMethod";
            } else {
                return "CallObjectMethod";
            }
        }

        String name = sJniEnvMethodNameMap.get(resultJointTypeName);
        if (name == null) {
            name = "CallObjectMethod";
        }

        if (isStatic) {
            // staticであればCall -> CallStaticに名称変更
            name = name.replace("Call", "CallStatic");
        }

        return name;
    }

    /**
     * JNI型名を取得する
     *
     * @param jointTypeName
     * @return
     */
    public static String getJniType(String jointTypeName, int arrayDimension) {
        String result = sJniTypeMap.get(jointTypeName);
        if (result == null) {
            // マッピングされているオブジェクト以外はjobjectを使用する
            if (arrayDimension == 0) {
                return "jobject";
            } else {
                return "jobjectArray";
            }
        } else {
            if (arrayDimension == 0) {
                return result;
            } else if (arrayDimension == 1) {
                // シンプルな配列であればそのまま返せる
                return result + "Array";
            } else {
                // それを超えたら、jobjectArrayを介する必要がある
                return "jobjectArray";
            }
        }
    }

    /**
     * JNIのsignに変換する。
     *
     * @param jointTypeName Javaのフルネーム
     * @param arrayDimen    配列の次元数。!=配列であれば0
     * @return signature
     */
    public static String getJniClassSignature(String jointTypeName, int arrayDimen) {
        String header = "";
        for (int i = 0; i < arrayDimen; ++i) {
            header += "[";
        }

        // rootであれば元に戻す
        if (jointTypeName.equals(Constants.TYPENAME_LANG_ROOTCLASS)) {
            jointTypeName = Object.class.getName();
        }

        String mapped = sMethodSignatureMap.get(jointTypeName);
        if (mapped != null) {
            return header + mapped;
        } else {
            return header + "L" + jointTypeName.replaceAll("\\.", "/") + ";";
        }
    }

    /**
     * Java型をJoint-Connector用の型に変換する
     *
     * @param javaType
     * @return
     */
    public static String convertToJointType(String javaType) {
        String parseTarget = javaType;
        String arrayFooder = "";
        while (parseTarget.endsWith("[]")) {
            arrayFooder += "[";
            parseTarget = parseTarget.substring(0, parseTarget.length() - 2);
        }


        final String result = sJavaConvertMap.get(parseTarget);
        if (result != null) {
            return arrayFooder + result;
        } else {
            return arrayFooder + parseTarget;
        }
    }

    /**
     * 特殊タイプである場合はtrue
     *
     * @param jointType
     * @return
     */
    public static boolean isJointType(String jointType) {
        Iterator<Map.Entry<String, String>> iterator = sJavaConvertMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (entry.getValue().equals(jointType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Joint-Connector用の型からJava型へ変換する
     *
     * @param jointType
     * @return
     */
    public static String convertToJavaType(String jointType) {
        Iterator<Map.Entry<String, String>> iterator = sJavaConvertMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (entry.getValue().equals(jointType)) {
                return entry.getKey();
            }
        }

        // 一致しない場合、そのまま独自型のためそのまま出力対象
        return jointType;
    }

    /**
     * 型一覧を返す
     */
    public static String[] listJointTypes() {
        return new String[]{
                Constants.TYPENAME_LANG_BYTE, //
                Constants.TYPENAME_LANG_SHORT, //
                Constants.TYPENAME_LANG_INT, //
                Constants.TYPENAME_LANG_LONG, //
                Constants.TYPENAME_LANG_FLOAT, //
                Constants.TYPENAME_LANG_DOUBLE, //
                Constants.TYPENAME_LANG_BOOLEAN, //
                Constants.TYPENAME_LANG_CHAR, //
                Constants.TYPENAME_LANG_VOID, //
                Constants.TYPENAME_LANG_STRING, //
                Constants.TYPENAME_LANG_ROOTCLASS, //
        };
    }

    /**
     * プリミティブ型一覧を返す
     */
    public static String[] listPrimitiveTypes() {
        return new String[]{
                Constants.TYPENAME_LANG_BYTE, //
                Constants.TYPENAME_LANG_SHORT, //
                Constants.TYPENAME_LANG_INT, //
                Constants.TYPENAME_LANG_LONG, //
                Constants.TYPENAME_LANG_FLOAT, //
                Constants.TYPENAME_LANG_DOUBLE, //
                Constants.TYPENAME_LANG_BOOLEAN, //
                Constants.TYPENAME_LANG_CHAR, //
                Constants.TYPENAME_LANG_VOID, //
        };
    }

    /**
     * 引数の型名がjobject/jstring/jclass以外であればtrue
     */
    public static boolean isJniPrimitiveType(String jointTypeName) {
        for (String name : listPrimitiveTypes()) {
            if (name.equals(jointTypeName)) {
                return true;
            }
        }
        return false;
    }
}
