package com.eaglesakura.jc.parser.dex

import com.eaglesakura.util.IOUtil
import com.eaglesakura.tool.log.Logger
import com.eaglesakura.util.StringUtil

/**
 * Pre-Dexedディレクトリに格納されているdexファイルをデコンパイルして特定ディレクトリに格納する
 */
public class PreDexedJarDecompiler {

    /**
     * ${android-project}/buildディレクトリ
     */
    File androidBuildDirectory;

    /**
     * pre-dexed配下のディレクトリ
     */
    File preDexedDirectory;

    File decompileOutDirectory;

    String dex2jarCommand = "";

    public PreDexedJarDecompiler() {

    }

    /**
     * デコンパイル結果を格納するディレクトリ名を取得する
     * @return
     */
    void setDecompileOutDirectory(String flavor, String buildType) {
        String dirName = "joint-connector/pre-dexed-";

        if (!StringUtil.isEmpty(flavor)) {
            dirName += flavor;
        }
        if (!StringUtil.isEmpty(buildType)) {
            dirName += buildType;
        }
        if (dirName.endsWith("-")) {
            dirName += "default";
        }

        decompileOutDirectory = new File(androidBuildDirectory, dirName);
    }

    /**
     * decompileを行う
     */
    public void execute() {
        decompileOutDirectory.mkdirs();
        def files = preDexedDirectory.listFiles();
        String commandName = "dex2jar.sh  "
        if (System.properties['os.name'].toLowerCase().contains('windows')) {
            // for windows
            commandName = "dex2jar.bat"
        }

        // コマンドが指定されていればそれを使用する
        if (!StringUtil.isEmpty(dex2jarCommand)) {
            commandName = dex2jarCommand;
        }

        Logger.out "dex2jar -> ${commandName}"
//        commandName = "/dev-home/scripts/dex2jar/dex2jar.sh"

        try {
            for (def dexjar : files) {

                def jarFileNmae = "${IOUtil.getFileName(dexjar.name)}_dex2jar.jar";
                def jarFile = new File(dexjar.parentFile, jarFileNmae);
                def dstFile = new File(decompileOutDirectory, jarFileNmae);

                if (!dstFile.exists()) {
                    Logger.out([commandName, dexjar.absolutePath].execute().text)
                    jarFile.renameTo(dstFile);
                } else {
                    Logger.out "dex2jar has cache(${dexjar.absolutePath})"
                }
            }
        } catch (Exception e) {
//            Logger.out e.message;
            Logger.out 'please install & setup dex2jar(https://code.google.com/p/dex2jar/)'
        }
    }
}