package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;

public class TraceGraph {

    private CyNetwork network;
    private ParameterDiscretizationModel pdm;
    private CyTable rawData;

    public TraceGraph(ParameterDiscretizationModel pdm, CyTable rawData) {
        this.pdm = pdm;
        this.rawData = rawData;
    }


}
