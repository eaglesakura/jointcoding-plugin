package com.eaglesakura.jc.generator.cpp

import com.eaglesakura.jc.generator.wrapper.JointClassWrapper
import com.eaglesakura.jc.generator.wrapper.JointFieldWrapper
import com.eaglesakura.jc.generator.wrapper.JointMethodWrapper
import com.eaglesakura.jc.parser.model.Constants
import com.eaglesakura.jc.parser.model.TypeUtil
import com.eaglesakura.tool.log.Logger
import com.eaglesakura.tool.generator.CodeWriter
import com.eaglesakura.util.StringUtil
import com.eaglesakura.util.Util

/**
 * ヘッダ出力を行う
 */
public class CppHeaderGenerator {
    /**
     * jobjectのラップを行うクラス
     */
    static final String OBJECT_WRAPPER = "::jc::lang::object_wrapper";

    /**
     * ラッパクラスの変数名
     */
    static final String OBJECT_WRAPPER_VAR = "mObject";

    /**
     * 出力対象のクラス
     */
    JointClassWrapper wrapper;

    /**
     * 出力するクラス名
     */
    private String wrapperClassName;

    /**
     * wrapperがインターフェースであればtrue
     */
    private boolean wrapperIsInterface;

    /**
     * 実装すべき親クラス
     */
    JointClassWrapper superClass;

    /**
     * 実装すべきインターフェース
     */
    List<JointClassWrapper> implInterfaces = new ArrayList<JointClassWrapper>();

    /**
     * 実装すべきメソッド
     */
    List<JointMethodWrapper> implMethods = new ArrayList<>();

    /**
     * 実装すべき定数フィールド
     */
    List<JointFieldWrapper> implConstants = new ArrayList<>();

    /**
     * 実装すべきフィールド
     */
    List<JointFieldWrapper> implVariables = new ArrayList<>();

    /**
     * インターフェースStubを生成する場合はtrue
     */
    boolean interfaceStubMode = false;


    public CppHeaderGenerator(JointClassWrapper wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * ファイルへ書き込みを行う
     * @param directory
     */
    public void build(File directory) {

        if (interfaceStubMode && wrapper.interface) {
            // Stub出力モード
            wrapperClassName = "${wrapper.className}Stub";
            wrapperIsInterface = false;
        } else {
            wrapperClassName = wrapper.className;
            wrapperIsInterface = wrapper.interface;
        }

        superClass = wrapper.getSuperClass();
        implInterfaces = wrapper.addedInterfaces;
        if (superClass != null && superClass.isRootClass()) {
            // rootであればSuperは無いものとする
            superClass = null;
        }

        OUT_INFO:
        {
            Logger.out("build class ${wrapper.javaFullName} -> ${wrapperClassName}");
            if (superClass != null) {
                Logger.out("    extends ${superClass.javaFullName}");
            }
            Logger.pushIndent();
            CONSTANTS:
            {
                implConstants = wrapper.addedConstantFields;
                implVariables = wrapper.addedVariableFields;

                for (def field : implConstants) {
                    Logger.out("constant :: ${field.name}");
                }

                for (def field : implVariables) {
                    Logger.out("variable :: ${field.name}");
                }
            }
            METHODS:
            {
                implMethods = wrapper.addedMethods;
                for (def method : implMethods) {
                    Logger.out("method :: ${method.name}");
                }
            }
            Logger.popIndent();
        }




        Logger.pushIndent();
        Logger.pushIndent();

        // ヘッダ出力
        HEADER:
        {
            String headerFileName = CppHeaderGenerator.getHeaderName(wrapper);
            if (interfaceStubMode) {
                headerFileName = headerFileName.replace(".h", "Stub.h");
            }
            File out = new File(directory, headerFileName);
            CodeWriter writer = new CodeWriter(out);

            // ヘッダの出力を行わせる
            writeHeader(writer);

            writer.commit();
        }
        Logger.popIndent();
        Logger.popIndent();
    }

    /**
     * ヘッダの出力を行う
     * @param writer
     */
    private void writeHeader(CodeWriter writer) {

        // 多重インクルード防止を行う
        String includeGuard = getHeaderName(wrapper);
        includeGuard = includeGuard.replaceAll("\\.", "_");
        if (interfaceStubMode) {
            includeGuard += "_STUB";
        }
        includeGuard = "__${includeGuard.toUpperCase()}__";

        writer.writeLine("#ifndef ${includeGuard}");
        writer.writeLine("#define ${includeGuard}");

        // 可読性のために１行あける
        writer.newLine();

        WRITE_HEADER:
        {
            // include部分を出力する
            generateIncludes(writer);

            // namespace内を出力する
            generateNamespaceBlock(writer);
        }

        // 可読性のために１行あける
        writer.newLine();
        // インクルード防止の終了
        writer.writeLine("#endif // ${includeGuard}");
    }

    /**
     * #include部位を出力する
     */
    private void generateIncludes(CodeWriter writer) {
        writer.write('#include  "JointConnector.hpp"').newLine(); // default include

        if (superClass != null) {
            // rootでなければsuperも出力を行う
            writer.write("#include  \"${getHeaderName(superClass)}\"").newLine();
        }


        if (interfaceStubMode) {
            // stubを吐き出す
            writer.write("#include  \"${getHeaderName(wrapper)}\"").newLine();
        }

        for (def ifs : implInterfaces) {
            // 実装しているinterfaceも継承を行う
            writer.write("#include  \"${getHeaderName(ifs)}\"").newLine();
        }

        // ヘッダ部終了
        writer.newLine();
    }

    /**
     * ネームスペース内を出力する
     */
    private void generateNamespaceBlock(CodeWriter writer) {
        def namespaces = getNamespaces(wrapper);

        for (def name : namespaces) {
            writer.writeLine("namespace ${name} {")
        }
        // 可読性のために１行あける
        writer.newLine();

        // クラスブロックを出力する
        generateClassBlock(writer);

        // 可読性のために１行あける
        writer.newLine();
        for (def name : namespaces) {
            writer.writeLine("}")
        }
    }

    /**
     * ヘッダブロックの出力を行う
     * @param writer
     */
    private void generateClassBlock(CodeWriter writer) {
        writer.write("class ${wrapperClassName}");

        // 出力された基底クラスが存在するならtrue
        boolean hasGeneratedSuperClass = false;
        def generatedSuperClassName = "";
        // super?
        if (interfaceStubMode) {
            // Stubモード出力
            writer.write(" : public ${wrapper.className}");

        } else {
            // 通常モード出力
            if (!wrapper.isInterface()) {
                boolean superClassWrited = false;
                // 直接の継承チェック
                if (superClass == null) {
                    // Rootであれば何らかのオブジェクトが必要になる
                    // デフォルトでは何もしない
                } else {
                    generatedSuperClassName = getCppFullName(superClass, true);
                    writer.write(" : public ${generatedSuperClassName}");
                    hasGeneratedSuperClass = true;
                    superClassWrited = true;
                }

                // インターフェースの継承を行う
                if (!implInterfaces.empty) {
                    if (!superClassWrited) {
                        writer.write(" : ");
                    }

                    for (def ifs : implInterfaces) {
                        if (superClassWrited) {
                            writer.write(", ");
                        }
                        writer.write(" public ${getCppFullName(ifs, true)}");

                        superClassWrited = true;
                    }
                }
            }
        }
        writer.writeLine(" {");

        // 基底クラスを持たない場合、jobject保持クラスを追加する
        if (interfaceStubMode || (!hasGeneratedSuperClass && !wrapper.isInterface())) {
            // superが無いため、jobject管理用の変数を追加する
            writer.writeLine("protected:");
            writer.pushIndent(true);
            writer.write("${OBJECT_WRAPPER} ${OBJECT_WRAPPER_VAR};");
            writer.popIndent(true);
        }

        writer.writeLine("public:");
        writer.pushIndent(true);

        // 標準メソッド出力
        DEF_METHODS:
        {
            // インターフェースではコンストラクタは不要となる
            // ただし、stubは出力する
            if (!wrapper.isInterface() || interfaceStubMode) {
                writer.writeLine("${wrapperClassName}(){}")

                // コンストラクタ
                if (hasGeneratedSuperClass) {
                    // superに任せる
                    writer.writeLine("${wrapperClassName}(${OBJECT_WRAPPER} obj) : ${generatedSuperClassName}(obj) {}")
                    writer.writeLine("${wrapperClassName}(jobject obj, JNIEnv *env = nullptr, bool newLocalRefFlag = true) : ${generatedSuperClassName}(obj, env, newLocalRefFlag) {}")
                } else {
                    // コンストラクタを生成する
                    writer.writeLine("${wrapperClassName}(${OBJECT_WRAPPER} obj){ this->${OBJECT_WRAPPER_VAR} = obj; }")
                    writer.writeLine("${wrapperClassName}(jobject obj, JNIEnv *env = nullptr, bool newLocalRefFlag = true){ this->${OBJECT_WRAPPER_VAR} = ${OBJECT_WRAPPER}(obj, env, newLocalRefFlag); }")
                }
            }

            // 空のデストラクタ
            writer.newLine();
            writer.writeLine("virtual ~${wrapperClassName}(){}");
            writer.newLine();

            // インターフェース、もしくは基底クラスがある場合、アクセサは不要となる
            if (!wrapper.isInterface() && !hasGeneratedSuperClass) {
                // デフォルトのアクセサ
                writer.writeLine("${OBJECT_WRAPPER} getWrapperObject() const { return ${OBJECT_WRAPPER_VAR}; }");
            }
        }

        writer.writeLine("/* Constant Fields */");
        // 定数フィールド出力
        CONSTANT_FIELDS:
        {
            for (def field : implConstants) {
                if (TypeUtil.isJniPrimitiveType(field.classWrapper.javaFullName)) {
                    // プリミティブならば、それを直接出力する
                    writer.writeLine("static constexpr ${getCppFullName(field.classWrapper, false)} ${field.name} = ${field.value};")
                } else {
                    // string型として出力する
                    writer.writeLine("static constexpr const char* ${field.name} = \"${field.value}\";")
                }
            }
        }

        // 通常フィールド出力
        writer.writeLine("/* Fields */");
        VAR_FIELDS:
        {
            for (def field : implVariables) {

                if (field.isStaticField()) {
                    writer.write("static ");
                }

                def typeName = getCppFullName(field.classWrapper, false);
                // setter/getterを通してアクセスする
                writer.writeLine("${typeName} field_${field.name}();")
                writer.writeLine("void field_${field.name}(${typeName} set);")
            }
        }

        // 出力対象メソッド
        writer.writeLine("/* Methods */");
        METHODS:
        {
            for (def methodWrapper : implMethods) {
                generateMethod(writer, methodWrapper);
                writer.newLine();
            }
        }

        writer.popIndent(true);
        // 終端
        writer.writeLine("};");

    }

    /**
     * メソッド出力を行う
     */
    private void generateMethod(CodeWriter writer, JointMethodWrapper method) {
        def methodHeader = method.isStaticMethod() ? "static" : "virtual";
        def methodFooder = wrapper.isInterface() ? " = 0" : "";

        if (interfaceStubMode) {
            // stubの場合は実装をリセット
            methodFooder = "";
        }

        SIMPLE:
        {
            // 直接出力を行う
            writer.write("${methodHeader} ${getCppFullName(method.getResultType(), false)} ${method.name} (")

            // 引数リスト
            def argmetns = method.argmentList;
            int index = 0;
            for (def arg : argmetns) {
                if (index != 0) {
                    writer.write(", ");
                }

                writer.write("${getCppFullName(arg.classWrapper, false)} ${arg.name}");
                ++index;
            }

            // 閉じる
            writer.write(")${methodFooder};");
            writer.newLine();
        }

        // TODO 戻り値のクラス名が自分の系譜にあるならば、safetyなメソッドを組み入れたい
    }

    /**
     * ヘッダ名を取得する
     */
    public static String getHeaderName(JointClassWrapper wrapper) {
        String nameSpace = wrapper.getCppNamespace();
        if (StringUtil.isEmpty(nameSpace)) {
            nameSpace = "";
        }

        nameSpace = nameSpace.replaceAll("\\.", "_");
        return "${nameSpace}_${wrapper.className}.h";
    }

    /**
     * ネームスペースを"."区切りで取得する
     * @param wrapper
     * @return
     */
    public static List<String> getNamespaces(JointClassWrapper wrapper) {
        String nameSpace = wrapper.getCppNamespace();

        def result = new ArrayList<String>();

        if (StringUtil.isEmpty(nameSpace)) {
            return result;
        }

        return Util.convert(nameSpace.split("\\."));
    }

    /**
     * C++の全名称を取得する。フルアクセス名になる。
     * @param wrapper
     * @param isAccruacy 正確に出力する場合はtrue, falseの場合はプリミティブ型以外を強制的にobject_wrapperとして返す。
     * @return
     */
    public static String getCppFullName(JointClassWrapper wrapper, boolean isAccruacy) {
        if (wrapper == null) {
            return null;
        }

        if (!isAccruacy && wrapper.arrayDimension > 0) {
            // 配列の場合、強制的にjobjectとなる
            return OBJECT_WRAPPER;
        }

        def javaFullName = wrapper.javaFullName;
        if (!StringUtil.isEmpty(wrapper.cppNamespace)) {
            javaFullName = "${wrapper.cppNamespace}.${wrapper.className}";
        }

        if (javaFullName.equals(Constants.TYPENAME_LANG_ROOTCLASS)) {
            return OBJECT_WRAPPER;
        }
        // voidはそのままvoid
        if (javaFullName.equals(Constants.TYPENAME_LANG_VOID)) {
            return "void";
        }

        String temp = javaFullName.replaceAll("\\.", "::");
        // joint typeの場合は"_wrapper"を付ける
        if (TypeUtil.isJointType(javaFullName)) {
            return "::${temp}_wrapper";
        } else {
            if (isAccruacy) {
                return "::${temp}";
            } else {
                return OBJECT_WRAPPER;
            }
        }

    }
}