package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.util.CustomLoader;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.google.ortools.sat.*;
import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.javatuples.Pair;

import java.util.*;

public class TraceGraph {

    private CyNetwork network;
    private ParameterDiscretizationModel pdm;
    private Set<CyTable> sourceTables;
    private CyTable defaultNodeTable;
    private CyTable defaultEdgetable;
    // the currently selected trace
    private TraceExtension trace;

    // hash to node suid
    //TODO: map directly to node
    private Map<Long, Long> suidHashMapping;
    // source table to array of nodes
    // every source row index must always be mapped to an existing node in the graph
    private Map<CyTable, CyNode[]> nodeMapping;
    // node to node auxiliary information
    private Map<CyNode, AuxiliaryInformation> nodeInfo;
    private Map<CyEdge, AuxiliaryInformation> edgeInfo;

    public TraceGraph(CyNetwork network, ParameterDiscretizationModel pdm) {
        this.pdm = pdm;
        this.sourceTables = new HashSet<>();
        this.network = network;
        this.trace = null;

        // DEFAULT_ATTRS = Shared (root) + Local
        this.defaultNodeTable = this.network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
        this.defaultEdgetable = this.network.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS);
        this.suidHashMapping = this.pdm.getSuidHashMapping();
        this.nodeMapping = new HashMap<>();
        this.nodeInfo = new HashMap<>();
        this.edgeInfo = new HashMap<>();

        this.initTables();
    }

    private void initTables() {
        var localNodeTable = this.network.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS);
        localNodeTable.createColumn(Columns.NODE_VISITS, Integer.class, false);
        localNodeTable.createColumn(Columns.NODE_FREQUENCY, Integer.class, false);

        var localEdgeTable = this.network.getTable(CyEdge.class, CyNetwork.LOCAL_ATTRS);
        localEdgeTable.createColumn(Columns.EDGE_TRAVERSALS, Integer.class, false, 1);
    }

    public void init(CyTable sourceTable) {
        var localNodeTable = this.network.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS);
        var localEdgeTable = this.network.getTable(CyEdge.class, CyNetwork.LOCAL_ATTRS);
        this.sourceTables.add(sourceTable);
        this.nodeMapping.put(sourceTable, new CyNode[sourceTable.getRowCount() + 1]);

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
            if (suid != null && (currentNode = rootNetwork.getNode(suid)) != null) { // node for the given state
                // already exists, maybe only in root
                currentRow = localNodeTable.getRow(currentNode.getSUID());
                currentNodeInfo = this.nodeInfo.get(currentNode);
                // node exists in root network, but not here
                if (!network.containsNode(currentNode)) {
                    // make node available from the default node table
                    ((CySubNetwork) network).addNode(currentNode);
                    currentRow = localNodeTable.getRow(currentNode.getSUID());
                    currentRow.set(Columns.NODE_VISITS, 1);
                    currentRow.set(Columns.NODE_FREQUENCY, 1);
                    currentNodeInfo = new AuxiliaryInformation();
                    this.nodeInfo.put(currentNode, currentNodeInfo);
                }
                if (prevNode != currentNode) { // prevNode cannot be null here
                    currentRow.set(Columns.NODE_VISITS, currentRow.get(Columns.NODE_VISITS, Integer.class) + 1);
                } else {
                    currentRow.set(Columns.NODE_FREQUENCY, currentRow.get(Columns.NODE_FREQUENCY, Integer.class) + 1);
                }
            } else { // node does not exist yet
                currentNode = network.addNode();
                suidHashMapping.put(hash, currentNode.getSUID());
                currentRow = localNodeTable.getRow(currentNode.getSUID());
                for (int j = 0; j < this.pdm.getParameters().size(); j++) {
                    currentRow.set(this.pdm.getParameters().get(j).getName(), state[j]);
                }
                currentRow.set(Columns.NODE_VISITS, 1);
                currentRow.set(Columns.NODE_FREQUENCY, 1);
                currentNodeInfo = new AuxiliaryInformation();
                this.nodeInfo.put(currentNode, currentNodeInfo);
            }
            this.nodeMapping.get(sourceTable)[sourceRow.get(Columns.SOURCE_ID, Long.class).intValue()] = currentNode;
            currentNodeInfo.addSourceRow(sourceTable, sourceRow.get(Columns.SOURCE_ID, Long.class).intValue());
            if (prevNode != null && prevNode != currentNode) {
                CyEdge edge;
                AuxiliaryInformation edgeAux;
                if ((edge = getEdge(prevNode, currentNode)) == null) {
                    edge = network.addEdge(prevNode, currentNode, true);
                    edgeAux = new AuxiliaryInformation();
                    this.edgeInfo.put(edge, edgeAux);
                } else {
                    CyRow edgeRow = localEdgeTable.getRow(edge.getSUID());
                    edgeRow.set(Columns.EDGE_TRAVERSALS, edgeRow.get(Columns.EDGE_TRAVERSALS, Integer.class) + 1);
                    edgeAux = this.edgeInfo.get(edge);
                }
                edgeAux.addSourceRow(sourceTable, sourceRow.get(Columns.SOURCE_ID, Long.class).intValue() - 1);
            }
            prevNode = currentNode;
        }

        this.fixNetworkName();
    }

    /**
     * Called when a trace is added to or extracted from this Trace Graph.
     */
    public void fixNetworkName() {
        network.getRow(network).set(CyNetwork.NAME, Util.getSubNetworkName(sourceTables));
    }

    public TraceGraph extractTraceGraph(CyNetwork newNetwork, Set<CyTable> sourceTables) {
        TraceGraph traceGraph = new TraceGraph(newNetwork, this.pdm);
        for (CyTable table : sourceTables) {
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
            if (sourceTables.containsAll(info.getSourceTables())) {
                nodesToRemove.add(node);
                nodeInfo.remove(node);
            }
        }

        for (CyEdge edge : this.network.getEdgeList()) {
            var info = this.edgeInfo.get(edge);
            if (sourceTables.containsAll(info.getSourceTables())) {
                edgesToRemove.add(edge);
                edgeInfo.remove(edge);
            }
        }

        this.network.removeNodes(nodesToRemove);
        this.network.removeEdges(edgesToRemove);

        for (CyTable table : sourceTables) {
            this.nodeMapping.remove(table);
        }

        //TODO: improvement: do not use init, pass data directly
        for (CyTable table : sourceTables) {
            traceGraph.init(table);
        }

        fixNetworkName();

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

    public void reinit(Parameter changedParameter) {
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
                // old node, may contain source rows that dont belong to this node anymore (if bucket of source row
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
                            // node exists in root network, but not here
                            if (!network.containsNode(newNode)) {
                                // make node available from the default node table
                                ((CySubNetwork) network).addNode(newNode);
                                newNodeRow = defaultNodeTable.getRow(newNode.getSUID());
                                newNodeRow.set(Columns.NODE_VISITS, 1);
                                newNodeRow.set(Columns.NODE_FREQUENCY, 1);
                                newNodeAux = new AuxiliaryInformation();
                                this.nodeInfo.put(newNode, newNodeAux);
                            } else {
                                newNodeRow = defaultNodeTable.getRow(newNode.getSUID());
                                newNodeAux = this.nodeInfo.get(newNode);
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
                        iterator.remove();
                        oldNodeAux.getSourceRows(sourceTable).remove((Object) j);
                        newNodeAux.addSourceRow(sourceTable, j);
                        this.nodeMapping.get(sourceTable)[j] = newNode;
                    }
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
            if (this.nodeInfo.get(node).hasNoSourceRows()) {
                nodesToRemove.add(node);
                this.nodeInfo.remove(node);
            }
        }
        // need to be removed in batches, otherwise events take forever
        // TODO: check problem with rendering timer concurrency (trying to render nodes that were already deleted)
        this.network.removeNodes(nodesToRemove);
    }

    public void clearEdges() {
        this.network.removeEdges(this.network.getEdgeList());
        this.edgeInfo.clear();
        for(CyRow row : this.defaultNodeTable.getAllRows()) {
            row.set(Columns.NODE_VISITS, 1);
            row.set(Columns.NODE_FREQUENCY, 1);
        }
    }

    public void generateEdges() {
        CyNode prevNode = null;
        CyNode currentNode;
        CyRow currentNodeRow;
        for (CyTable sourceTable : this.sourceTables) {
            for (CyRow sourceRow : sourceTable.getAllRows()) {
                currentNode =
                        this.nodeMapping.get(sourceTable)[sourceRow.get(Columns.SOURCE_ID, Long.class).intValue()];
                currentNodeRow = defaultNodeTable.getRow(currentNode.getSUID());
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
                    edgeAux.addSourceRow(sourceTable, sourceRow.get(Columns.SOURCE_ID, Long.class).intValue() - 1);

                    currentNodeRow.set(Columns.NODE_VISITS, currentNodeRow.get(Columns.NODE_VISITS, Integer.class) + 1);
                } else {
                    currentNodeRow.set(Columns.NODE_FREQUENCY, currentNodeRow.get(Columns.NODE_FREQUENCY,
                            Integer.class) + 1);
                }
                prevNode = currentNode;
            }
        }
    }

    private boolean isFeasible(List<CyNode> nodes, CyTable sourceTable) {
        for (var node : nodes) {
            if (this.nodeInfo.get(node).getSourceRows(sourceTable) == null) {
                return false;
            }
        }
        return true;
    }

    public Trace findTraceCPSat(List<CyNode> nodes) {
        Trace shortestTrace = null;
        Trace trace = null;

        CustomLoader.loadNativeLibraries();

        for (CyTable sourceTable : this.sourceTables) {

            if (!isFeasible(nodes, sourceTable)) {
                continue;
            }

            trace = new Trace();

            BoolVar[] vars = new BoolVar[sourceTable.getRowCount()];
            BoolVar[] starts = new BoolVar[sourceTable.getRowCount()];

            CpModel model = new CpModel();
            for (int i = 0; i < sourceTable.getRowCount(); i++) {
                vars[i] = model.newBoolVar("" + i);
                starts[i] = model.newBoolVar("s" + i);
            }

            for (var node : nodes) {
                var sources = this.nodeInfo.get(node).getSourceRows(sourceTable);
                IntVar[] expressions = new IntVar[sources.size()];
                int i = 0;
                for (var x : sources) {
                    expressions[i] = vars[x - 1];
                    i = i + 1;
                }
                model.addGreaterOrEqual(LinearExpr.sum(expressions), 1);
            }

            for (int i = 1; i < vars.length; i++) {
                model.addLessOrEqual(LinearExpr.weightedSum(new Literal[]{vars[i], vars[i - 1], starts[i]},
                        new long[]{1, -1, -1}), 0);
            }
            model.addImplication(vars[0], starts[0]);

            model.addLessOrEqual(LinearExpr.sum(starts), 1);

            model.minimize(LinearExpr.sum(vars));

            // Create a solver and solve the model.
            CpSolver solver = new CpSolver();
            solver.getParameters().setCpModelPresolve(false);
            solver.getParameters().setLogSearchProgress(true);
            CpSolverStatus status = solver.solve(model);


            if (status == CpSolverStatus.OPTIMAL) {
                int start = -1;
                int end = vars.length - 1;
                for (int i = 0; i < vars.length; i++) {
                    if (solver.value(vars[i]) == 1) {
                        start = i;
                        continue;
                    }
                    if (start != -1 && solver.value(vars[i]) == 0) {
                        end = i - 1;
                        break;
                    }
                }

                for (int i = start; i <= end; i++) {
                    trace.addAfter(findNode(sourceTable, i), i);
                }

                if (shortestTrace == null || shortestTrace.getSequence().size() > trace.getSequence().size()) {
                    shortestTrace = trace;
                }

            } else {
                return null;
            }
        }


        return shortestTrace;
    }

    public Trace findTrace(List<CyNode> nodes) {
        if (nodes.size() == 2) {
            return findTraceNaive(nodes);
        } else {
            return findTraceCPSat(nodes);
        }
    }

    public Trace findTraceNaive(List<CyNode> nodes) {
        Trace shortestTrace = null;
        Trace trace = null;
        for (CyTable sourceTable : this.sourceTables) {

            if (!isFeasible(nodes, sourceTable)) {
                continue;
            }

            trace = new Trace();

            var nodeA = nodes.get(0);
            var nodeB = nodes.get(1);

            var sourcesA = this.nodeInfo.get(nodeA).getSourceRows(sourceTable);
            var sourcesB = this.nodeInfo.get(nodeB).getSourceRows(sourceTable);

            if (sourcesA == null || sourcesB == null) {
                continue;
            }

            Pair<Integer, Integer> window = null;
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
                trace.addAfter(findNode(sourceTable, i), i);
            }

            if (shortestTrace == null || shortestTrace.getSequence().size() > trace.getSequence().size()) {
                shortestTrace = trace;
            }

        }
        return shortestTrace;
    }

    public Map<String, String> getNodeInfo(CyNode node) {
        HashMap<String, String> map = new HashMap<>();
        var localNodeTable = this.network.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS);
        var row = localNodeTable.getRow(node.getSUID());
        map.put("Visits", "" + row.get(Columns.NODE_VISITS, Integer.class));
        map.put("Frequency", "" + row.get(Columns.NODE_FREQUENCY, Integer.class));
        return map;
    }

    public AuxiliaryInformation getNodeAux(CyNode node) {
        return this.nodeInfo.get(node);
    }

    public AuxiliaryInformation getEdgeAux(CyEdge edge) {
        return this.edgeInfo.get(edge);
    }

    public void setTrace(TraceExtension trace) {
        this.trace = trace;
    }

    public TraceExtension getTrace() {
        return this.trace;
    }
}
