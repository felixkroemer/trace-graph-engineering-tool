package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceExtension;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.model.UIState;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class TraceDetailsController implements PropertyChangeListener {

    private CyServiceRegistrar registrar;
    private CyNetwork network;
    private CyNetworkView networkView;
    private Map<CyNode, CyNode> nodeMapping;
    private TraceGraph traceGraph;
    private UIState uiState;
    private boolean liveUpdate;

    public TraceDetailsController(CyServiceRegistrar registrar, TraceGraph traceGraph, UIState uiState) {
        this.registrar = registrar;
        this.nodeMapping = new HashMap<>();
        this.traceGraph = traceGraph;
        this.uiState = uiState;
        this.liveUpdate = false;

        this.uiState.addObserver(this);
        this.createNetwork();
    }

    public void createNetwork() {
        CyNetworkFactory networkFactory = registrar.getService(CyNetworkFactory.class);
        this.network = networkFactory.createNetwork();
        CyNetworkManager networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.addNetwork(network);
        CyNetworkViewFactory networkViewFactory = registrar.getService(CyNetworkViewFactory.class);
        this.networkView = networkViewFactory.createNetworkView(network);
        CyNetworkViewManager networkViewManager = registrar.getService(CyNetworkViewManager.class);
        networkViewManager.addNetworkView(networkView, false);
        CyApplicationManager manager = registrar.getService(CyApplicationManager.class);
        manager.setSelectedNetworks(List.of(traceGraph.getNetwork(), this.network));
    }

    public void update() {
        this.updateTraces(this.uiState.getTraceSet());
    }

    public void updateTraces(Set<TraceExtension> traces) {
        this.nodeMapping.clear();
        this.network.removeNodes(this.network.getNodeList());

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
                nodeView.setVisualProperty(NODE_LABEL, startIndex != node.getValue1() ?
                        startIndex + " - " + node.getValue1() : "" + node.getValue1());
                nodeView.setVisualProperty(NODE_FILL_COLOR, trace.getColor());
                nodeView.setVisualProperty(NODE_WIDTH, 100.0);
                if (trace.getNode() == node.getValue0()) {
                    nodeView.setVisualProperty(NODE_BORDER_PAINT, Color.MAGENTA);
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


    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {

            //UIState
            case "traceSet" -> {
                if (this.liveUpdate) {
                    this.updateTraces((Set<TraceExtension>) evt.getNewValue());
                }
            }
        }
    }
}
