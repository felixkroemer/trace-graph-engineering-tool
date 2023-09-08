package com.felixkroemer.trace_graph_engineering_tool.util;

import java.util.Map;
import java.util.Properties;

public class Util {

    public static Properties genProperties(Map<String, String> input) {
        Properties props = new Properties();
        input.forEach(props::setProperty);
        return props;
    }

}
