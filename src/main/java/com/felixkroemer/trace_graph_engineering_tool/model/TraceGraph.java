package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import org.apache.commons.lang3.time.StopWatch;
import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.undo.UndoSupport;

import java.util.*;

public class TraceGraph {

    private CyNetwork network;
    private ParameterDiscretizationModel pdm;
    private Set<CyTable> traces;
    private CyTable defaultNodeTable;
    // hash to node suid
    private Map<Long, Long> suidHashMapping;
    // source table to array of nodes
    // every situation index must always be mapped to an existing node in the graph
    private Map<CyTable, CyNode[]> nodeMapping;
    // node to node auxiliary information
    private Map<CyNode, NodeAuxiliaryInformation> nodeInfo;
    private Map<CyEdge, EdgeAuxiliaryInformation> edgeInfo;
    private CyServiceRegistrar registrar;

    public TraceGraph(CyServiceRegistrar registrar, CyNetwork network, ParameterDiscretizationModel pdm) {
        this.pdm = pdm;
        this.traces = new HashSet<>();
        this.network = network;

        // DEFAULT_ATTRS = Shared (root) + Local
        this.defaultNodeTable = this.network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
        this.suidHashMapping = this.pdm.getSuidHashMapping();
        this.nodeMapping = new HashMap<>();
        this.nodeInfo = new HashMap<>();
        this.edgeInfo = new HashMap<>();
        this.registrar = registrar;
    }

    public CyNode getOrCreateNode(int[] state) {
        long hash = Arrays.hashCode(state);
        Long suid = suidHashMapping.get(hash);
        CyNode currentNode;
        NodeAuxiliaryInformation currentNodeInfo;
        var rootNetwork = ((CySubNetwork) network).getRootNetwork();
        if (suid != null && (currentNode = rootNetwork.getNode(suid)) != null) { // node for the given state
            // node exists in root network, but not here
            if (!network.containsNode(currentNode)) {
                Profiler.getInstance().addNodeNotFoundInSubnetwork();
                // make node available from the default node table
                ((CySubNetwork) network).addNode(currentNode);
                currentNodeInfo = new NodeAuxiliaryInformation();
                this.nodeInfo.put(currentNode, currentNodeInfo);
            } else {
                Profiler.getInstance().addNodeFound();
            }
        } else { // node does not exist yet
            Profiler.getInstance().addNodeNotFoundInRootNetwork();
            currentNode = network.addNode();
            suidHashMapping.put(hash, currentNode.getSUID());
            var currentRow = this.pdm.getRootNetwork().getSharedNodeTable().getRow(currentNode.getSUID());
            for (int j = 0; j < this.pdm.getParameters().size(); j++) {
                currentRow.set(this.pdm.getParameters().get(j).getName(), state[j]);
            }
            currentNodeInfo = new NodeAuxiliaryInformation();
            this.nodeInfo.put(currentNode, currentNodeInfo);
        }
        return currentNode;
    }

    public void addTrace(CyTable trace) {
        StopWatch watch = new StopWatch();
        watch.start();
        this.traces.add(trace);
        this.nodeMapping.put(trace, new CyNode[trace.getRowCount() + 1]);

        int[] state = new int[this.pdm.getParameterCount()];
        CyNode prevNode = null;
        for (CyRow situation : trace.getAllRows()) {
            Map<String, Object> values = situation.getAllValues();
            int i = 0;
            for (Parameter param : pdm.getParameters()) {
                if (param.isEnabled()) {
                    state[i] = findBucket((Double) values.get(param.getName()), param);
                } else {
                    state[i] = 0;
                }
                i++;
            }

            CyNode currentNode = getOrCreateNode(state);
            NodeAuxiliaryInformation currentNodeInfo = this.nodeInfo.get(currentNode);

            this.nodeMapping.get(trace)[situation.get(Columns.SOURCE_ID, Long.class).intValue()] = currentNode;
            currentNodeInfo.addSituation(trace, situation.get(Columns.SOURCE_ID, Long.class).intValue());
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
                edgeAux.addSituation(trace, situation.get(Columns.SOURCE_ID, Long.class).intValue() - 1);
            }
            prevNode = currentNode;
        }

        this.fixNetworkName();

        long result = watch.getTime();
        Profiler.getInstance().setAddTraceResult(result);
    }

    /**
     * Called when a trace is added to or extracted from this Trace Graph.
     */
    private void fixNetworkName() {
        network.getRow(network).set(CyNetwork.NAME, Util.getSubNetworkName(traces));
    }

    public TraceGraph extractTraceGraph(CyNetwork newNetwork, Set<CyTable> tracesToRemove) {
        for (CyTable table : tracesToRemove) {
            if (!this.traces.contains(table)) {
                throw new IllegalArgumentException("Source Table does not belong to this TraceGraph");
            } else {
                this.traces.remove(table);
            }
        }

        Set<CyNode> nodesToRemove = new HashSet<>();
        Set<CyEdge> edgesToRemove = new HashSet<>();

        for (CyNode node : this.network.getNodeList()) {
            var info = this.nodeInfo.get(node);
            if (tracesToRemove.containsAll(info.getTraces())) {
                nodesToRemove.add(node);
                nodeInfo.remove(node);
            } else {
                for (var trace : info.getTraces()) {
                    if (tracesToRemove.contains(trace)) {
                        info.removeTrace(trace);
                    }
                }
            }
        }

        for (CyEdge edge : this.network.getEdgeList()) {
            var info = this.edgeInfo.get(edge);
            if (tracesToRemove.containsAll(info.getTraces())) {
                edgesToRemove.add(edge);
                edgeInfo.remove(edge);
            } else {
                for (var trace : info.getTraces()) {
                    if (tracesToRemove.contains(trace)) {
                        info.removeTrace(trace);
                    }
                }
            }
        }

        this.network.removeNodes(nodesToRemove);
        this.network.removeEdges(edgesToRemove);

        for (CyTable table : tracesToRemove) {
            this.nodeMapping.remove(table);
        }

        fixNetworkName();
        fixAux();
        registrar.getService(UndoSupport.class).reset();

        TraceGraph traceGraph = new TraceGraph(this.registrar, newNetwork, this.pdm);
        //TODO: improvement: do not use init, pass data directly
        for (CyTable table : tracesToRemove) {
            traceGraph.addTrace(table);
        }

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
     * Find the node that a row in the trace belongs to
     */
    public CyNode findNode(CyTable trace, int traceIndex) {
        var arr = this.nodeMapping.get(trace);
        return arr == null ? null : arr[traceIndex];
    }

    public CyNetwork getNetwork() {
        return this.network;
    }

    public Set<CyTable> getTraces() {
        return this.traces;
    }

    public ParameterDiscretizationModel getPDM() {
        return this.pdm;
    }

    public void refresh() {
        this.clearNodes();
        for (CyTable trace : this.traces) {
            this.addTrace(trace);
        }
    }

    public void onParameterChangedInefficient(Parameter changedParameter) {
        StopWatch watch = new StopWatch();
        watch.start();
        this.refresh();
        long result = watch.getTime();
        Profiler.getInstance().setUpdateTraceGraphResult(result);
    }

    public void onParameterChangedSemiEfficient(Parameter changedParameter) {
        StopWatch watch = new StopWatch();
        watch.start();
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
            CyTable trace = source.getKey();
            for (int i = 1; i <= trace.getRowCount(); i++) {
                // old node, may contain situations that don't belong to this node anymore (if bucket of situation
                // changes)
                // uses situation index to node map, not influenced by already changed parameter bins
                var oldNode = this.nodeMapping.get(trace)[i];
                var oldNodeRow = this.pdm.getRootNetwork().getSharedNodeTable().getRow(oldNode.getSUID());
                var oldNodeAux = this.nodeInfo.get(oldNode);
                int oldNodeBucket = oldNodeRow.get(changedParameter.getName(), Integer.class);
                var situation = trace.getRow((long) i);
                double situationValue = situation.get(changedParameter.getName(), Double.class);
                var bucket = changedParameter.isEnabled() ? findBucket(situationValue, changedParameter) : 0;

                // situation j does not belong to this node anymore
                // create state of j, hash it, check if a node with that hash exists, add j to it,
                // otherwise create a new node with the state of j
                // move ingoing and outgoing edges belonging to j from oldNode to newNode
                if (bucket != oldNodeBucket) {
                    Profiler.getInstance().addImpactedSituation();
                    for (int k = 0; k < pdm.getParameterCount(); k++) {
                        state[k] = oldNodeRow.get(this.pdm.getParameters().get(k).getName(), Integer.class);
                    }
                    state[changedParameterIndex] = bucket;
                    CyNode newNode = getOrCreateNode(state);
                    NodeAuxiliaryInformation newNodeAux = this.nodeInfo.get(newNode);
                    oldNodeAux.getSituations(trace).remove((Object) i);
                    newNodeAux.addSituation(trace, i);
                    this.nodeMapping.get(trace)[i] = newNode;
                }
            }
        }
        this.removeLeftoverNodes();
        this.generateEdges();
        this.fixAux();
        registrar.getService(UndoSupport.class).reset();

        long result = watch.getTime();
        Profiler.getInstance().setUpdateTraceGraphResult(result);
    }

    private void fixAux() {
        for (CyNode node : this.network.getNodeList()) {
            this.nodeInfo.get(node).fixVisitDurationAndFrequency();
        }
    }

    private void removeLeftoverNodes() {
        List<CyNode> nodesToRemove = new ArrayList<>();
        // if node situations is empty there is no entry in node mapping that points to this node, it can be removed
        // from this network but may still exist in another network with the same pdm
        for (CyNode node : this.network.getNodeList()) {
            if (this.nodeInfo.get(node).hasNoSituations()) {
                nodesToRemove.add(node);
                this.nodeInfo.remove(node);
            }
        }
        Profiler.getInstance().setLeftOverNodes(nodesToRemove.size());
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
        registrar.getService(UndoSupport.class).reset();
        this.nodeInfo.clear();
        this.edgeInfo.clear();
    }

    public void generateEdges() {
        CyNode prevNode;
        CyNode currentNode;
        for (CyTable trace : this.traces) {
            prevNode = null;
            for (CyRow situation : trace.getAllRows()) {
                currentNode = this.nodeMapping.get(trace)[situation.get(Columns.SOURCE_ID, Long.class).intValue()];
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
                    edgeAux.addSituation(trace, situation.get(Columns.SOURCE_ID, Long.class).intValue() - 1);
                }
                prevNode = currentNode;
            }
        }
    }

    public Map<CyNode, NodeAuxiliaryInformation> createNodeInfoSnapshot() {
        Map<CyNode, NodeAuxiliaryInformation> snapshot = new HashMap<>();
        for (var entry : this.nodeInfo.entrySet()) {
            snapshot.put(entry.getKey(), new NodeAuxiliaryInformation(entry.getValue()));
        }
        return snapshot;
    }

    public SubTrace findTrace(List<CyNode> nodes) {
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
