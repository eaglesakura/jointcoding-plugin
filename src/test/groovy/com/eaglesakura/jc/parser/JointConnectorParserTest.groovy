package com.eaglesakura.jc.parser

import com.eaglesakura.tool.log.Logger

public class JointConnectorParserTest extends GroovyTestCase {

    public void testSampleParse() {
        Logger.initialize();
        Logger.setOutLogLevel(0);

        JointConnectorParser parser = new JointConnectorParser();
        parser.generateFilePath = new File("build/joint-connector-prebuild");
        parser.buildFlavor = "prebuild"
        parser.projectPath = new File("sample/app/");

        parser.build();
    }
}