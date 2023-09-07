package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TraceGraph {

    private final Logger logger;

    private final CyNetwork network;
    private ParameterDiscretizationModel pdm;
    private CyTable rawDataTable;
    private CyTable nodeTable;
    private CyTable edgeTable;
    private Map<Long, Long> suidHashMapping;

    public TraceGraph(CyNetworkFactory networkFactory, ParameterDiscretizationModel pdm, CyTable rawData) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.pdm = pdm;
        this.rawDataTable = rawData;
        this.network = networkFactory.createNetwork();
        this.network.getRow(network).set(CyNetwork.NAME, pdm.getName());
        this.nodeTable = this.network.getDefaultNodeTable();
        this.edgeTable = this.network.getDefaultEdgeTable();
        this.suidHashMapping = new HashMap<>(); //TODO: find way to create nodes with specified SUID
        this.initNetwork();
    }

    private void initNetwork() {

        for (Parameter param : this.pdm.getParameters()) {
            this.nodeTable.createColumn(param.getName(), Integer.class, false);
        }

        int[] state = new int[this.pdm.getParameterCount()];
        CyNode prevNode = null;
        for (CyRow row : rawDataTable.getAllRows()) {
            Map<String, Object> values = row.getAllValues();
            int i = 0;
            for (Parameter param : pdm.getParameters()) {
                state[i] = getBucket((Double) values.get(param.getName()), param);
                i++;
            }
            long hash = Arrays.hashCode(state);
            Long suid = suidHashMapping.get(hash);
            if (suid != null) {
                // TODO: append row to reference list of node
            } else {
                CyNode node = network.addNode();
                suidHashMapping.put(hash, node.getSUID());
                CyRow newRow = nodeTable.getRow(node.getSUID());
                for (int j = 0; j < this.pdm.getParameterCount(); j++) {
                    newRow.set(this.pdm.getParameters().get(j).getName(), state[j]);
                }
                if (prevNode != null) {
                    network.addEdge(node, prevNode, true);
                }
                prevNode = node;
            }

        }

    }

    private int getBucket(Double value, Parameter param) {
        for (int i = 0; i < param.getBins().size(); i++) {
            if (value < param.getBins().get(i)) {
                return i;
            }
        }
        return param.getBins().size();
    }

    public CyNetwork getNetwork() {
        return this.network;
    }
}
