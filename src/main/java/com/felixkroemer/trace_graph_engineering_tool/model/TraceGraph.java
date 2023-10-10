package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CySubNetwork;
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
    private CyTable defaultEdgetable;

    // hash to node suid
    //TODO: map directly to node
    private Map<Long, Long> suidHashMapping;
    // source row index to node
    // every source row index must always be mapped to an existing node in the graph
    private Map<Long, CyNode> nodeMapping;
    // node to node auxiliary information
    private Map<CyNode, AuxiliaryInformation> nodeInfo;
    private Map<CyEdge, AuxiliaryInformation> edgeInfo;

    public TraceGraph(CyNetwork network, ParameterDiscretizationModel pdm, CyTable sourceTable) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.pdm = pdm;
        this.sourceTable = sourceTable;
        this.network = network;

        this.network.getRow(network).set(CyNetwork.NAME, pdm.getName());
        // DEFAULT_ATTRS = Shared (root) + Local
        this.defaultNodeTable = this.network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
        this.defaultEdgetable = this.network.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS);
        this.suidHashMapping = this.pdm.getSuidHashMapping(); //TODO: find way to create nodes with specified SUID
        this.nodeMapping = new HashMap<>();
        this.nodeInfo = new HashMap<>();
        this.edgeInfo = new HashMap<>();

        this.initTables();
        this.init();
    }

    private void initTables() {
        var localNodeTable = this.network.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS);
        localNodeTable.createColumn(Columns.NODE_VISITS, Integer.class, false);
        localNodeTable.createColumn(Columns.NODE_FREQUENCY, Integer.class, false);

        var localEdgeTable = this.network.getTable(CyEdge.class, CyNetwork.LOCAL_ATTRS);
        localEdgeTable.createColumn(Columns.EDGE_TRAVERSALS, Integer.class, false, 1);
    }

    public void init() {
        int[] state = new int[this.pdm.getParameterCount()];
        CyNode prevNode = null;
        CyNode currentNode = null;
        CyRow currentRow = null;
        AuxiliaryInformation currentNodeInfo = null;
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
                currentNodeInfo = this.nodeInfo.get(currentNode);
                // node exists in root network, but not here
                if (!network.containsNode(currentNode)) {
                    // make node available from the default node table
                    ((CySubNetwork) network).addNode(currentNode);
                    currentRow = defaultNodeTable.getRow(currentNode.getSUID());
                    currentRow.set(Columns.NODE_VISITS, 1);
                    currentRow.set(Columns.NODE_FREQUENCY, 1);
                    currentNodeInfo = new AuxiliaryInformation();
                    this.nodeInfo.put(currentNode, currentNodeInfo);
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
                currentNodeInfo = new AuxiliaryInformation();
                this.nodeInfo.put(currentNode, currentNodeInfo);
            }
            this.nodeMapping.put(sourceRow.get(Columns.SOURCE_ID, Long.class), currentNode);
            currentNodeInfo.addSourceRow(sourceTable, sourceRow.get(Columns.SOURCE_ID, Long.class).intValue());
            if (prevNode != null && prevNode != currentNode) {
                CyEdge edge;
                AuxiliaryInformation edgeAux;
                if ((edge = getEdge(prevNode, currentNode)) == null) {
                    edge = network.addEdge(prevNode, currentNode, true);
                    edgeAux = new AuxiliaryInformation();
                    this.edgeInfo.put(edge, edgeAux);
                } else {
                    CyRow edgeRow = this.defaultEdgetable.getRow(edge.getSUID());
                    edgeRow.set(Columns.EDGE_TRAVERSALS, edgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class) + 1);
                    edgeAux = this.edgeInfo.get(edge);
                }
                edgeAux.addSourceRow(this.sourceTable, sourceRow.get(Columns.SOURCE_ID, Long.class).intValue() - 1);
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
        this.nodeInfo.clear();
    }

    public void reinit(Parameter changedParameter) {
        this.network.removeEdges(this.network.getEdgeList());
        this.edgeInfo.clear();
        int changedParameterIndex = -1;
        for (int j = 0; j < pdm.getParameterCount(); j++) {
            if (this.pdm.getParameters().get(j).equals(changedParameter)) {
                changedParameterIndex = j;
                break;
            }
        }
        int[] state = new int[this.pdm.getParameterCount()];
        boolean[] visited = new boolean[sourceTable.getRowCount() + 1];
        // TODO: improvement: dont use i as representative for node but most common new bucket
        for (int i = 1; i <= sourceTable.getRowCount(); i++) {
            if (visited[i]) continue;
            // old node, may contain source rows that dont belong to this node anymore (if bucket of source row changes)
            // uses source row index to node map, not influenced by already changed parameter bins
            var oldNode = this.nodeMapping.get((long) i);
            var oldNodeRow = this.defaultNodeTable.getRow(oldNode.getSUID());
            var oldNodeAux = this.nodeInfo.get(oldNode);
            for (int j = 0; j < pdm.getParameterCount(); j++) {
                state[j] = oldNodeRow.get(this.pdm.getParameters().get(j).getName(), Integer.class);
            }
            int oldNodeBucket = oldNodeRow.get(changedParameter.getName(), Integer.class);
            // set of rows in the source table that map to the old node
            // also contains source row index i
            var iterator = this.nodeInfo.get(oldNode).getSourceRows(this.sourceTable).iterator();
            while (iterator.hasNext()) {
                var j = iterator.next();
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
                    AuxiliaryInformation newNodeAux;
                    // node exists, maybe only in root network
                    var rootNetwork = ((CySubNetwork) network).getRootNetwork();
                    // may be null if suidHashMapping reference is stale; -> node may have been removed from all
                    // TraceGraphs of the pdm, reference is not cleaned up automatically
                    if (suid != null && (newNode = rootNetwork.getNode(suid)) != null) { // implies suid is not null
                        // may be null
                        newNodeRow = defaultNodeTable.getRow(newNode.getSUID());
                        newNodeAux = this.nodeInfo.get(newNode);
                        // node exists in root network, but not here
                        if (!network.containsNode(newNode)) {
                            // make node available from the default node table
                            ((CySubNetwork) network).addNode(newNode);
                            newNodeRow = defaultNodeTable.getRow(newNode.getSUID());
                            newNodeRow.set(Columns.NODE_VISITS, 1);
                            newNodeRow.set(Columns.NODE_FREQUENCY, 1);
                            newNodeAux = new AuxiliaryInformation();
                            this.nodeInfo.put(newNode, newNodeAux);
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
                        newNodeAux = new AuxiliaryInformation();
                        this.nodeInfo.put(newNode, newNodeAux);
                    }
                    // TODO: adjust NODE_VISITS
                    // TODO: adjust NODE_FREQUENCY
                    // move source row index from old to new, remove old edge if traversals is 0
                    //oldNodeAux.getSourceRows(this.sourceTable).remove((Object) j);
                    iterator.remove();
                    newNodeAux.addSourceRow(this.sourceTable, j);
                    this.nodeMapping.put((long) j, newNode);
                }
            }
        }
        this.generateEdges();
        this.removeLeftoverNodes();
    }

    private void removeLeftoverNodes() {
        List<CyNode> nodesToRemove = new ArrayList<>();
        // if node source rows is empty there is no entry in nodemapping that points to this node, it can be removed
        // from this network but may still exist in another network with the same pdm
        for (CyNode node : this.network.getNodeList()) {
            //TODO clear empty entries
            if (this.nodeInfo.get(node).hasNoSourceRows()) {
                nodesToRemove.add(node);
                this.nodeInfo.remove(node);
            }
        }
        // need to be removed in batches, otherwise events take forever
        // TODO: check problem with rendering timer concurrency (trying to render nodes that were already deleted)
        this.network.removeNodes(nodesToRemove);
    }

    public void generateEdges() {
        CyNode prevNode = null;
        CyNode currentNode;
        for (CyRow sourceRow : sourceTable.getAllRows()) {
            currentNode = this.nodeMapping.get(sourceRow.get(Columns.SOURCE_ID, Long.class));
            if (prevNode != null && prevNode != currentNode) {
                CyEdge edge;
                CyRow edgeRow;
                AuxiliaryInformation edgeAux;
                if ((edge = getEdge(prevNode, currentNode)) == null) {
                    edge = network.addEdge(prevNode, currentNode, true);
                    edgeAux = new AuxiliaryInformation();
                    this.edgeInfo.put(edge, edgeAux);
                } else {
                    edgeRow = this.defaultEdgetable.getRow(edge.getSUID());
                    edgeRow.set(Columns.EDGE_TRAVERSALS, edgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class) + 1);
                    edgeAux = this.edgeInfo.get(edge);
                }
                edgeAux.addSourceRow(this.sourceTable, sourceRow.get(Columns.SOURCE_ID, Long.class).intValue() - 1);
            }
            prevNode = currentNode;
        }
    }

    public Trace findTrace(List<CyNode> nodes) {
        if (nodes.size() != 2) {
            return null;
        }
        var nodeA = nodes.get(0);
        var nodeB = nodes.get(1);

        var sourcesA = this.nodeInfo.get(nodeA).getSourceRows(this.sourceTable);
        var sourcesB = this.nodeInfo.get(nodeB).getSourceRows(this.sourceTable);

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

    public AuxiliaryInformation getNodeAux(CyNode node) {
        return this.nodeInfo.get(node);
    }

    public AuxiliaryInformation getEdgeAux(CyEdge edge) {
        return this.edgeInfo.get(edge);
    }
}
