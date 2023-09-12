package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
        this.suidHashMapping = new HashMap<>(); //TODO: find way to create nodes with specified SUID (hash as suid)
        this.initTables();
        this.initNetwork();
    }

    private void initTables() {
        for (Parameter param : this.pdm.getParameters()) {
            this.nodeTable.createColumn(param.getName(), Integer.class, false);
        }
        // cannot set default here because object will be shared
        this.nodeTable.createColumn(Columns.NODE_VISITS, Integer.class, false);
        this.nodeTable.createColumn(Columns.NODE_FREQUENCY, Integer.class, false);
        this.nodeTable.createListColumn(Columns.NODE_SOURCE_ROWS, Integer.class, false);

        this.edgeTable.createColumn(Columns.EDGE_TRAVERSALS, Integer.class, false, 1);

        CyTable networkTable = this.network.getDefaultNetworkTable();
        networkTable.createColumn(Columns.NETWORK_TG_MARKER, Integer.class, true);
    }

    private void initNetwork() {
        int[] state = new int[this.pdm.getParameterCount()];
        CyNode prevNode = null;
        CyNode currentNode = null;
        CyRow currentRow = null;
        for (CyRow sourceRow : rawDataTable.getAllRows()) {
            Map<String, Object> values = sourceRow.getAllValues();
            int i = 0;
            for (Parameter param : pdm.getParameters()) {
                state[i] = getBucket((Double) values.get(param.getName()), param);
                i++;
            }
            long hash = Arrays.hashCode(state);
            Long suid = suidHashMapping.get(hash);
            if (suid != null) { // node for the given state already exists
                currentNode = network.getNode(suid);
                currentRow = nodeTable.getRow(currentNode.getSUID());
                if (prevNode == currentNode) { // prevNode cannot be null here
                    currentRow.set(Columns.NODE_VISITS, currentRow.get(Columns.NODE_VISITS, Integer.class) + 1);
                } else {
                    currentRow.set(Columns.NODE_FREQUENCY, currentRow.get(Columns.NODE_FREQUENCY, Integer.class) + 1);
                }
            } else { // node does not exist yet
                currentNode = network.addNode();
                suidHashMapping.put(hash, currentNode.getSUID());
                currentRow = nodeTable.getRow(currentNode.getSUID());
                for (int j = 0; j < this.pdm.getParameterCount(); j++) {
                    currentRow.set(this.pdm.getParameters().get(j).getName(), state[j]);
                }
                currentRow.set(Columns.NODE_VISITS, 1);
                currentRow.set(Columns.NODE_FREQUENCY, 1);
                currentRow.set(Columns.NODE_SOURCE_ROWS, new ArrayList<>());
            }
            currentRow.getList(Columns.NODE_SOURCE_ROWS, Integer.class).add(sourceRow.get(Columns.SOURCE_ID,
                    Integer.class));
            if (prevNode != null && prevNode != currentNode) {
                CyEdge edge;
                if ((edge = getEdge(prevNode, currentNode)) == null) {
                    network.addEdge(prevNode, currentNode, true);
                } else {
                    CyRow edgeTableRow = this.edgeTable.getRow(edge.getSUID());
                    edgeTableRow.set(Columns.EDGE_TRAVERSALS, edgeTableRow.get(Columns.EDGE_TRAVERSALS,
                            Integer.class) + 1);
                }
            }
            prevNode = currentNode;
        }
    }

    // CyEdge.Type.OUTGOING or CyEdge.Type.INCOMING are necessary, network.containsEdge returns true for both
    // directions, same if CyEdge.Type.DIRECTED is used
    private CyEdge getEdge(CyNode source, CyNode target) {
        List<CyEdge> edges = this.network.getAdjacentEdgeList(source, CyEdge.Type.OUTGOING);
        for (CyEdge edge : edges) {
            if (edge.getTarget() == target) {
                return edge;
            }
        }
        return null;
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

    public ParameterDiscretizationModel getPDM() {
        return this.pdm;
    }
}
