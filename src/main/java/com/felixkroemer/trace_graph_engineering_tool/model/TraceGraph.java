package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.TaskMonitor;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TraceGraph {

    private final Logger logger;

    private CyNetwork network;
    private ParameterDiscretizationModel pdm;
    private CyTable sourceTable;
    private CyTable defaultNodeTable;
    private CyTable localNodeTable;
    private CyTable defaultEdgetable;
    private CyTable localEdgeTable;
    private Map<Long, Long> suidHashMapping;
    private Map<Long, CyNode> nodeMapping;

    public TraceGraph(CyNetwork network, ParameterDiscretizationModel pdm, CyTable sourceTable) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.pdm = pdm;
        this.sourceTable = sourceTable;
        this.network = network;

        this.network.getRow(network).set(CyNetwork.NAME, pdm.getName());
        // DEFAULT_ATTRS = Shared (root) + Local
        this.defaultNodeTable = this.network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
        this.localNodeTable = this.network.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS);
        this.defaultEdgetable = this.network.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS);
        this.localEdgeTable = this.network.getTable(CyEdge.class, CyNetwork.LOCAL_ATTRS);
        this.suidHashMapping = this.pdm.getSuidHashMapping(); //TODO: find way to create nodes with specified SUID
        // (hash as suid)
        this.nodeMapping = new HashMap<>();

        this.initTables();
        this.init();
    }

    private void initTables() {
        // cannot set default here because object will be shared
        this.localNodeTable.createColumn(Columns.NODE_VISITS, Integer.class, false);
        this.localNodeTable.createColumn(Columns.NODE_FREQUENCY, Integer.class, false);
        this.localNodeTable.createListColumn(Columns.NODE_SOURCE_ROWS, Integer.class, false);

        this.localEdgeTable.createColumn(Columns.EDGE_TRAVERSALS, Integer.class, false, 1);
        this.localEdgeTable.createListColumn(Columns.EDGE_SOURCE_ROWS, Integer.class, false);
    }

    public void init() {
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
            var rootNetwork = ((CySubNetwork) network).getRootNetwork();
            if (suid != null && (currentNode = rootNetwork.getNode(suid)) != null) { // node for the given state already exists, maybe only in root
                currentRow = defaultNodeTable.getRow(currentNode.getSUID());
                // node exists in root network, but not here
                if (!network.containsNode(currentNode)) {
                    // make node available from the default node table
                    ((CySubNetwork) network).addNode(currentNode);
                    currentRow = defaultNodeTable.getRow(currentNode.getSUID());
                    currentRow.set(Columns.NODE_VISITS, 1);
                    currentRow.set(Columns.NODE_FREQUENCY, 1);
                    currentRow.set(Columns.NODE_SOURCE_ROWS, new ArrayList<>());
                }
                if (prevNode == currentNode) { // prevNode cannot be null here
                    currentRow.set(Columns.NODE_VISITS, currentRow.get(Columns.NODE_VISITS, Integer.class) + 1);
                } else {
                    currentRow.set(Columns.NODE_FREQUENCY, currentRow.get(Columns.NODE_FREQUENCY, Integer.class) + 1);
                }
            } else { // node does not exist yet
                currentNode = network.addNode();
                suidHashMapping.put(hash, currentNode.getSUID());
                currentRow = defaultNodeTable.getRow(currentNode.getSUID());
                for (int j = 0; j < this.pdm.getParameters().size(); j++) {
                    currentRow.set(this.pdm.getParameters().get(j).getName(), state[j]);
                }
                currentRow.set(Columns.NODE_VISITS, 1);
                currentRow.set(Columns.NODE_FREQUENCY, 1);
                currentRow.set(Columns.NODE_SOURCE_ROWS, new ArrayList<>());
            }
            this.nodeMapping.put(sourceRow.get(Columns.SOURCE_ID, Long.class), currentNode);
            currentRow.getList(Columns.NODE_SOURCE_ROWS, Integer.class).add(sourceRow.get(Columns.SOURCE_ID,
                    Long.class).intValue());
            if (prevNode != null && prevNode != currentNode) {
                CyEdge edge;
                CyRow edgeRow;
                if ((edge = getEdge(prevNode, currentNode)) == null) {
                    edge = network.addEdge(prevNode, currentNode, true);
                    edgeRow = this.defaultEdgetable.getRow(edge.getSUID());
                    edgeRow.set(Columns.EDGE_SOURCE_ROWS, new ArrayList<>());
                } else {
                    edgeRow = this.defaultEdgetable.getRow(edge.getSUID());

                    edgeRow.set(Columns.EDGE_TRAVERSALS, edgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class) + 1);
                }
                edgeRow.getList(Columns.EDGE_SOURCE_ROWS, Integer.class).add(sourceRow.get(Columns.SOURCE_ID,
                        Long.class).intValue() - 1);
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
    public CyNode findNode(long sourceTableIndex) {
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
        // also removes edges and clears all tables
        this.network.removeNodes(this.network.getNodeList());
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
            var oldNodeRow = this.defaultNodeTable.getRow(oldNode.getSUID());
            for (int j = 0; j < pdm.getParameterCount(); j++) {
                state[j] = oldNodeRow.get(this.pdm.getParameters().get(j).getName(), Integer.class);
            }
            int oldNodeBucket = oldNodeRow.get(changedParameter.getName(), Integer.class);
            // set of rows in the source table that map to the old node
            var sourceRowSet = this.defaultNodeTable.getRow(oldNode.getSUID()).getList(Columns.NODE_SOURCE_ROWS,
                    Integer.class);
            // also contains source row index i
            for (int j : sourceRowSet) {
                visited[j] = true;

                var sourceRow = this.sourceTable.getRow((long) j);
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
                    // node exists, maybe only in root network
                    var rootNetwork = ((CySubNetwork) network).getRootNetwork();
                    // may be null if suidHashMapping reference is stale; -> node may have been removed from all
                    // TraceGraphs of the pdm, reference is not cleaned up automatically
                    if (suid != null && (newNode = rootNetwork.getNode(suid)) != null) { // implies suid is not null
                        // may be null
                        newNodeRow = defaultNodeTable.getRow(newNode.getSUID());
                        // node exists in root network, but not here
                        if (!network.containsNode(newNode)) {
                            // make node available from the default node table
                            ((CySubNetwork) network).addNode(newNode);
                            newNodeRow = defaultNodeTable.getRow(newNode.getSUID());
                            newNodeRow.set(Columns.NODE_VISITS, 1);
                            newNodeRow.set(Columns.NODE_FREQUENCY, 1);
                            newNodeRow.set(Columns.NODE_SOURCE_ROWS, new ArrayList<>());
                        }
                    } else {
                        newNode = network.addNode();
                        suidHashMapping.put(hash, newNode.getSUID());
                        newNodeRow = defaultNodeTable.getRow(newNode.getSUID());
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
                    this.nodeMapping.put((long) j, newNode);
                    if (oldNodeRow.getList(Columns.NODE_SOURCE_ROWS, Integer.class).isEmpty()) {
                        nodesToRemove.add(oldNode);
                    }
                    // move edges
                    // incoming
                    if (j != 1) {
                        CyNode source = this.findNode(j - 1);
                        CyEdge oldEdge = this.getEdge(source, oldNode);
                        if (oldEdge != null) {
                            var oldEdgeRow = this.defaultEdgetable.getRow(oldEdge.getSUID());
                            var oldEdgeTraversals = oldEdgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class);
                            oldEdgeRow.set(Columns.EDGE_TRAVERSALS, oldEdgeTraversals - 1);
                            CyEdge newEdge = null;
                            CyRow newEdgeRow = null;
                            if ((newEdge = this.getEdge(source, newNode)) != null) {
                                newEdgeRow = this.defaultEdgetable.getRow(newEdge.getSUID());
                                var newEdgeTraversals = newEdgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class);
                                this.defaultEdgetable.getRow(newEdge.getSUID()).set(Columns.EDGE_TRAVERSALS,
                                        newEdgeTraversals + 1);
                            } else {
                                newEdge = network.addEdge(source, newNode, true);
                                newEdgeRow = this.defaultEdgetable.getRow(newEdge.getSUID());
                                newEdgeRow.set(Columns.EDGE_SOURCE_ROWS, new ArrayList<>());
                            }
                            newEdgeRow.getList(Columns.EDGE_SOURCE_ROWS, Integer.class).add((int) j - 1);
                        }
                    }
                    if (j != sourceTable.getRowCount()) {
                        CyNode target = this.findNode(j + 1);
                        CyEdge oldEdge = this.getEdge(oldNode, target);
                        if (oldEdge != null) {
                            var oldEdgeRow = this.defaultEdgetable.getRow(oldEdge.getSUID());
                            var oldEdgeTraversals = oldEdgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class);
                            oldEdgeRow.set(Columns.EDGE_TRAVERSALS, oldEdgeTraversals - 1);

                            CyEdge newEdge = null;
                            CyRow newEdgeRow = null;
                            if ((newEdge = this.getEdge(newNode, target)) != null) {
                                newEdgeRow = this.defaultEdgetable.getRow(newEdge.getSUID());
                                var newEdgeTraversals = newEdgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class);
                                this.defaultEdgetable.getRow(newEdge.getSUID()).set(Columns.EDGE_TRAVERSALS,
                                        newEdgeTraversals + 1);
                            } else {
                                newEdge = network.addEdge(newNode, target, true);
                                newEdgeRow = this.defaultEdgetable.getRow(newEdge.getSUID());
                                newEdgeRow.set(Columns.EDGE_SOURCE_ROWS, new ArrayList<>());
                            }
                            newEdgeRow.getList(Columns.EDGE_SOURCE_ROWS, Integer.class).add((int) j - 1);
                        }
                    }
                }
            }
        }
        // if node source rows is empty there is no entry in nodemapping that points to this node, it can be removed
        // from this network but may still exist in another network with the same pdm
        for (CyNode node : this.network.getNodeList()) {
            if (this.defaultNodeTable.getRow(node.getSUID()).getList(Columns.NODE_SOURCE_ROWS, Integer.class).isEmpty()) {
                nodesToRemove.add(node);
            }
        }
        for (CyEdge edge : this.network.getEdgeList()) {
            if (this.defaultEdgetable.getRow(edge.getSUID()).getList(Columns.EDGE_SOURCE_ROWS, Integer.class).isEmpty()) {
                edgesToRemove.add(edge);
            }
        }
        // need to be removed in batches, otherwise events take forever
        // TODO: check problem with rendering timer concurrency (trying to render nodes that were already deleted)
        this.network.removeNodes(nodesToRemove);
        this.network.removeEdges(edgesToRemove);
    }

    private void fixEdge(CyNode oldSource, CyNode newSource, CyNode target) {

    }

    public Trace findTrace(List<CyNode> nodes) {
        if (nodes.size() != 2) {
            return null;
        }
        var nodeA = nodes.get(0);
        var nodeB = nodes.get(1);

        var sourcesA = this.defaultNodeTable.getRow(nodeA.getSUID()).getList(Columns.NODE_SOURCE_ROWS, Integer.class);
        var sourcesB = this.defaultNodeTable.getRow(nodeB.getSUID()).getList(Columns.NODE_SOURCE_ROWS, Integer.class);

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
