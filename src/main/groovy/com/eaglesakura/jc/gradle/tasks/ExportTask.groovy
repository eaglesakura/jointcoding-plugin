package com.eaglesakura.jc.gradle.tasks

import com.eaglesakura.jc.generator.JointConnectorCppGenerator
import com.eaglesakura.jc.parser.JointConnectorParser
import com.eaglesakura.tool.log.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ExportTask extends DefaultTask {

    /**
     * プロジェクトパス
     */
    String projectPath = new File('.').absolutePath;

    /**
     * debug/release
     */
    JointConnectorParser.BuildTarget buildTarget = JointConnectorParser.BuildTarget.debug;

    /**
     * 出力対象のフレーバー
     */
    String buildFlavor = "";

    /**
     * APIバージョン
     */
    String apiVersion = "19";

    /**
     * 出力対象パス
     */
    String tempGeneratePath = new File('build/joint-connector/build.jointclasses').absolutePath;

    /**
     * C++コード出力パス
     */
    String cppGeneratePath = new File('src/main/jni-jcgen').absolutePath;

    String dex2jarCommand = "";

    /**
     * CPPヘッダをコピーする場合はtrue
     */
    boolean exportCppHeader = true;

    int outLogLevel = 0;

    @TaskAction
    def cppGenerate() {
        Logger.initialize();
        if (outLogLevel >= 0) {
            Logger.outLogLevel = outLogLevel;
        }

        // copy JointConnector.hpp
        if (exportCppHeader) {
            def headerFileName = "JointConnector.hpp";
            def url = Thread.currentThread().getContextClassLoader().getResource(headerFileName);
//            assert url != null

            if (url != null) {
                def header = new File(new File(cppGeneratePath), headerFileName);
                new File(cppGeneratePath).mkdirs();
                header.write(url.getText());
            }
        }


        Logger.out "projectPath(${projectPath})";
        Logger.out "cppGeneratePath(${cppGeneratePath})"

        Logger.out "buildTarget(${buildTarget})";
        Logger.out "buildFlavor(${buildFlavor})";
        Logger.out "apiVersion(${apiVersion})";
        Logger.out "tempGeneratePath(${tempGeneratePath})";

        PARSE:
        {
            JointConnectorParser parser = new JointConnectorParser();

            // 各種設定
            parser.projectPath = projectPath;
            parser.buildTarget = buildTarget;
            parser.buildFlavor = buildFlavor;
            parser.apiVersion = apiVersion;
            parser.generateFilePath = tempGeneratePath;
            parser.dex2jarCommand = dex2jarCommand;

            parser.build();
        }

        GENERATE:
        {
            JointConnectorCppGenerator generator = new JointConnectorCppGenerator();
            generator.jniGeneratePath = cppGeneratePath;
            generator.jointClassesFile = tempGeneratePath;

            generator.build();
        }
    }
}
