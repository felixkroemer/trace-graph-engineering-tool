package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.display_manager.Trace;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskManager;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class TraceDetailsController {

    private CyServiceRegistrar registrar;
    private CyNetwork network;
    private CyNetworkView networkView;
    private Map<CyNode, CyNode> nodeMapping;

    public TraceDetailsController(CyServiceRegistrar registrar) {
        this.registrar = registrar;
        this.nodeMapping = new HashMap<>();
    }

    public void createNetwork() {
        CyNetworkFactory networkFactory = registrar.getService(CyNetworkFactory.class);
        this.network = networkFactory.createNetwork();
        CyNetworkManager networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.addNetwork(network);
        CyNetworkViewFactory networkViewFactory = registrar.getService(CyNetworkViewFactory.class);
        this.networkView = networkViewFactory.createNetworkView(network);
        CyNetworkViewManager networkViewManager = registrar.getService(CyNetworkViewManager.class);
        networkViewManager.addNetworkView(networkView);
    }

    public void showTraces(Set<Trace> traces) {
        this.nodeMapping.clear();
        if (this.network == null) {
            // view is set automatically as current network view
            this.createNetwork();
        } else {
            this.network.removeNodes(this.network.getNodeList());
            var manager = this.registrar.getService(CyApplicationManager.class);
            manager.setCurrentNetworkView(this.networkView);
        }

        var eventHelper = registrar.getService(CyEventHelper.class);

        for (var trace : traces) {
            CyNode prevTraceNode = null;
            for (int i = 0; i < trace.getSequence().size(); i++) {
                var node = trace.getSequence().get(i);
                var startIndex = node.getValue1();
                while (i < trace.getSequence().size() - 1 && trace.getSequence().get(i + 1).getValue0() == node.getValue0()) {
                    node = trace.getSequence().get(i + 1);
                    i++;
                }
                var traceNode = network.addNode();
                eventHelper.flushPayloadEvents();
                this.nodeMapping.put(traceNode, node.getValue0());
                var nodeView = networkView.getNodeView(traceNode);
                nodeView.setLockedValue(NODE_LABEL, startIndex != node.getValue1() ?
                        startIndex + " - " + node.getValue1() : "" + node.getValue1());
                nodeView.setLockedValue(NODE_FILL_COLOR, Color.WHITE);
                nodeView.setLockedValue(NODE_WIDTH, 100.0);
                if (trace.getNode() == node.getValue0()) {
                    nodeView.setLockedValue(NODE_BORDER_PAINT, Color.MAGENTA);
                }
                if (prevTraceNode != null) {
                    network.addEdge(prevTraceNode, traceNode, true);
                }
                prevTraceNode = traceNode;
            }
        }

        applyWorkingLayout(networkView);
    }

    public void applyWorkingLayout(CyNetworkView view) {
        var layoutManager = registrar.getService(CyLayoutAlgorithmManager.class);
        CyLayoutAlgorithm layoutFactory = layoutManager.getLayout("hierarchical");
        Object context = layoutFactory.getDefaultLayoutContext();
        var taskIterator = layoutFactory.createTaskIterator(view, context, CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
        TaskManager<?, ?> manager = registrar.getService(TaskManager.class);
        manager.execute(taskIterator);
    }

    public CyNetwork getNetwork() {
        return this.network;
    }

    public CyNode findCorrespondingNode(CyNode node) {
        return this.nodeMapping.get(node);
    }
}
