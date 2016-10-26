package com.eaglesakura.jc.generator.wrapper

import com.eaglesakura.util.IOUtil
import com.eaglesakura.jc.parser.model.Constants
import com.eaglesakura.jc.parser.model.JointModel
import com.eaglesakura.jc.parser.model.TypeUtil

/**
 * クラスのラッピングを行う
 */
class JointClassWrapper {
    /**
     * パッケージ名
     */
    String packageName;

    /**
     * クラス名
     */
    String className;

    /**
     * 変換済みクラス
     * プリミティブ型の場合は存在しない
     */
    private JointModel.Class clazz;

    /**
     * 配列の次元
     */
    int arrayDimension = 0;

    public JointClassWrapper(String name) {
        packageName = IOUtil.getFileName(name);
        className = IOUtil.getFileExt(name);
    }

    public JointClassWrapper(JointModel.Class clazz) {
        this.clazz = clazz;
        this.packageName = clazz.javaPackageName;
        this.className = clazz.javaClassName;
    }

    /**
     * C++出力時のnamespaceを持っている場合は返す
     * @return
     */
    String getCppNamespace() {
        if (clazz == null || !clazz.hasCppNamespace()) {
            return null;
        }

        return clazz.getCppNamespace();
    }

    boolean hasCppNamespace() {
        return getCppNamespace() != null;
    }

    /**
     * 指定したクラスが自分の系譜に存在すればtrue;
     */
    boolean hasGenealogy(JointClassWrapper check) {
        // check interface
        def interfaces = generateInterfaces;
        for (def ifs : interfaces) {
            if (ifs == check) {
                return true;
            }
        }

        // クラスの階層構造をチェックする
        def clz = this;
        while (clz != null) {
            if (clz == check) {
                return true;
            }

            // rootであればもうチェックの必要はない
            if (clz.isRootClass()) {
                return false;
            }

            clz = superClass;
        }

        return false;
    }

    /**
     * Java側のフル名を取得する
     * @return 　
     */
    String getJavaFullName() {
        return "${packageName}.${className}";
    }

    /**
     * 自身がrootであればtrue
     * @return
     */
    boolean isRootClass() {
        return this.javaFullName.equals(Constants.TYPENAME_LANG_ROOTCLASS);
    }

    /**
     * 出力対象の基底クラスを取得する
     *
     * 継承関係を正しくするため、上位の上位が仮にExport対象でもsuperがExport対象でなければGenerate対象とみなさない。
     * @return
     */
    JointClassWrapper getSuperClass() {
        if (clazz == null) {
            return null;
        }

        def spClass = ClassesCache.get(clazz.superClassName);

        if (spClass == null || !spClass.isGenerateTarget()) {
            // superが存在しない場合、root classを返す
            return ClassesCache.get(Constants.TYPENAME_LANG_ROOTCLASS);
        } else {
            // superが存在するなら、それを返す
            return spClass;
        }
    }

    /**
     * 実装しているインターフェース一覧を返す
     */
    List<JointClassWrapper> getGenerateInterfaces() {
        if (clazz == null) {
            return null;
        }

        def result = new ArrayList<JointClassWrapper>();

        for (def ifs : clazz.interfaceClassNamesList) {
            // インターフェース名から取得する
            def ifsWrapper = ClassesCache.get(ifs);
            // 取得成功し、かつインターフェースであれば対象に含める
            if (ifsWrapper != null && ifsWrapper.isInterface()) {
                result.add(ifsWrapper);
            }
        }

        return result;
    }

    /**
     * このクラスが独自に実装すべきインターフェース一覧を返す
     */
    List<JointClassWrapper> getAddedInterfaces() {
        if (superClass == null || !superClass.isGenerateTarget()) {
            // 親クラスに実装が無ければそのまま帰す
            return getGenerateInterfaces();
        }

        def selfInterfaces = generateInterfaces;
        def superInterfaces = superClass.generateInterfaces;

        selfInterfaces.removeAll(superInterfaces);
        return selfInterfaces;
    }

    /**
     * このクラスが独自に実装すべきインターフェース一覧を返す
     * @return
     */
    List<JointMethodWrapper> getAddedMethods() {
        if (superClass == null || !superClass.isGenerateTarget()) {
            // 親クラスに実装が無ければそのまま帰す
            return generateMethodList;
        }
        def selfMethods = generateMethodList;
        def superMethods = superClass.generateMethodList;

        selfMethods.removeAll(superMethods);

        return selfMethods;
    }

    /**
     * インターフェースとして出力する場合はtrue
     * @return
     */
    boolean isInterface() {
        if (!clazz) {
            return false;
        }

        // not exported以外は全て出力対象である
        return clazz.getType() == JointModel.Class.ClassType.Interface;
    }

    /**
     * 出力対応の場合はtrue
     * @return
     */
    boolean isGenerateTarget() {
        if (!clazz) {
            return false;
        }

        // not exported以外は全て出力対象である
        return clazz.getType() != JointModel.Class.ClassType.NotExported;
    }

    /**
     * 出力対象のメソッド一覧を取得する
     */
    List<JointMethodWrapper> getGenerateMethodList() {
        def result = new ArrayList<JointMethodWrapper>();

        for (def method : clazz.methodsList) {
            JointMethodWrapper wrapper = new JointMethodWrapper(this, method);
            if (wrapper.isGenerateTarget()) {
                result.add(wrapper);
            }
        }

        return result;
    }

    /**
     * 出力対象のフィールド一覧を取得する
     */
    List<JointFieldWrapper> getGenerateFieldList() {
        def result = new ArrayList<JointFieldWrapper>();

        for (def field : clazz.fieldsList) {
            JointFieldWrapper wrapper = new JointFieldWrapper(this, field);
            if (wrapper.isGenerateTarget()) {
                result.add(wrapper);
            }
        }

        return result;
    }

    /**
     * このクラスが実装すべきフィールド一覧を取得する
     */
    List<JointFieldWrapper> getAddedFields() {
        if (superClass == null || !superClass.isGenerateTarget()) {
            return generateFieldList;
        }

        def selfFields = generateFieldList;
        def superFields = superClass.generateFieldList;

        selfFields.removeAll(superFields);

        return selfFields;
    }

    /**
     * このクラスが実装しているフィールド一覧を取得する
     */
    List<JointFieldWrapper> getAddedConstantFields() {
        def fields = addedFields;
        def itr = fields.iterator();

        // 定数フィールド以外を取り除く
        while (itr.hasNext()) {
            def wrapper = itr.next();
            if (!wrapper.isConstantField()) {
                itr.remove();
            }
        }

        return fields;
    }

    /**
     * このクラスが実装している可変フィールド一覧を取得する
     */
    List<JointFieldWrapper> getAddedVariableFields() {
        def fields = addedFields;
        def itr = fields.iterator();

        // 定数フィールドを取り除く
        while (itr.hasNext()) {
            def wrapper = itr.next();
            if (wrapper.isConstantField()) {
                itr.remove();
            }
        }

        return fields;
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        JointClassWrapper that = (JointClassWrapper) o

        if (className != that.className) return false
        if (packageName != that.packageName) return false

        return true
    }

    int hashCode() {
        int result
        result = (packageName != null ? packageName.hashCode() : 0)
        result = 31 * result + (className != null ? className.hashCode() : 0)
        return result
    }

    /**
     * jobject/jstring/jclass以外であればtrue
     * @return
     */
    boolean isJniPrimitive() {
        return TypeUtil.isJniPrimitiveType(javaFullName);
    }

    /**
     * void型である場合はtrue
     * @return
     */
    boolean isVoid() {
        return javaFullName.equals(Constants.TYPENAME_LANG_VOID);
    }

    /**
     * 配列であればtrue
     * @return
     */
    boolean isArray() {
        return arrayDimension > 0;
    }

    /**
     * 複数次元配列であればtrue
     * @return
     */
    boolean isMultiArray() {
        return arrayDimension > 1;
    }

    /**
     * 配列の次元を指定して新たに生成する。
     */
    JointClassWrapper newClassWrapper(int arrayDimensions) {
        JointClassWrapper result;
        if (clazz != null) {
            result = new JointClassWrapper(clazz);
        } else {
            result = new JointClassWrapper(this.javaFullName);
        }

        result.arrayDimension = arrayDimensions;
        return result;
    }
}