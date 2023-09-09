package com.felixkroemer.trace_graph_engineering_tool.util;

import org.cytoscape.model.CyNetwork;

import java.util.Map;
import java.util.Properties;

public class Util {

    public static Properties genProperties(Map<String, String> input) {
        Properties props = new Properties();
        input.forEach(props::setProperty);
        return props;
    }

    public static boolean isTraceGraphNetwork(CyNetwork network) {
        return network.getDefaultNetworkTable().getColumn("TraceGraphMarker") != null;
    }

}
