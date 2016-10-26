package com.eaglesakura.jc.parser.dex

/**
 *
 */
public class PreDexedJarDecompilerTest extends GroovyTestCase {
    public void testDex2jar() {
        PreDexedJarDecompiler decompiler = new PreDexedJarDecompiler();
        decompiler.androidBuildDirectory = new File("sample/app/build");
        decompiler.preDexedDirectory = new File("sample/app/build/intermediates/pre-dexed/prebuild/debug");
        decompiler.setDecompileOutDirectory("prebuild", "debug");

        decompiler.execute();
    }
}