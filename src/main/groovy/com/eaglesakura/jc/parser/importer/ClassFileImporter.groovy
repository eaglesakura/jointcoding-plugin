package com.eaglesakura.jc.parser.importer

import com.eaglesakura.jc.parser.pool.ParsedClassFile
import com.eaglesakura.tool.log.Logger
import com.eaglesakura.util.IOUtil
import com.eaglesakura.util.LogUtil
import javassist.ClassPool
import javassist.CtClass

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Classファイルを読み込む
 */
public class ClassFileImporter {

    List<CtClass> classes = new ArrayList<CtClass>();

    final File root;

    public ClassFileImporter(File root) {
        this.root = root;
    }

    /**
     * Streamからクラスを生成する
     */
    private synchronized void loadClassFromStream(InputStream is) {
        byte[] classBuffer = IOUtil.toByteArray(is, false);

        CtClass ctClass = ClassPool.getDefault().makeClass(new ByteArrayInputStream(classBuffer));
        if (ctClass != null) {
            // 後から入ったclassを優先するため、一旦アンロードとサイドロードを行う
            classes.add(ctClass);
        }
    }

    /**
     * Classファイルを読み込む
     * @param classFile
     */
    private void loadClassFromFile(File classFile) {
        if (classFile.name.contains('$')) {
            // "$"を含んでいるinnner classはjavaasist非対応
            return;
        }

        try {
            FileInputStream is = new FileInputStream(classFile);
            loadClassFromStream(is);
            is.close();
        } catch (Exception e) {
            LogUtil.log(e);
        }
    }

    /**
     * Zipファイルを読み込む
     * @param zip
     */
    private void loadClassFromZip(File zip) {
        InputStream is = null;
        try {
            int clzCount = 0;
            is = new FileInputStream(zip);
            ZipInputStream zis = new ZipInputStream(is);

            ZipEntry entry = null;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    try {
                        byte[] clazzBuffer = IOUtil.toByteArray(zis, false);
                        Logger.out("import class :: " + name);

                        loadClassFromStream(new ByteArrayInputStream(clazzBuffer));
                        ++clzCount;

                    } catch (Exception e) {
                        LogUtil.log(e);
                        Logger.out("import failed class :: " + name);
                    }
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            LogUtil.log(e);
        } finally {
            try {
                is.close();
            } catch (Exception e) {

            }
        }
    }

    /**
     *
     * @param file
     */
    private void parseFrom(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                // 再帰的に読み込む
                parseFrom(child);
            }
        } else {
            // 拡張子でチェック
            switch (IOUtil.getFileExt(file.name)) {
                case 'class':
                    loadClassFromFile(file); // ファイルから読み込む
                    break;
                case 'zip':
                case 'jar':
                    loadClassFromZip(file);
                    break;
            }
        }
    }

    /**
     * 解析を行う
     */
    public void parse() {
        Logger.pushIndent();
        // rootから再帰的に読み込む
        parseFrom(root);
        Logger.popIndent();

        // import数を報告
        Logger.out("import ${classes.size()} classes form(${root.absolutePath})");
    }

    /**
     * ファイルのビルドを行う
     * @return
     */
    public List<ParsedClassFile> build() {
        List<ParsedClassFile> parsedClasses = new ArrayList<CtClass>();

        for (def ctClass : classes) {
            // 出力用に解析して保存する
            ParsedClassFile parser = new ParsedClassFile(ctClass);
            parser.build();

            parsedClasses.add(parser);
        }

        return parsedClasses;
    }
}