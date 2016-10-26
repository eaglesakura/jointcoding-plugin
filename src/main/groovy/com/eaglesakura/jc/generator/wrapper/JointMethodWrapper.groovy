package com.eaglesakura.jc.generator.wrapper

import com.eaglesakura.jc.parser.model.JointModel

class JointMethodWrapper {
    JointClassWrapper clazz;

    JointModel.Method method;

    String methodUniqueId;

    JointMethodWrapper(JointClassWrapper clz, JointModel.Method method) {
        this.clazz = clz;
        this.method = method;

        methodUniqueId = "(${method.resultType})${method.name}";
        for (def arg : method.argsList) {
            methodUniqueId += "(${arg.argType})";
        }
        methodUniqueId += ";";
    }

    /**
     * 指定した属性を持っている場合はtrue
     * @param attribute
     * @return
     */
    boolean hasAttribute(JointModel.Method.MethodAttribute attribute) {
        for (def attr : method.getMethodAttributesList()) {
            if (attr == attribute) {
                // Exportを含んでいたら出力
                return true;
            }
        }
        return false;
    }

    /**
     * 出力対象である場合はtrue
     * @return
     */
    boolean isGenerateTarget() {
        return hasAttribute(JointModel.Method.MethodAttribute.Export);
    }

    String getName() {
        return method.name;
    }

    /**
     * staticメソッドの場合はtrue
     */
    boolean isStaticMethod() {
        return hasAttribute(JointModel.Method.MethodAttribute.Static);
    }

    /**
     * nativeメソッドの場合はtrue
     */
    boolean isNativeMethod() {
        return hasAttribute(JointModel.Method.MethodAttribute.Native);
    }

    /**
     * 戻り値の型を取得する
     */
    JointClassWrapper getResultType() {
        return ClassesCache.get(method.resultType);
    }

    /**
     * 引数の型一覧を取得する
     */
    List<JointFieldWrapper> getArgmentList() {
        def result = new ArrayList<JointFieldWrapper>();
        int argIndex = 0;
        for (def arg : method.argsList) {

            def field = new JointFieldWrapper();
            field.classWrapper = ClassesCache.get(arg.argType);
            if (arg.hasArgName()) {
                field.name = arg.argName;
            } else {
                // 引数名がわからないならばシンプルなネームを付ける
                field.name = "arg${argIndex}";
            }

            result.add(field);
            ++argIndex;
        }

        return result;
    }


    @Override
    public String toString() {
        return "JointMethodWrapper{" +
                "clazz=" + clazz +
                ", method=" + method +
                ", methodUniqueId='" + methodUniqueId + '\'' +
                '}';
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        JointMethodWrapper that = (JointMethodWrapper) o

        if (methodUniqueId != that.methodUniqueId) return false

        return true
    }

    int hashCode() {
        return (methodUniqueId != null ? methodUniqueId.hashCode() : 0)
    }
}