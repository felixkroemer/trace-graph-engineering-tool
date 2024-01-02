package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CySubNetwork;

import java.util.*;

public class TraceGraph {

    private CyNetwork network;
    private ParameterDiscretizationModel pdm;
    private Set<CyTable> sourceTables;
    private CyTable defaultNodeTable;
    // hash to node suid
    private Map<Long, Long> suidHashMapping;
    // source table to array of nodes
    // every source row index must always be mapped to an existing node in the graph
    private Map<CyTable, CyNode[]> nodeMapping;
    // node to node auxiliary information
    private Map<CyNode, NodeAuxiliaryInformation> nodeInfo;
    private Map<CyEdge, EdgeAuxiliaryInformation> edgeInfo;

    public TraceGraph(CyNetwork network, ParameterDiscretizationModel pdm) {
        this.pdm = pdm;
        this.sourceTables = new HashSet<>();
        this.network = network;

        // DEFAULT_ATTRS = Shared (root) + Local
        this.defaultNodeTable = this.network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
        this.suidHashMapping = this.pdm.getSuidHashMapping();
        this.nodeMapping = new HashMap<>();
        this.nodeInfo = new HashMap<>();
        this.edgeInfo = new HashMap<>();
    }

    public void addSourceTable(CyTable sourceTable) {
        this.sourceTables.add(sourceTable);
        this.nodeMapping.put(sourceTable, new CyNode[sourceTable.getRowCount() + 1]);

        int[] state = new int[this.pdm.getParameterCount()];
        CyNode prevNode = null;
        CyNode currentNode;
        CyRow currentRow;
        NodeAuxiliaryInformation currentNodeInfo;
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
            if (suid != null && (currentNode = rootNetwork.getNode(suid)) != null) { // node for the given state
                // node exists in root network, but not here
                if (!network.containsNode(currentNode)) {
                    // make node available from the default node table
                    ((CySubNetwork) network).addNode(currentNode);
                    currentNodeInfo = new NodeAuxiliaryInformation();
                    this.nodeInfo.put(currentNode, currentNodeInfo);
                } else {
                    currentNodeInfo = this.nodeInfo.get(currentNode);
                }
            } else { // node does not exist yet
                currentNode = network.addNode();
                suidHashMapping.put(hash, currentNode.getSUID());
                currentRow = this.defaultNodeTable.getRow(currentNode.getSUID());
                for (int j = 0; j < this.pdm.getParameters().size(); j++) {
                    currentRow.set(this.pdm.getParameters().get(j).getName(), state[j]);
                }
                currentNodeInfo = new NodeAuxiliaryInformation();
                this.nodeInfo.put(currentNode, currentNodeInfo);
            }

            this.nodeMapping.get(sourceTable)[sourceRow.get(Columns.SOURCE_ID, Long.class).intValue()] = currentNode;
            currentNodeInfo.addSourceRow(sourceTable, sourceRow.get(Columns.SOURCE_ID, Long.class).intValue());

            if (prevNode != currentNode) {
                currentNodeInfo.incrementFrequency();
            } else {
                currentNodeInfo.incrementVisitDuration();
            }

            if (prevNode != null && prevNode != currentNode) {
                CyEdge edge;
                EdgeAuxiliaryInformation edgeAux;
                if ((edge = getEdge(prevNode, currentNode)) == null) {
                    edge = network.addEdge(prevNode, currentNode, true);
                    edgeAux = new EdgeAuxiliaryInformation();
                    this.edgeInfo.put(edge, edgeAux);
                } else {
                    edgeAux = this.edgeInfo.get(edge);
                    edgeAux.increaseTraversals();
                }
                edgeAux.addSourceRow(sourceTable, sourceRow.get(Columns.SOURCE_ID, Long.class).intValue() - 1);
            }
            prevNode = currentNode;
        }

        this.fixNetworkName();
        this.fixAux();
    }

    /**
     * Called when a trace is added to or extracted from this Trace Graph.
     */
    private void fixNetworkName() {
        network.getRow(network).set(CyNetwork.NAME, Util.getSubNetworkName(sourceTables));
    }

    public TraceGraph extractTraceGraph(CyNetwork newNetwork, Set<CyTable> sourceTablesToRemove) {
        TraceGraph traceGraph = new TraceGraph(newNetwork, this.pdm);
        for (CyTable table : sourceTablesToRemove) {
            if (!this.sourceTables.contains(table)) {
                throw new IllegalArgumentException("Source Table does not belong to this TraceGraph");
            } else {
                this.sourceTables.remove(table);
            }
        }

        Set<CyNode> nodesToRemove = new HashSet<>();
        Set<CyEdge> edgesToRemove = new HashSet<>();

        for (CyNode node : this.network.getNodeList()) {
            var info = this.nodeInfo.get(node);
            if (sourceTablesToRemove.containsAll(info.getSourceTables())) {
                nodesToRemove.add(node);
                nodeInfo.remove(node);
            } else {
                info.getSourceTables().removeIf(sourceTablesToRemove::contains);
            }
        }

        for (CyEdge edge : this.network.getEdgeList()) {
            var info = this.edgeInfo.get(edge);
            if (sourceTablesToRemove.containsAll(info.getSourceTables())) {
                edgesToRemove.add(edge);
                edgeInfo.remove(edge);
            } else {
                info.getSourceTables().removeIf(sourceTablesToRemove::contains);
            }
        }

        this.network.removeNodes(nodesToRemove);
        this.network.removeEdges(edgesToRemove);

        for (CyTable table : sourceTablesToRemove) {
            this.nodeMapping.remove(table);
        }

        //TODO: improvement: do not use init, pass data directly
        for (CyTable table : sourceTablesToRemove) {
            traceGraph.addSourceTable(table);
        }

        fixNetworkName();
        fixAux();

        return traceGraph;
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
    public CyNode findNode(CyTable sourceTable, int sourceTableIndex) {
        var arr = this.nodeMapping.get(sourceTable);
        return arr == null ? null : arr[sourceTableIndex];
    }

    public CyNetwork getNetwork() {
        return this.network;
    }

    public Set<CyTable> getSourceTables() {
        return this.sourceTables;
    }

    public ParameterDiscretizationModel getPDM() {
        return this.pdm;
    }

    public void refresh() {
        this.clearNodes();
        for (CyTable trace : this.sourceTables) {
            this.addSourceTable(trace);
        }
    }

    public void onParameterChanged(Parameter changedParameter) {
        clearEdges();
        int changedParameterIndex = -1;
        for (int j = 0; j < pdm.getParameterCount(); j++) {
            if (this.pdm.getParameters().get(j).equals(changedParameter)) {
                changedParameterIndex = j;
                break;
            }
        }
        int[] state = new int[this.pdm.getParameterCount()];
        for (var source : this.nodeMapping.entrySet()) {
            CyTable sourceTable = source.getKey();
            boolean[] visited = new boolean[sourceTable.getRowCount() + 1];
            for (int i = 1; i <= sourceTable.getRowCount(); i++) {
                if (visited[i]) continue;
                // old node, may contain source rows that don't belong to this node anymore (if bucket of source row
                // changes)
                // uses source row index to node map, not influenced by already changed parameter bins
                var oldNode = this.nodeMapping.get(sourceTable)[i];
                var oldNodeRow = this.defaultNodeTable.getRow(oldNode.getSUID());
                var oldNodeAux = this.nodeInfo.get(oldNode);
                for (int j = 0; j < pdm.getParameterCount(); j++) {
                    state[j] = oldNodeRow.get(this.pdm.getParameters().get(j).getName(), Integer.class);
                }
                int oldNodeBucket = oldNodeRow.get(changedParameter.getName(), Integer.class);
                // set of rows in the source table that map to the old node
                // also contains source row index i
                var iterator = this.nodeInfo.get(oldNode).getSourceRows(sourceTable).iterator();
                while (iterator.hasNext()) {
                    var j = iterator.next();
                    visited[j] = true;

                    var sourceRow = sourceTable.getRow((long) j);
                    double sourceRowValue = sourceRow.get(changedParameter.getName(), Double.class);
                    var bucket = changedParameter.isEnabled() ? findBucket(sourceRowValue, changedParameter) : 0;

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
                        NodeAuxiliaryInformation newNodeAux;
                        // node exists, maybe only in root network
                        var rootNetwork = ((CySubNetwork) network).getRootNetwork();
                        // may be null if suidHashMapping reference is stale; -> node may have been removed from all
                        // TraceGraphs of the pdm, reference is not cleaned up automatically
                        if (suid != null && (newNode = rootNetwork.getNode(suid)) != null) { // implies suid is not null
                            // node exists in root network, but not here
                            if (!network.containsNode(newNode)) {
                                // make node available from the local node table
                                ((CySubNetwork) network).addNode(newNode);
                                newNodeAux = new NodeAuxiliaryInformation();
                                this.nodeInfo.put(newNode, newNodeAux);
                            } else {
                                newNodeAux = this.nodeInfo.get(newNode);
                            }
                        } else {
                            newNode = network.addNode();
                            suidHashMapping.put(hash, newNode.getSUID());
                            newNodeRow = this.defaultNodeTable.getRow(newNode.getSUID());
                            for (int k = 0; k < this.pdm.getParameters().size(); k++) {
                                newNodeRow.set(this.pdm.getParameters().get(k).getName(), state[k]);
                            }
                            newNodeAux = new NodeAuxiliaryInformation();
                            this.nodeInfo.put(newNode, newNodeAux);
                        }
                        iterator.remove();
                        oldNodeAux.getSourceRows(sourceTable).remove(j);
                        newNodeAux.addSourceRow(sourceTable, j);
                        this.nodeMapping.get(sourceTable)[j] = newNode;
                    }
                }
            }
        }
        this.generateEdges();
        this.removeLeftoverNodes();
        this.fixAux();
    }

    private void fixAux() {
        for (CyNode node : this.network.getNodeList()) {
            this.nodeInfo.get(node).fixVisitDurationAndFrequency();
        }
        for (CyEdge edge : this.network.getEdgeList()) {
            this.edgeInfo.get(edge).fixTraversals();
        }
    }

    private void removeLeftoverNodes() {
        List<CyNode> nodesToRemove = new ArrayList<>();
        // if node source rows is empty there is no entry in node mapping that points to this node, it can be removed
        // from this network but may still exist in another network with the same pdm
        for (CyNode node : this.network.getNodeList()) {
            if (this.nodeInfo.get(node).hasNoSourceRows()) {
                nodesToRemove.add(node);
                this.nodeInfo.remove(node);
            }
        }
        // need to be removed in batches, otherwise events take forever
        // also removes edges
        this.network.removeNodes(nodesToRemove);
    }

    public void clearEdges() {
        this.network.removeEdges(this.network.getEdgeList());
        this.edgeInfo.clear();
    }

    public void clearNodes() {
        this.network.removeNodes(this.network.getNodeList());
        this.nodeInfo.clear();
        this.edgeInfo.clear();
    }

    public void generateEdges() {
        CyNode prevNode = null;
        CyNode currentNode;
        for (CyTable sourceTable : this.sourceTables) {
            for (CyRow sourceRow : sourceTable.getAllRows()) {
                currentNode =
                        this.nodeMapping.get(sourceTable)[sourceRow.get(Columns.SOURCE_ID, Long.class).intValue()];
                if (prevNode != null && prevNode != currentNode) {
                    CyEdge edge;
                    EdgeAuxiliaryInformation edgeAux;
                    if ((edge = getEdge(prevNode, currentNode)) == null) {
                        edge = network.addEdge(prevNode, currentNode, true);
                        edgeAux = new EdgeAuxiliaryInformation();
                        this.edgeInfo.put(edge, edgeAux);
                    } else {
                        edgeAux = this.edgeInfo.get(edge);
                    }
                    edgeAux.addSourceRow(sourceTable, sourceRow.get(Columns.SOURCE_ID, Long.class).intValue() - 1);
                }
                prevNode = currentNode;
            }
        }
    }

    public Trace findTrace(List<CyNode> nodes) {
        if (nodes.size() <= 2) {
            return TraceFindingAlgorithm.findTraceNaive(this, nodes);
        } else {
            return TraceFindingAlgorithm.findTraceEfficient(this, nodes);
        }
    }

    public Map<String, String> getNodeInfo(CyNode node) {
        HashMap<String, String> map = new HashMap<>();
        var aux = this.nodeInfo.get(node);
        map.put("Visit Duration", "" + aux.getVisitDuration());
        map.put("Frequency", "" + aux.getFrequency());
        return map;
    }

    public NodeAuxiliaryInformation getNodeAux(CyNode node) {
        return this.nodeInfo.get(node);
    }

    public EdgeAuxiliaryInformation getEdgeAux(CyEdge edge) {
        return this.edgeInfo.get(edge);
    }
}
