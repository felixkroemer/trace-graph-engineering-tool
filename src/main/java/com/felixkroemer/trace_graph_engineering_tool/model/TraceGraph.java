package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.*;
import org.cytoscape.work.TaskMonitor;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeSupport;
import java.util.*;

public class TraceGraph {

    private final Logger logger;

    private CyNetwork network;
    private ParameterDiscretizationModel pdm;
    private CyTable sourceTable;
    private CyTable nodeTable;
    private CyTable edgeTable;
    private Map<Long, Long> suidHashMapping;
    private Map<Integer, CyNode> nodeMapping;
    private PropertyChangeSupport pcs;

    public TraceGraph(CyNetwork network, ParameterDiscretizationModel pdm, CyTable sourceTable) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.pdm = pdm;
        this.sourceTable = sourceTable;
        this.network = network;

        this.network.getRow(network).set(CyNetwork.NAME, pdm.getName());
        this.nodeTable = this.network.getDefaultNodeTable();
        this.edgeTable = this.network.getDefaultEdgeTable();
        this.suidHashMapping = new HashMap<>(); //TODO: find way to create nodes with specified SUID (hash as suid)
        this.nodeMapping = new HashMap<>();
        this.initTables();
        this.initNetwork();
    }

    private void initTables() {
        this.pdm.forEach(p -> this.nodeTable.createColumn(p.getName(), Integer.class, false));
        // cannot set default here because object will be shared
        this.nodeTable.createColumn(Columns.NODE_VISITS, Integer.class, false);
        this.nodeTable.createColumn(Columns.NODE_FREQUENCY, Integer.class, false);
        this.nodeTable.createListColumn(Columns.NODE_SOURCE_ROWS, Integer.class, false);

        this.edgeTable.createColumn(Columns.EDGE_TRAVERSALS, Integer.class, false, 1);
        this.edgeTable.createListColumn(Columns.EDGE_SOURCE_ROWS, Integer.class, false);

        CyTable networkTable = this.network.getDefaultNetworkTable();
        networkTable.createColumn(Columns.NETWORK_TG_MARKER, Integer.class, true);
    }

    public void initNetwork() {
        int[] state = new int[this.pdm.getParameterCount()];
        CyNode prevNode = null;
        CyNode currentNode = null;
        CyRow currentRow = null;
        for (CyRow sourceRow : sourceTable.getAllRows()) {
            Map<String, Object> values = sourceRow.getAllValues();
            int i = 0;
            for (Parameter param : pdm.getParameters()) {
                if (param.isEnabled()) {
                    state[i] = findBucket((Double) values.get(param.getName()), param);
                } else {
                    state[i] = 0;
                }
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
                for (int j = 0; j < this.pdm.getParameters().size(); j++) {
                    currentRow.set(this.pdm.getParameters().get(j).getName(), state[j]);
                }
                currentRow.set(Columns.NODE_VISITS, 1);
                currentRow.set(Columns.NODE_FREQUENCY, 1);
                currentRow.set(Columns.NODE_SOURCE_ROWS, new ArrayList<>());
            }
            this.nodeMapping.put(sourceRow.get("id", Integer.class), currentNode);
            currentRow.getList(Columns.NODE_SOURCE_ROWS, Integer.class).add(sourceRow.get(Columns.SOURCE_ID,
                    Integer.class));
            if (prevNode != null && prevNode != currentNode) {
                CyEdge edge;
                CyRow edgeRow;
                if ((edge = getEdge(prevNode, currentNode)) == null) {
                    edge = network.addEdge(prevNode, currentNode, true);
                    edgeRow = this.edgeTable.getRow(edge.getSUID());
                    edgeRow.set(Columns.EDGE_SOURCE_ROWS, new ArrayList<>());
                } else {
                    edgeRow = this.edgeTable.getRow(edge.getSUID());

                    edgeRow.set(Columns.EDGE_TRAVERSALS, edgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class) + 1);
                }
                edgeRow.getList(Columns.EDGE_SOURCE_ROWS, Integer.class).add(sourceRow.get(Columns.SOURCE_ID,
                        Integer.class) - 1);

            }
            prevNode = currentNode;
        }
    }

    public CyEdge getEdge(CyNode source, CyNode target) {
        List<CyEdge> edges = this.network.getConnectingEdgeList(source, target, CyEdge.Type.DIRECTED);
        for (CyEdge edge : edges) {
            if (edge.getTarget() == target && edge.getSource() == source) {
                return edge;
            }
        }
        return null;
    }

    private int findBucket(Double value, Parameter param) {
        for (int i = 0; i < param.getBins().size(); i++) {
            if (value < param.getBins().get(i)) {
                return i;
            }
        }
        return param.getBins().size();
    }

    /*
     * Find the node that a row in the sourceTable belongs to
     */
    public CyNode findNode(int sourceTableIndex) {
        return this.nodeMapping.get(sourceTableIndex);
    }

    public CyNetwork getNetwork() {
        return this.network;
    }

    public CyTable getSourceTable() {
        return this.sourceTable;
    }

    public ParameterDiscretizationModel getPDM() {
        return this.pdm;
    }

    public void clearNetwork() {
        this.network.removeNodes(this.network.getNodeList());
        suidHashMapping.clear();
        nodeMapping.clear();
    }

    public void reinitNetwork(Parameter changedParameter, TaskMonitor monitor) {
        int changedParameterIndex = -1;
        for (int j = 0; j < pdm.getParameterCount(); j++) {
            if (this.pdm.getParameters().get(j).equals(changedParameter)) {
                changedParameterIndex = j;
                break;
            }
        }
        List<CyNode> nodesToRemove = new ArrayList<>();
        List<CyEdge> edgesToRemove = new ArrayList<>();
        int[] state = new int[this.pdm.getParameterCount()];
        boolean[] visited = new boolean[sourceTable.getRowCount() + 1];
        // TODO: improvement: dont use i as representative for node but most common new bucket
        for (int i = 1; i <= sourceTable.getRowCount(); i++) {
            if (visited[i]) continue;
            // old node, may contain source rows that dont belong to this node anymore (if bucket of source row changes)
            // uses source row index to node map, not influenced by already changed parameter bins
            var oldNode = this.findNode(i);
            var oldNodeRow = this.nodeTable.getRow(oldNode.getSUID());
            for (int j = 0; j < pdm.getParameterCount(); j++) {
                state[j] = oldNodeRow.get(this.pdm.getParameters().get(j).getName(), Integer.class);
            }
            int oldNodeBucket = oldNodeRow.get(changedParameter.getName(), Integer.class);
            // set of rows in the source table that map to the old node
            var sourceRowSet = this.nodeTable.getRow(oldNode.getSUID()).getList(Columns.NODE_SOURCE_ROWS,
                    Integer.class);
            // also contains source row index i
            for (int j : sourceRowSet) {
                visited[j] = true;

                var sourceRow = this.sourceTable.getRow(j);
                double sourceRowValue = sourceRow.get(changedParameter.getName(), Double.class);
                var bucket = findBucket(sourceRowValue, changedParameter);

                // source row j does not belong to this node anymore
                // create state of j, hash it, check if a node with that hash exists, add j to it,
                // otherwise create a new node with the state of j
                // move ingoing and outgoing edges belonging to j from oldNode to newNode
                if (bucket != oldNodeBucket) {
                    state[changedParameterIndex] = bucket;
                    long hash = Arrays.hashCode(state);
                    Long suid = suidHashMapping.get(hash);
                    CyNode newNode;
                    CyRow newNodeRow;
                    if (suid != null && (newNode = network.getNode(suid)) != null) { // node for the given state
                        // already exists
                        newNodeRow = this.nodeTable.getRow(newNode.getSUID());
                    } else {
                        newNode = network.addNode();
                        suidHashMapping.put(hash, newNode.getSUID());
                        newNodeRow = nodeTable.getRow(newNode.getSUID());
                        for (int k = 0; k < this.pdm.getParameters().size(); k++) {
                            newNodeRow.set(this.pdm.getParameters().get(k).getName(), state[k]);
                        }
                        newNodeRow.set(Columns.NODE_VISITS, 1);
                        newNodeRow.set(Columns.NODE_FREQUENCY, 1);
                        newNodeRow.set(Columns.NODE_SOURCE_ROWS, new ArrayList<>());
                    }
                    // TODO: adjust NODE_VISITS
                    // TODO: adjust NODE_FREQUENCY
                    // move source row index from old to new, remove old edge if traversals is 0
                    oldNodeRow.getList(Columns.NODE_SOURCE_ROWS, Integer.class).remove((Object) j);
                    newNodeRow.getList(Columns.NODE_SOURCE_ROWS, Integer.class).add(j);
                    if (oldNodeRow.getList(Columns.NODE_SOURCE_ROWS, Integer.class).size() == 0) {
                        nodesToRemove.add(oldNode);
                    }
                    this.nodeMapping.put(j, newNode);
                    // move edges
                    // incoming
                    if (j != 1) {
                        CyNode source = this.findNode(j - 1);
                        CyEdge oldEdge = this.getEdge(source, oldNode);
                        if (oldEdge != null) {
                            var oldEdgeRow = this.edgeTable.getRow(oldEdge.getSUID());
                            var oldEdgeTraversals = oldEdgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class);
                            if (oldEdgeTraversals == 1) {
                                edgesToRemove.add(oldEdge);
                            } else {
                                oldEdgeRow.set(Columns.EDGE_TRAVERSALS, oldEdgeTraversals - 1);
                            }
                            CyEdge newEdge = null;
                            CyRow newEdgeRow = null;
                            if ((newEdge = this.getEdge(source, newNode)) != null) {
                                newEdgeRow = this.edgeTable.getRow(newEdge.getSUID());
                                var newEdgeTraversals = newEdgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class);
                                this.edgeTable.getRow(newEdge.getSUID()).set(Columns.EDGE_TRAVERSALS,
                                        newEdgeTraversals + 1);
                            } else {
                                newEdge = network.addEdge(source, newNode, true);
                                newEdgeRow = this.edgeTable.getRow(newEdge.getSUID());
                                newEdgeRow.set(Columns.EDGE_SOURCE_ROWS, new ArrayList<>());
                            }
                            newEdgeRow.getList(Columns.EDGE_SOURCE_ROWS, Integer.class).add(j - 1);
                        }
                    }
                    if (j != sourceTable.getRowCount()) {
                        CyNode target = this.findNode(j + 1);
                        CyEdge oldEdge = this.getEdge(oldNode, target);
                        if (oldEdge != null) {
                            var oldEdgeRow = this.edgeTable.getRow(oldEdge.getSUID());
                            var oldEdgeTraversals = oldEdgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class);
                            if (oldEdgeTraversals == 1) {
                                edgesToRemove.add(oldEdge);
                            } else {
                                oldEdgeRow.set(Columns.EDGE_TRAVERSALS, oldEdgeTraversals - 1);
                            }

                            CyEdge newEdge = null;
                            CyRow newEdgeRow = null;
                            if ((newEdge = this.getEdge(newNode, target)) != null) {
                                newEdgeRow = this.edgeTable.getRow(newEdge.getSUID());
                                var newEdgeTraversals = newEdgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class);
                                this.edgeTable.getRow(newEdge.getSUID()).set(Columns.EDGE_TRAVERSALS,
                                        newEdgeTraversals + 1);
                            } else {
                                newEdge = network.addEdge(newNode, target, true);
                                newEdgeRow = this.edgeTable.getRow(newEdge.getSUID());
                                newEdgeRow.set(Columns.EDGE_SOURCE_ROWS, new ArrayList<>());
                            }
                            newEdgeRow.getList(Columns.EDGE_SOURCE_ROWS, Integer.class).add(j - 1);
                        }
                    }
                }
            }
        }
        // need to be removed in batches, otherwise events take forever
        this.network.removeEdges(edgesToRemove);
        this.network.removeNodes(nodesToRemove);
    }

    private void fixEdge(CyNode oldSource, CyNode newSource, CyNode target) {

    }

    public Trace findTrace(List<CyNode> nodes) {
        if (nodes.size() != 2) {
            return null;
        }
        var nodeA = nodes.get(0);
        var nodeB = nodes.get(1);

        var sourcesA = this.nodeTable.getRow(nodeA.getSUID()).getList(Columns.NODE_SOURCE_ROWS, Integer.class);
        var sourcesB = this.nodeTable.getRow(nodeB.getSUID()).getList(Columns.NODE_SOURCE_ROWS, Integer.class);

        Pair<Integer, Integer> window = null;
        Trace trace = new Trace();
        for (var x : sourcesA) {
            for (var y : sourcesB) {
                if (window == null) {
                    var lowerBound = x < y ? x : y;
                    var upperBound = lowerBound == x ? y : x;
                    window = new Pair<>(lowerBound, upperBound);
                } else {
                    if (Math.abs(x - y) < window.getValue1() - window.getValue0()) {
                        var lowerBound = x < y ? x : y;
                        var upperBound = lowerBound == x ? y : x;
                        window = new Pair<>(lowerBound, upperBound);
                    }
                }
            }
        }

        for (int i = window.getValue0(); i <= window.getValue1(); i++) {
            trace.addAfter(findNode(i), i);
        }
        return trace;
    }
}
