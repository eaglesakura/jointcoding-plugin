package com.eaglesakura.jc.generator.wrapper

import com.eaglesakura.jc.parser.model.Constants
import com.eaglesakura.jc.parser.model.JointModel
import com.eaglesakura.jc.parser.model.TypeUtil
import com.eaglesakura.util.StringUtil

class JointFieldWrapper {
    /**
     * フィールドの型
     */
    JointClassWrapper classWrapper;

    /**
     * フィールドの値
     */
    String value;

    /**
     * フィールド名
     */
    String name;

    JointModel.Field field;

    boolean interfaceField = false;

    public JointFieldWrapper() {

    }

    public JointFieldWrapper(JointClassWrapper clazz, JointModel.Field field) {
        interfaceField = clazz.isInterface();
        this.field = field;

        this.classWrapper = ClassesCache.get(field.fieldType);
        if (field.hasValue()) {
            this.value = field.value;
        }

        if (field.hasFieldName()) {
            this.name = field.fieldName;
        }
    }

    /**
     * 出力対象である場合はtrueを返す
     */
    public boolean isGenerateTarget() {
        return interfaceField || (field != null && field.fieldAttributesList.contains(JointModel.Field.FieldAttribute.Export));
    }

    /**
     * staticフィールドである場合はtrue
     */
    public boolean isStaticField() {
        return field != null && field.fieldAttributesList.contains(JointModel.Field.FieldAttribute.Static);
    }

    /**
     * 定数フィールドとして出力する場合はtrue
     */
    public boolean isConstantField() {
        if (!isGenerateTarget()) {
            return false;
        }

        // 変数名が無ければ出力対象にできない
        if (StringUtil.isEmpty(name)) {
            return false;
        }

        // Constnt属性がついていないなら定数フィールドにはなれない
        if (!field.fieldAttributesList.contains(JointModel.Field.FieldAttribute.Constant) || !field.fieldAttributesList.contains(JointModel.Field.FieldAttribute.Static)) {
            // export / constant / static全て揃わなければ出力不可能
            return false;
        }

        // string型は特別に許可する
        if (Constants.TYPENAME_LANG_STRING == classWrapper.javaFullName) {
            return true;
        }

        // JNIのプリミティブ型に変換できないなら定数に含まれない
        return TypeUtil.isJniPrimitiveType(classWrapper.javaFullName);
    }
}