package com.eaglesakura.jc.generator.cpp

import com.eaglesakura.jc.generator.wrapper.JointClassWrapper
import com.eaglesakura.jc.generator.wrapper.JointFieldWrapper
import com.eaglesakura.jc.generator.wrapper.JointMethodWrapper
import com.eaglesakura.jc.parser.model.TypeUtil
import com.eaglesakura.tool.log.Logger
import com.eaglesakura.tool.generator.CodeWriter
import com.eaglesakura.util.IOUtil

/**
 * メソッドの出力を行う
 */
class CppSourceGenerator {
    /**
     * Classオブジェクト管理クラス
     */
    public static final String CLASS_WRAPPER = "::jc::lang::class_wrapper";

    /**
     * 出力するクラス名
     *
     * interfaceの場合はstubを生成してあげる。
     */
    private String wrapperClassName;

    JointClassWrapper wrapper;

    /**
     * クラスオブジェクト
     */
    String classObjectName;

    /**
     * メソッドオブジェクト
     */
    String methodObjectName;

    /**
     * メソッドオブジェクト
     */
    String fieldObjectName;

    /**
     * 初期化関数名
     */
    String initializeFunctionName;

    /**
     * インターフェースStubを生成する場合はtrue
     */
    boolean interfaceStubMode = false;

    /**
     * 出力対象のメソッド一覧
     */
    List<JointMethodWrapper> methods = new ArrayList<JointMethodWrapper>();

    /**
     * 出力対象のフィールド一覧
     */
    List<JointFieldWrapper> implVariableFields = new ArrayList<JointFieldWrapper>();

    CppSourceGenerator(JointClassWrapper wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * ファイルへの書き込みを行う
     *
     * ファイル名は自動で確定される
     * @param directory 書き込み先ディレクトリ
     */
    public void build(File directory) {
        if (interfaceStubMode && wrapper.interface) {
            // Stub出力モード
            wrapperClassName = "${wrapper.className}Stub";
        } else {
            wrapperClassName = wrapper.className;
        }

        implVariableFields = wrapper.addedVariableFields;

        Logger.pushIndent();
        Logger.pushIndent();
        OUT_FILE:
        {
            classObjectName = "class_${wrapper.className}";
            methods = wrapper.addedMethods;

            String headerFileName = CppHeaderGenerator.getHeaderName(wrapper);
            if (interfaceStubMode) {
                headerFileName = headerFileName.replace(".h", "Stub.h");
            }
            File out = new File(directory, headerFileName.replace(".h", "_impl.cpp"));
            CodeWriter writer = new CodeWriter(out);

            // ヘッダ部を出力させる
            generateHeaderIncludes(writer);

            // ネームスペースを出力させる
            generateNamespace(writer);

            // スタブを出力する
            generateMethodStubs(writer);

            writer.commit();
        }
        Logger.popIndent();
        Logger.popIndent();
    }

    /**
     *
     * @param writer
     */
    private void generateHeaderIncludes(CodeWriter writer) {
        // include
        writer.writeLine("#include <array>");

        String headerName = CppHeaderGenerator.getHeaderName(wrapper);
        if (interfaceStubMode) {
            headerName = headerName.replace(".h", "Stub.h");
        }
        writer.writeLine("#include \"${headerName}\"");

        writer.newLine();
    }

    /**
     * ネームスペース部分を出力する
     * @param writer
     */
    private void generateNamespace(CodeWriter writer) {
        // 必要なネームスペースを出力する
        def namespaces = CppHeaderGenerator.getNamespaces(wrapper);
        for (def ns : namespaces) {
            writer.writeLine("namespace ${ns} {");
        }
        writer.newLine();

        // 初期化部分を記述する
        generateClassInitializer(writer);

        // コードを出力する
        int index = 0;
        index = 0;
        for (def field : implVariableFields) {
            generateFieldCall(writer, index, field);
            ++index;
        }


        index = 0;
        for (def method : methods) {
            generateMethodCall(writer, index, method);
            ++index;
        }


        writer.newLine();
        for (def ns : namespaces) {
            writer.writeLine("}");
        }

    }

    /**
     * クラスの初期化部分を出力する
     */
    private void generateClassInitializer(CodeWriter writer) {

        // 汚染を避けるために無名namespaceで囲う
        writer.writeLine("namespace {").newLine();

        // クラスオブジェクトを生成する
        writer.writeLine("static ${CLASS_WRAPPER} ${classObjectName};");
        // メソッドオブジェクトを生成する
        if (!methods.empty) {
            methodObjectName = "${classObjectName}_methods";
            writer.writeLine("static ::std::array<jmethodID, ${methods.size()}> ${methodObjectName};")
        }
        // フィールドオブジェクトを生成する
        if (!implVariableFields.empty) {
            fieldObjectName = "${classObjectName}_fields";
            writer.writeLine("static ::std::array<jfieldID, ${implVariableFields.size()}> ${fieldObjectName};")
        }

        // 初期化関数を生成する
        initializeFunctionName = IOUtil.getFileName(CppHeaderGenerator.getHeaderName(wrapper)) + "_initialize";
        writer.writeLine("static void ${initializeFunctionName}(JNIEnv *env) {");
        writer.pushIndent(false).newLine();

        // 初期化済みチェック
        writer.writeLine("if(${classObjectName}.hasObject()){ return; } // initialized").newLine();

        // Classを読み込み、チェックする
        writer.writeLine("${classObjectName} = ${CLASS_WRAPPER}::find(env, \"${getClassSignature(wrapper)}\");")
        writer.writeLine("assert(${classObjectName}.hasObject());").newLine();

        // Global指定とマルチスレッド前提とする
        writer.writeLine("${classObjectName}.globalRef().setMultiThreadAccess(true);");

        LOAD_METHODS:
        {
            // メソッドIDを取得する
            writer.writeLine("/* load method */");

            int methodNumber = 0;
            // メソッドを読み込む
            for (def method : methods) {
                writer.writeLine("${methodObjectName}[${methodNumber}] = ${classObjectName}.getMethod(\"${method.name}\", \"${getMethodSignature(method)}\", ${method.isStaticMethod()});")
                writer.writeLine("assert(${methodObjectName}[${methodNumber}]);");
                ++methodNumber;
            }
        }

        LOAD_FIELDS:
        {
            // フィールドIDを取得する
            writer.writeLine("/* load field */")
            int fieldNumber;
            // メソッドを読み込む
            for (def field : implVariableFields) {
                writer.writeLine("${fieldObjectName}[${fieldNumber}] = ${classObjectName}.getField(\"${field.name}\", \"${TypeUtil.getJniClassSignature(field.classWrapper.javaFullName, field.classWrapper.arrayDimension)}\", ${field.isStaticField()});")
                writer.writeLine("assert(${fieldObjectName}[${fieldNumber}]);");
                ++fieldNumber;
            }
        }

        writer.popIndent(true);
        writer.writeLine("}");

        writer.newLine().writeLine("}");
    }

    /**
     * フィールド自体のIOを書き込む
     */
    private void generateFieldCall(CodeWriter writer, int fieldNumber, JointFieldWrapper field) {
        def resultType = field.classWrapper;
        def resultTypeCpp = CppHeaderGenerator.getCppFullName(field.classWrapper, false);

        GETTER:
        {
            def methodName = TypeUtil.getJniEnvMethodName(resultType.javaFullName, resultType.arrayDimension, field.isStaticField());
            writer.writeLine("${resultTypeCpp} ${wrapper.className}::field_${field.name}() {");
            writer.pushIndent(true);

            // env取得
            writer.write("JNIEnv *env = ");
            if (field.isStaticField()) {
                writer.writeLine("${classObjectName}.getEnv();")
            } else {
                writer.writeLine("${CppHeaderGenerator.OBJECT_WRAPPER_VAR}.getEnv();")
            }

            // 初期化メソッドを必ず経由する
            writer.writeLine("${initializeFunctionName}(env);");

            // env check
            writer.writeLine("if(!env){ env = ${classObjectName}.getEnv(); }")

            def jniMethodName = methodName.replaceAll("Call", "Get").replaceAll("Method", "Field");
            def jniValueType = TypeUtil.getJniType(resultType.javaFullName, resultType.arrayDimension);
            if (jniMethodName.contains("Object")) {
                writer.write("return ::jc::lang::wrapFromVM< ${resultTypeCpp}, ${jniValueType}>(env, (${jniValueType})");
            } else {
                writer.write("return ${resultTypeCpp}(");
            }

            // call
            writer.write("env->${jniMethodName}(");

            // 第1引数 jobject
            if (field.isStaticField()) {
                writer.write("${classObjectName}.getJclass(), ");
            } else {
                writer.write("mObject.getJobject(), ");
            }

            // 第2引数 fieldId
            writer.write("${fieldObjectName}[${fieldNumber}]));");

            writer.popIndent(true);
            writer.writeLine("}");
        }

        SETTER:
        {
            def methodName = TypeUtil.getJniEnvMethodName(resultType.javaFullName, resultType.arrayDimension, field.isStaticField());
            writer.writeLine("void ${wrapper.className}::field_${field.name}(${resultTypeCpp} set) {")
            writer.pushIndent(true);

            // env取得
            writer.write("JNIEnv *env = ");
            if (field.isStaticField()) {
                writer.writeLine("${classObjectName}.getEnv();")
            } else {
                writer.writeLine("${CppHeaderGenerator.OBJECT_WRAPPER_VAR}.getEnv();")
            }

            // 初期化メソッドを必ず経由する
            writer.writeLine("${initializeFunctionName}(env);");

            // env check
            writer.writeLine("if(!env){ env = ${classObjectName}.getEnv(); }")


            def jniMethodName = methodName.replaceAll("Call", "Set").replaceAll("Method", "Field");
            writer.write("env->${jniMethodName}(");

            // 第1引数 jobject
            if (field.isStaticField()) {
                writer.write("${classObjectName}.getJclass(), ");
            } else {
                writer.write("mObject.getJobject(), ");
            }

            // 第2引数 fieldId
            writer.write("${fieldObjectName}[${fieldNumber}], set");

            // 第3引数 value
            if (jniMethodName.contains("Object")) {
                writer.write(".getJobject()");
            }

            // 閉じる
            writer.write(");");

            writer.popIndent(true);
            writer.writeLine("}");
        }
    }

    /**
     * メソッド本体を書き込む
     */
    private void generateMethodCall(CodeWriter writer, int methodNumber, JointMethodWrapper method) {
        def resultType = method.resultType;
        def resultTypeCpp = CppHeaderGenerator.getCppFullName(resultType, false);

        writer.write("${resultTypeCpp} ${wrapperClassName}::${method.name}(");

        // 引数リスト
        def argmetns = method.argmentList;
        int index = 0;
        for (def arg : argmetns) {
            if (index != 0) {
                writer.write(", ");
            }

            writer.write("${CppHeaderGenerator.getCppFullName(arg.classWrapper, false)} ${arg.name}");
            ++index;
        }

        // 本文
        writer.writeLine(") {");

        writer.pushIndent(true);

        // env取得
        writer.write("JNIEnv *env = ");
        if (method.isStaticMethod()) {
            writer.writeLine("${classObjectName}.getEnv();")
        } else {
            writer.writeLine("${CppHeaderGenerator.OBJECT_WRAPPER_VAR}.getEnv();")
        }

        // 初期化メソッドを必ず経由する
        writer.writeLine("${initializeFunctionName}(env);");

        // env check
        writer.writeLine("if(!env){ env = ${classObjectName}.getEnv(); }")

        // コール
        CALL:
        {
            def jniEnvMethodName = TypeUtil.getJniEnvMethodName(resultType.javaFullName, resultType.arrayDimension, method.isStaticMethod());

            // 戻り値を持っている場合
            if (!resultType.isVoid()) {
                // xxxObjctMethodの場合はtemplate版
                if (jniEnvMethodName.contains("ObjectMethod")) {
                    writer.write("return ::jc::lang::wrapFromVM< ${resultTypeCpp}, ${TypeUtil.getJniType(resultType.javaFullName, resultType.arrayDimension)}>(env, ");
                } else {
                    writer.write("return ${resultTypeCpp}(")
                }
            }

            // 呼び出す
            writer.write("env->${jniEnvMethodName}(");

            // 第1引数 jobject
            if (method.isStaticMethod()) {
                writer.write("${classObjectName}.getJclass(), ");
            } else {
                writer.write("mObject.getJobject(), ");
            }

            // 第2引数 methodId
            writer.write("${methodObjectName}[${methodNumber}]");

            // 引数リストを呼び出す
            def args = method.argmentList;
            for (def arg : args) {
                if (!arg.classWrapper.isArray() && arg.classWrapper.isJniPrimitive()) {
                    writer.write(", ${arg.name}");
                } else {
                    writer.write(", ${arg.name}.getJobject()");
                }
            }

            // 戻り値を持っている場合
            if (!resultType.isVoid()) {
                writer.write(")");
            }

            // 呼び出しを閉じる
            writer.write(");");

        }
        writer.popIndent(true);

        // 本文終了
        writer.writeLine("}");
    }

    /**
     * メソッドを作成するための参考になるコードを出力する
     */
    private void generateMethodStubs(CodeWriter writer) {

        writer.newLine();
        writer.writeLine("#if 0 /* stub! */");
        writer.writeLine("#include \"${CppHeaderGenerator.getHeaderName(wrapper)}\"");
        writer.writeLine('extern "C" {');

        // メソッド数だけ繰り返す
        for (def method : methods) {
            if (method.isNativeMethod()) {
                writer.newLine();

                writer.write("JNIEXPORT ${TypeUtil.getJniType(method.resultType.javaFullName, method.resultType.arrayDimension)} JNICALL Java_");
                // java classを"_"に変換する
                writer.write("${wrapper.javaFullName.replaceAll("\\.", "_")}_${method.name}(JNIEnv *env");

                // static?
                if (!method.isStaticMethod()) {
                    writer.write(", jobject _this");
                }

                // 引数一覧
                if (!method.argmentList.empty) {
                    for (def arg : method.argmentList) {
                        writer.write(", ${TypeUtil.getJniType(arg.classWrapper.javaFullName, arg.classWrapper.arrayDimension)} ${arg.name}");
                    }
                }

                // 閉じる
                writer.writeLine(") {")

                // 関数閉じる
                writer.writeLine("}");
            }
        }

        writer.newLine();
        writer.writeLine("}");
        writer.writeLine("#endif /* stub! */")
    }

    /**
     * Class識別子を取得する
     */
    public static String getClassSignature(JointClassWrapper wrapper) {
        return wrapper.javaFullName.replaceAll("\\.", "/");
    }

    /**
     * メソッド識別子を取得する
     */
    public static String getMethodSignature(JointMethodWrapper wrapper) {
        def result = wrapper.resultType;
        def args = wrapper.argmentList;

        // 引数リスト
        String sign = "(";
        for (def arg : args) {
            sign += TypeUtil.getJniClassSignature(arg.classWrapper.javaFullName, arg.classWrapper.arrayDimension);
        }
        sign += ")";

        // 戻り値
        sign += TypeUtil.getJniClassSignature(result.javaFullName, result.arrayDimension);

        return sign;
    }
}