package com.eaglesakura.jc.generator

import com.eaglesakura.jc.generator.cpp.CppHeaderGenerator
import com.eaglesakura.jc.generator.cpp.CppSourceGenerator
import com.eaglesakura.jc.generator.wrapper.ClassesCache

public class JointConnectorCppGenerator {
    /**
     * ビルド対象の*.jointclassesファイル
     */
    String jointClassesFile;

    /**
     * 出力先ディレクトリ
     */
    String jniGeneratePath;

    public JointConnectorCppGenerator() {
    }

    /**
     * 出力を行う
     */
    public void build() {
        // 既存ファイルを全てロード
        ClassesCache.loadPrimitives();
        ClassesCache.load(new File(jointClassesFile).absoluteFile);

        // 出力対象を取得
        def generateTargets = ClassesCache.listGenerateTargets();

        // 出力ディレクトリ
        File outDir = new File(jniGeneratePath).absoluteFile;

        // 全て出力
        for (def target : generateTargets) {
            def headerGen = new CppHeaderGenerator(target);
            headerGen.build(outDir);

            if (!target.interface) {
                // 本体を出力する
                def implGen = new CppSourceGenerator(target);
                implGen.build(outDir);
            } else {
                // interfaceであればstub出力を行わせる
                def stubHeaderGen = new CppHeaderGenerator(target);
                def stubImplGen = new CppSourceGenerator(target);

                stubHeaderGen.interfaceStubMode = true;
                stubImplGen.interfaceStubMode = true;

                stubImplGen.build(outDir);
                stubHeaderGen.build(outDir);

            }
        }
    }
}