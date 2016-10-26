package com.eaglesakura.jc.parser

import com.eaglesakura.jc.parser.dex.PreDexedJarDecompiler
import com.eaglesakura.jc.parser.importer.ClassFileImporter
import com.eaglesakura.jc.parser.pool.ParsedClassesPool
import com.eaglesakura.tool.log.Logger
import com.eaglesakura.util.StringUtil

/**
 * パーサーの実行本体
 */
public class JointConnectorParser {
    /**
     * ビルド対象のプロジェクト
     */
    String projectPath;

    public enum BuildTarget {
        debug,
        release,
    }

    /**
     * 検索対象のビルドバージョン
     *
     * debug/release等を指定する
     */
    BuildTarget buildTarget = BuildTarget.debug;

    /**
     * ビルド対象のフレーバー指定
     *
     * staging等のフレーバーが指定されている場合
     *
     * null可
     */
    String buildFlavor = null;

    /**
     * Android SDK
     *
     * APIバージョン
     */
    String apiVersion = "19";

    /**
     * 解析済みの一時ファイルを格納する先を指定する
     */
    String generateFilePath;

    /**
     * dex2jarコマンドを書き換える場合
     */
    String dex2jarCommand;

    String ANDROID_HOME;

    public JointConnectorParser() {
        ANDROID_HOME = System.getenv("ANDROID_HOME");
    }

    /**
     * ビルド対象のAndroidプロジェクトのRootを取得する
     *
     * @return
     */
    private File getAndroidProjectRoot() {
        return new File(projectPath).absoluteFile;
    }

    /**
     * Gradleのビルド成果物が配置されるディレクトリを取得する。
     *
     * これは "$androidProjectRoot/build" が基本的に指定される。
     */
    private File getGradleBuildDirectory() {
        return new File(androidProjectRoot, "build");
    }

    /**
     * 各ディレクトリ配下のサブディレクトリとなるパスを取得する
     */
    private String getTargetSubDirectoryPath() {
        if (StringUtil.isEmpty(buildFlavor)) {
            return buildTarget.name();
        } else {
            return "${buildFlavor}/${buildTarget.name()}";
        }
    }

    /**
     * 依存Jarが含まれているディレクトリを取得する
     */
    private File getDependenciesJarFileDirectory() {
        return new File(gradleBuildDirectory, "intermediates/pre-dexed/${targetSubDirectoryPath}").absoluteFile;
    }

    /**
     * Androidプロジェクトのコンパイル済みソースディレクトリを取得する
     */
    private File getAndroidCompiledClassesDirectory() {
        return new File(gradleBuildDirectory, "intermediates/classes/${targetSubDirectoryPath}").absoluteFile;
    }

    public void build() {
        // env ANDROID_HOME ディレクトリ
        Logger.out "project(${projectPath}), api(${apiVersion})";
        Logger.out "Environment Android SDK(${ANDROID_HOME})"

        PreDexedJarDecompiler decompiler = new PreDexedJarDecompiler();
        // pre-dexedを処理する
        PREDEXED:
        {
            decompiler.androidBuildDirectory = gradleBuildDirectory;
            decompiler.preDexedDirectory = dependenciesJarFileDirectory;
            decompiler.setDecompileOutDirectory(buildFlavor, buildTarget.name());
            decompiler.dex2jarCommand = dex2jarCommand;

            // dex -> *.class
            decompiler.execute();
        }

        ParsedClassesPool pool = new ParsedClassesPool();

        ClassFileImporter apiImporter;  // for Android API
        ClassFileImporter projectLibsImporter; // for Build Project /libs
        ClassFileImporter projectClassesImporter; // for Build Project /classes

        // APIをインポートする
        APIIMPORT:
        {
            apiImporter = new ClassFileImporter(new File("${ANDROID_HOME}/platforms/android-${apiVersion}/android.jar"));
            apiImporter.parse();
        }
        System.gc();

        // ビルドに必要なファイルをインポートする
        PROJECTIMPORT:
        {
            Logger.out "dependencies :: ${decompiler.decompileOutDirectory.absolutePath}"
            Logger.out "classes      :: ${androidCompiledClassesDirectory.absolutePath}"

            projectLibsImporter = new ClassFileImporter(decompiler.decompileOutDirectory);
            projectClassesImporter = new ClassFileImporter(androidCompiledClassesDirectory);

            projectLibsImporter.parse();
            projectClassesImporter.parse();
        }
        System.gc();

        // 各種ファイルを中間コードにビルドする
        BUILD_CLASSES:
        {
            pool.add(apiImporter.build());
            pool.add(projectLibsImporter.build());
            pool.add(projectClassesImporter.build());
        }
        System.gc();

        // 中間コードをバッファに変換する
        pool.build();

        // ファイルとして書き出す
        def outputPath = new File(generateFilePath).absoluteFile;
        outputPath.parentFile.mkdirs();
        pool.export(outputPath);

        // 結果出力
        Logger.out("parse complete classes(${pool.classesNum}) path(${outputPath.absolutePath}) ")
    }
}