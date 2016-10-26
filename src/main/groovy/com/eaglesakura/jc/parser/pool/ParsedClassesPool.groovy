package com.eaglesakura.jc.parser.pool

import com.eaglesakura.jc.parser.model.JointModel;

/**
 * 必要なクラスをpoolしておく
 */
public class ParsedClassesPool {
    private JointModel.GeneratedClasses.Builder generatedClasses;

    private Set<ParsedClassFile> parsedClasses = new HashSet<ParsedClassFile>();

    public ParsedClassesPool() {

    }

    /**
     * クラスを追加する
     * @param classes
     */
    public void add(List<ParsedClassFile> classes) {
        for (ParsedClassFile p : classes) {
            parsedClasses.add(p);
        }
    }

    public int getClassesNum() {
        return parsedClasses.size();
    }

    /**
     * 最終出力用のプールを作成する
     */
    public void build() {
        generatedClasses = JointModel.GeneratedClasses.newBuilder();
        for (ParsedClassFile p : parsedClasses) {
            generatedClasses.addClasses(p.getClassBuilder());
        }
    }

    /**
     * 最終出力を行う
     * @param output
     */
    public void export(File output) {
        byte[] buffer = generatedClasses.build().toByteArray();

        FileOutputStream os = new FileOutputStream(output);
        os.write(buffer);
        os.flush();
        os.close();
    }
}