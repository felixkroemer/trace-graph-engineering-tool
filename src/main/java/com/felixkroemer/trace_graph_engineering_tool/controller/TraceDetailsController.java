package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceExtension;
import com.felixkroemer.trace_graph_engineering_tool.model.UIState;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
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
import java.util.Map;
import java.util.Set;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class TraceDetailsController implements PropertyChangeListener {

    private CyServiceRegistrar registrar;
    private CyNetwork network;
    private CyNetworkView networkView;
    private Map<CyNode, CyNode> nodeMapping;
    private UIState uiState;
    private boolean liveUpdate;

    public TraceDetailsController(CyServiceRegistrar registrar) {
        this.registrar = registrar;
        this.nodeMapping = new HashMap<>();
        this.liveUpdate = false;
    }

    public void setUiState(UIState uiState) {
        if (this.uiState != null) {
            this.uiState.removeObserver(this);
        }
        this.uiState = uiState;
        this.uiState.addObserver(this);
    }

    public void createAndRegisterNetwork() {
        CyNetworkFactory networkFactory = registrar.getService(CyNetworkFactory.class);
        this.network = networkFactory.createNetwork();

        this.network.getRow(this.network).set(CyRootNetwork.SHARED_NAME, "Trace Graph Helper Networks");
        this.network.getRow(this.network).set(CyNetwork.NAME, "Trace Details Network");

        var localNetworkTable = this.network.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS);
        localNetworkTable.createColumn(Columns.NETWORK_TRACE_DETAILS_MARKER, Integer.class, true);

        CyNetworkManager networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.addNetwork(network);
        CyNetworkViewFactory networkViewFactory = registrar.getService(CyNetworkViewFactory.class);
        this.networkView = networkViewFactory.createNetworkView(network);
        CyNetworkViewManager networkViewManager = registrar.getService(CyNetworkViewManager.class);
        networkViewManager.addNetworkView(networkView, false);
    }

    public void update() {
        this.updateTraces(this.uiState.getTraceSet());
    }

    public CyNode findCorrespondingNode(CyNode node) {
        return this.nodeMapping.get(node);
    }

    public CyNetwork getCorrespondingNetwork() {
        return this.uiState.getTraceSetNetwork();
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
