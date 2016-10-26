package com.eaglesakura.jc.gradle

import com.eaglesakura.jc.gradle.tasks.ExportTask
import org.gradle.testfixtures.ProjectBuilder

public class ExportTaskTest extends GroovyTestCase {
    public void testExport() {
        def project = ProjectBuilder.builder().build();
        def task = (ExportTask) project.task("exportTask", type: ExportTask);

        File root = new File("sample/app");
        task.cppGeneratePath = new File("build/joint-connector/generated");
//        task.buildFlavor = "googlePlay"
//        task.outLogLevel = -1;
        task.projectPath = root;
        task.tempGeneratePath = new File(root, "build/joint-connector/jnibuild.jointclasses");
        task.dex2jarCommand = System.getenv("DEX2JAR_PATH")

        task.cppGenerate();
    }
}