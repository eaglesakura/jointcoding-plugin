package com.eaglesakura.jc.generator.wrapper

import com.eaglesakura.jc.parser.model.Constants
import com.eaglesakura.jc.parser.model.JointModel
import com.eaglesakura.jc.parser.model.TypeUtil
import com.eaglesakura.tool.log.Logger
import com.eaglesakura.util.LogUtil

/**
 * クラスのグローバルキャッシュを行う
 */
public class ClassesCache {
    private static Map<String, JointClassWrapper> sCaches = new HashMap<>();

    /**
     * 　デフォルトオブジェクトを指定する
     * クラスが見つからなかった場合、このオブジェクトを返す
     * @param defObjectClass
     */
    static void setDefaultObjectClass(JointClassWrapper defObjectClass) {
        ClassesCache.defObjectClass = defObjectClass
    }

    /**
     * Javaフル名を指定したクラスを返す
     * @param javaFullName
     * @return
     */
    public static JointClassWrapper get(String javaFullName) {
        int dimen = 0;
        if (javaFullName.startsWith("[")) {
            ++dimen;
            javaFullName = javaFullName.substring(1);
        }

        JointClassWrapper result = sCaches.get(javaFullName);
        if (result != null) {
            return result.newClassWrapper(dimen);
        } else {
            return sCaches.get(Constants.TYPENAME_LANG_ROOTCLASS).newClassWrapper(dimen);
        }
    }

    /**
     * キャッシュをクリアする
     */
    public static void clean() {
        sCaches.clear();
    }

    /**
     * 出力対象を取得する
     * @return
     */
    public static List<JointClassWrapper> listGenerateTargets() {
        def result = new ArrayList<JointClassWrapper>();

        // info
        for (def itr : sCaches.entrySet().iterator()) {
            def wrapper = itr.value;
            if (wrapper.isGenerateTarget()) {
                Logger.out "generate target :: ${wrapper.javaFullName} / super(${wrapper.superClass.javaFullName})";
                result.add(wrapper);
            }
        }

        return result;
    }

    /**
     * プリミティブ型をロードする
     */
    public static void loadPrimitives() {
        def types = TypeUtil.listPrimitiveTypes();
        for (def primitive : types) {
            JointClassWrapper wrapper = new JointClassWrapper(primitive);
            sCaches.put(wrapper.javaFullName, wrapper);
        }
    }

    /**
     * parserから出力したクラスファイルをロードする
     * @param file
     */
    public static void load(File file) {
        try {
            Logger.pushIndent();

            InputStream is = new FileInputStream(file);

            JointModel.GeneratedClasses generatedClasses = JointModel.GeneratedClasses.parseFrom(is);

            Logger.out "loaded ${generatedClasses.classesCount} classes"

            for (def clz : generatedClasses.classesList) {
                JointClassWrapper wrapper = new JointClassWrapper(clz);
                sCaches.put(wrapper.javaFullName, wrapper);

                // 出力対象であればログ
                if (wrapper.isGenerateTarget()) {
                    Logger.out "load target :: ${wrapper.javaFullName}";
                }
            }

            is.close();
        } catch (FileNotFoundException ioe) {
            Logger.out("${file.absolutePath} not found...")
        } catch (Exception e) {
            LogUtil.log(e);
        } finally {
            Logger.popIndent();
        }
    }
}