package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentComparisonControllerEvent;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;

import java.awt.*;
import java.util.Properties;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class NetworkComparisonController extends NetworkController implements SetCurrentNetworkListener {
    private CyNetwork networkA;
    private CyNetwork networkB;
    private CySubNetwork network;
    private CyNetworkView view;

    private boolean baseOnlyVisible;
    private boolean deltaOnlyVisible;
    private boolean baseDeltaVisible;


    private VisualStyle defaultVisualStyle;

    public NetworkComparisonController(CyNetwork networkA, CyNetwork networkB, CySubNetwork network,
                                       CyServiceRegistrar registrar) {
        super(registrar, network);
        this.networkA = networkA;
        this.networkB = networkB;
        this.network = network;
        this.baseOnlyVisible = true;
        this.deltaOnlyVisible = true;
        this.baseDeltaVisible = true;
        this.defaultVisualStyle = createDefaultVisualStyle();

        this.registrar.registerService(this, SetCurrentNetworkListener.class, new Properties());

        this.initTables();
        //TODO: maybe add more efficient batching version
        for (CyNode node : this.networkA.getNodeList()) {
            this.network.addNode(node);
        }
        for (CyEdge edge : this.networkA.getEdgeList()) {
            this.network.addEdge(edge);
        }
        for (CyNode node : this.networkB.getNodeList()) {
            this.network.addNode(node);
        }
        for (CyEdge edge : this.networkB.getEdgeList()) {
            if (!this.network.containsEdge(edge.getSource(), edge.getTarget())) this.network.addEdge(edge);
        }

        var defaultNodeTable = this.network.getDefaultNodeTable();
        var defaultEdgeTable = this.network.getDefaultEdgeTable();
        for (CyNode node : this.network.getNodeList()) {
            boolean base = this.networkA.containsNode(node);
            boolean delta = this.networkB.containsNode(node);
            var row = defaultNodeTable.getRow(node.getSUID());
            row.set(Columns.COMPARISON_GROUP_MEMBERSHIP, base && delta ? 2 : (base ? 0 : 1));
        }
        for (CyEdge edge : this.network.getEdgeList()) {
            boolean base = this.networkA.containsEdge(edge);
            boolean delta = this.networkB.containsEdge(edge);
            var row = defaultEdgeTable.getRow(edge.getSUID());
            row.set(Columns.COMPARISON_GROUP_MEMBERSHIP, base && delta ? 2 : (base ? 0 : 1));
        }

        this.initView();

        var mapper = registrar.getService(VisualMappingManager.class);
        mapper.setVisualStyle(this.defaultVisualStyle, this.view);
        this.applyStyleAndLayout();
    }

    public void initTables() {
        var localNodeTable = this.network.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS);
        var localEdgeTable = this.network.getTable(CyEdge.class, CyNetwork.LOCAL_ATTRS);
        localNodeTable.createColumn(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class, false);
        localEdgeTable.createColumn(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class, false);

        //TODO map columns of nodes that are compared to the node table of this network;
    }

    @Override
    public CyNetworkView getView() {
        return this.view;
    }

    @Override
    public VisualStyle getVisualStyle() {
        return this.defaultVisualStyle;
    }

    @Override
    public void destroy() {
        this.registrar.unregisterService(this, SetCurrentNetworkListener.class);
    }

    @Override
    public void updateNetwork(Parameter parameter) {
        //TODO
    }

    private VisualStyle createDefaultVisualStyle() {
        var VisualStyleFactory = registrar.getService(org.cytoscape.view.vizmap.VisualStyleFactory.class);
        VisualStyle style = VisualStyleFactory.createVisualStyle("default-comparison");

        VisualMappingFunctionFactory visualMappingFunctionFactory =
                registrar.getService(VisualMappingFunctionFactory.class, "(mapping.type=discrete)");

        DiscreteMapping<Integer, Paint> nodeColorMapping =
                (DiscreteMapping<Integer, Paint>) visualMappingFunctionFactory.createVisualMappingFunction(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class, BasicVisualLexicon.NODE_FILL_COLOR);
        nodeColorMapping.putMapValue(0, Color.GREEN);
        nodeColorMapping.putMapValue(1, Color.RED);
        nodeColorMapping.putMapValue(2, Color.BLUE);
        style.addVisualMappingFunction(nodeColorMapping);

        DiscreteMapping<Integer, Paint> edgeColorMapping =
                (DiscreteMapping<Integer, Paint>) visualMappingFunctionFactory.createVisualMappingFunction(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class, EDGE_STROKE_UNSELECTED_PAINT);
        edgeColorMapping.putMapValue(0, Color.GREEN);
        edgeColorMapping.putMapValue(1, Color.RED);
        edgeColorMapping.putMapValue(2, Color.BLUE);
        style.addVisualMappingFunction(edgeColorMapping);

        style.setDefaultValue(NODE_SIZE, 10.0);
        style.setDefaultValue(EDGE_VISIBLE, false);
        style.setDefaultValue(EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);

        return style;
    }

    public void initView() {
        var manager = this.registrar.getService(CyApplicationManager.class);
        var tgNetworkViewRenderer = manager.getNetworkViewRenderer("org.cytoscape.ding-extension");
        var networkViewFactory = tgNetworkViewRenderer.getNetworkViewFactory();
        this.view = networkViewFactory.createNetworkView(this.network);
    }

    public void hideBO() {
        this.baseOnlyVisible = false;
        this.updateVisibility();
    }

    public void updateVisibility() {
        for (var nodeView : this.view.getNodeViews()) {
            var node = nodeView.getModel();
            var group = this.network.getRow(node).get(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class);
            switch (group) {
                case 2 -> {
                    nodeView.setVisualProperty(NODE_VISIBLE, baseDeltaVisible);
                }
                case 1 -> {
                    nodeView.setVisualProperty(NODE_VISIBLE, deltaOnlyVisible);
                }
                case 0 -> {
                    nodeView.setVisualProperty(NODE_VISIBLE, baseOnlyVisible);
                }
            }
        }
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        if (e.getNetwork() == this.getNetwork()) {
            var eventHelper = this.registrar.getService(CyEventHelper.class);
            eventHelper.fireEvent(new SetCurrentComparisonControllerEvent(this, this));
        }
    }
}
