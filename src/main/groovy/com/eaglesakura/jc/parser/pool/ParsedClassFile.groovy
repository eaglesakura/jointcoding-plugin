package com.eaglesakura.jc.parser.pool

import com.eaglesakura.util.IOUtil
import com.eaglesakura.jc.annotation.JCClass
import com.eaglesakura.jc.annotation.JCField
import com.eaglesakura.jc.annotation.JCMethod
import com.eaglesakura.jc.parser.importer.AnnotationArgment
import com.eaglesakura.jc.parser.model.JointModel
import com.eaglesakura.jc.parser.model.TypeUtil
import com.eaglesakura.tool.log.Logger
import com.eaglesakura.util.StringUtil
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import javassist.Modifier
import javassist.bytecode.CodeAttribute
import javassist.bytecode.LocalVariableAttribute

/**
 * *.classオブジェクトのparseを行う
 */
public class ParsedClassFile {

    /**
     * ビルド対象のクラス
     */
    private CtClass targetClazz;

    /**
     * ビルダー作成
     */
    JointModel.Class.Builder classBuilder = JointModel.Class.newBuilder();


    public ParsedClassFile(CtClass clazz) {
        this.targetClazz = clazz;
    }

    /**
     * クラスから型名を取得する
     * @param origin
     * @return
     */
    String getTypeName(CtClass origin) {
        return TypeUtil.convertToJointType(origin.getName());
    }

    /**
     * フィールドを読み込む
     */
    private void buildFields() {
        def classFields = targetClazz.getFields();
        if (classFields.length == 0) {
            Logger.out("field not found...");
            return;
        }

        Logger.out("parse fields(${classFields.length})");
        try {
            Logger.pushIndent(); // ログ開始
            for (CtField field : classFields) {

                try {
                    JointModel.Field.Builder builder = JointModel.Field.newBuilder();

                    boolean attrExport = false;
                    boolean attrStatic = false;
                    boolean attrConstant = false;

                    final Object jcfield = getAnnotation(field, JCField.class);
                    if (jcfield != null) {
                        // annotationが設定されていたらエクスポートを行う
                        builder.addFieldAttributes(JointModel.Field.FieldAttribute.Export);

                        attrExport = true;
                    }

                    // static属性？
                    if (Modifier.isStatic(field.getModifiers())) {
                        builder.addFieldAttributes(JointModel.Field.FieldAttribute.Static);

                        attrStatic = true;
                    }

                    // constant属性
                    if (Modifier.isFinal(field.getModifiers())) {
                        builder.addFieldAttributes(JointModel.Field.FieldAttribute.Constant);

                        attrConstant = true;
                    }

                    // フィールド名
                    builder.setFieldName(field.getName());

                    // 型名
                    builder.setFieldType(getTypeName(field.getType()));

                    // 値
                    builder.setValue(field.getConstantValue().toString());

                    // 登録
                    classBuilder.addFields(builder);

                    // ログ
                    Logger.out("name(${builder.getName()}) type(${builder.getType()}) export(${attrExport}) static(${attrStatic}) const(${attrConstant})");

                } catch (Exception e) {
//                    LogUtil.log(e);
                    Logger.out("  :: field export failed :: " + field.getName())
                }
            }
        } finally {
            Logger.popIndent(); // ログ終了
        }

        Logger.out("parse fields(${classFields.length}) completed");
    }

    /**
     * メソッド単体を解析する
     * @param method
     */
    private void buildMethod(CtMethod method) {
        JointModel.Method.Builder methodBuilder = JointModel.Method.newBuilder();

        boolean attrExport = false;
        boolean attrStatic = false;
        boolean attrNative = false;
        boolean attPublic = false;

        final Object jcmethod = getAnnotation(method, JCMethod.class);
        if (jcmethod != null) {
            // annotationが設定されていたらエクスポートを行う
            methodBuilder.addMethodAttributes(JointModel.Method.MethodAttribute.Export);

            attrExport = true;
        }

        // static属性？
        if (Modifier.isStatic(method.getModifiers())) {
            methodBuilder.addMethodAttributes(JointModel.Method.MethodAttribute.Static);

            attrStatic = true;
        }

        // public属性
        if (Modifier.isPublic(method.getModifiers())) {
            methodBuilder.addMethodAttributes(JointModel.Method.MethodAttribute.Public);

            attPublic = true;
        }

        if (Modifier.isNative(method.getModifiers())) {
            methodBuilder.addMethodAttributes(JointModel.Method.MethodAttribute.Native);

            attrNative = true;
        }

        // メソッド名
        methodBuilder.setName(method.getName());

        // 戻り型
        methodBuilder.setResultType(getTypeName(method.getReturnType()));

        // 引数解析
        try {
            Logger.pushIndent();

            def parameterTypes = method.getParameterTypes();

            CodeAttribute code = (CodeAttribute) method.getMethodInfo().getAttribute("Code");
            LocalVariableAttribute lval = null;
            if (code != null) {
                lval = (LocalVariableAttribute) code.getAttribute("LocalVariableTable");
            }
            for (int i = 0; i < parameterTypes.length; ++i) {
                int index = attrStatic ? i : (i + 1);
                String argName = "";

                // 引数名の確定
                if (lval != null) {
                    argName = lval.getConstPool().getUtf8Info(lval.nameIndex(index));
                }

                JointModel.Method.Argment.Builder argBuilder = JointModel.Method.Argment.newBuilder();
                // 型
                argBuilder.setArgType(getTypeName(parameterTypes[i]));
                if (!StringUtil.isEmpty(argName)) {
                    argBuilder.setArgName(argName);
                }

                Logger.out("arg(${i + 1}) type(${argBuilder.argType}) name(${argBuilder.argName})");

                methodBuilder.addArgs(argBuilder);
            }

            // ログ
            Logger.out("method export :: ${attPublic ? "public" : ""} result(${methodBuilder.getResultType()}) name(${methodBuilder.getName()}) export(${attrExport}) static(${attrStatic}) native(${attrNative})");

        } finally {
            Logger.popIndent();
        }

        // 登録
        classBuilder.addMethods(methodBuilder);

    }

    /**
     * メソッドの変換を行う
     */
    private void buildMethods() {
        def classMethods = targetClazz.getMethods();

        Logger.out("parse methods(${classMethods.length})");
        try {
            Logger.pushIndent(); // ログ開始
            for (CtMethod method : classMethods) {
                try {
                    buildMethod(method);
                } catch (Exception e) {
//                    LogUtil.log(e);
                    Logger.out("  -- method export failed :: " + method.getName())
                }
            }
        } finally {
            Logger.popIndent(); // ログ終了
        }

        Logger.out("parse methods(${classMethods.length}) completed");
    }

    /**
     * Classの属性変換を行う
     */
    private void buildClass() {
        String tempFullName = TypeUtil.convertToJointType(targetClazz.getName());
        classBuilder.setJavaPackageName(IOUtil.getFileName(tempFullName));
        classBuilder.setJavaClassName(IOUtil.getFileExt(tempFullName));
        try {
            classBuilder.setSuperClassName(getTypeName(targetClazz.getSuperclass()));
        } catch (Exception e) {
            classBuilder.setSuperClassName(TypeUtil.convertToJointType(Object.getClass().getName()));
        }
        try {
            def interfaces = targetClazz.getInterfaces()
            for (CtClass impl : interfaces) {
                // 実装しているインターフェースを追加
                classBuilder.addInterfaceClassNames(getTypeName(impl));
            }
        } catch (Exception e) {
            Logger.out("interface parse error :: ${targetClazz.getName()}");
        }

        Logger.out("package(${classBuilder.getJavaPackageName()}) class(${classBuilder.getJavaClassName()}) super(${classBuilder.getSuperClassName()})")

        Object jcclass = getAnnotation(targetClazz, JCClass.class);

        if (jcclass != null) {
            if (targetClazz.isInterface()) {
                classBuilder.setType(JointModel.Class.ClassType.Interface);
            } else {
                classBuilder.setType(JointModel.Class.ClassType.Class);
            }

            try {
                Logger.pushIndent();

                AnnotationArgment argment = new AnnotationArgment(jcclass);

                String cppNamespace = argment.getArgment("cppNamespace", "");
                String cppClassname = argment.getArgment("cppClassname", "");

                if (!StringUtil.isEmpty(cppNamespace)) {
                    classBuilder.setCppNamespace(cppNamespace);
                }

                if (!StringUtil.isEmpty(cppClassname)) {
                    classBuilder.setCppClassname(cppClassname);
                }

                Logger.out("annotation cppNamespace(${cppNamespace}) cppClassname(${cppClassname})");
            } finally {
                Logger.popIndent();
            }
        } else {
            classBuilder.setType(JointModel.Class.ClassType.NotExported);
        }
    }

    /**
     * パース済みのClassを取得する
     * @return
     */
    public JointModel.Class getParsedClass() {
        return classBuilder.build();
    }

    /**
     * ビルドを行う
     */
    public void build() {
        Logger.out("build class ${targetClazz.getName()}");
        try {
            Logger.pushIndent();

            BUILD:
            {
                // クラスを生成
                buildClass();

                // メソッドを生成
                buildMethods();

                // フィールドを生成
                buildFields();
            }
        } finally {

            Logger.popIndent();
        }
    }

    @Override
    public int hashCode() {
        return targetClazz.getName().hashCode();
    }

    @Override
    boolean equals(o) {
        try {

            ParsedClassFile otherParser = (ParsedClassFile) o;
            ParsedClassFile selfParser = this;

            return selfParser.targetClazz.getName().equals(otherParser.targetClazz.getName());
        } catch (Exception e) {
            return false;
        }
    }

    public static Object getAnnotation(CtClass clazz, Class<?> clz) {
        try {
            final String HEADER = "@" + clz.getName();
            Object[] annotations = clazz.getAnnotations();
            for (Object anno : annotations) {
                String str = anno.toString();
                if (str.startsWith(HEADER)) {
                    return anno;
                }
            }
        } catch (Exception e) {
            //            e.printStackTrace();
        }
        return null;
    }

    public static Object getAnnotation(CtField clazz, Class<?> clz) {
        try {
            final String HEADER = "@" + clz.getName();
            Object[] annotations = clazz.getAnnotations();
            for (Object anno : annotations) {
                String str = anno.toString();
                if (str.startsWith(HEADER)) {
                    return anno;
                }
            }
        } catch (Exception e) {
            //            e.printStackTrace();
        }
        return null;
    }

    public static Object getAnnotation(CtMethod clazz, Class<?> clz) {
        try {
            final String HEADER = "@" + clz.getName();
            Object[] annotations = clazz.getAnnotations();
            for (Object anno : annotations) {
                String str = anno.toString();
                if (str.startsWith(HEADER)) {
                    return anno;
                }
            }
        } catch (Exception e) {
            //            e.printStackTrace();
        }
        return null;
    }
}