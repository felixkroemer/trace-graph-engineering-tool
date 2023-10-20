package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentComparisonControllerEvent;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class NetworkComparisonController extends NetworkController implements SetCurrentNetworkListener {

    public static final String BO = "BO";
    public static final String DO = "DO";
    public static final String BD = "BD";

    private CyNetwork base;
    private CyNetwork delta;
    private CySubNetwork network;
    private CyNetworkView view;

    private boolean nodesBaseOnlyVisible;
    private boolean nodesDeltaOnlyVisible;
    private boolean nodesBaseDeltaVisible;
    private boolean edgesBaseOnlyVisible;
    private boolean edgesDeltaOnlyVisible;
    private boolean edgesBaseDeltaVisible;

    private VisualStyle defaultVisualStyle;

    public NetworkComparisonController(CyNetwork base, CyNetwork delta, CySubNetwork network,
                                       ParameterDiscretizationModel pdm, CyServiceRegistrar registrar) {
        super(registrar, network, pdm);
        this.base = base;
        this.delta = delta;
        this.network = network;
        this.nodesBaseOnlyVisible = true;
        this.nodesDeltaOnlyVisible = true;
        this.nodesBaseDeltaVisible = true;
        this.edgesBaseOnlyVisible = true;
        this.edgesDeltaOnlyVisible = true;
        this.edgesBaseDeltaVisible = true;
        this.defaultVisualStyle = createDefaultVisualStyle();

        this.registrar.registerService(this, SetCurrentNetworkListener.class, new Properties());

        this.initTables();
        //TODO: maybe add more efficient batching version
        for (CyNode node : this.base.getNodeList()) {
            this.network.addNode(node);
        }
        for (CyEdge edge : this.base.getEdgeList()) {
            this.network.addEdge(edge);
        }
        for (CyNode node : this.delta.getNodeList()) {
            this.network.addNode(node);
        }
        for (CyEdge edge : this.delta.getEdgeList()) {
            if (!this.network.containsEdge(edge.getSource(), edge.getTarget())) this.network.addEdge(edge);
        }

        var defaultNodeTable = this.network.getDefaultNodeTable();
        var defaultEdgeTable = this.network.getDefaultEdgeTable();
        for (CyNode node : this.network.getNodeList()) {
            boolean inBase = this.base.containsNode(node);
            boolean inDelta = this.delta.containsNode(node);
            var row = defaultNodeTable.getRow(node.getSUID());
            row.set(Columns.COMPARISON_GROUP_MEMBERSHIP, (inBase && inDelta) ? 2 : (inBase ? 0 : 1));
        }
        // edges are not unique, they are not defined by their source, target, multiedges are possible
        //TODO: fix: edges 
        for (CyEdge edge : this.network.getEdgeList()) {
            boolean inBase = this.base.containsEdge(edge.getSource(), edge.getTarget());
            boolean inDelta = this.delta.containsEdge(edge.getSource(), edge.getTarget());
            var row = defaultEdgeTable.getRow(edge.getSUID());
            row.set(Columns.COMPARISON_GROUP_MEMBERSHIP, (inBase && inDelta) ? 2 : (inBase ? 0 : 1));
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

    public void updateVisibility() {
        for (var edgeView : this.view.getEdgeViews()) {
            var edge = edgeView.getModel();
            var group = this.network.getRow(edge).get(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class);
            switch (group) {
                case 2 -> {
                    edgeView.setVisualProperty(EDGE_VISIBLE, edgesBaseDeltaVisible);
                }
                case 1 -> {
                    edgeView.setVisualProperty(EDGE_VISIBLE, edgesDeltaOnlyVisible);
                }
                case 0 -> {
                    edgeView.setVisualProperty(EDGE_VISIBLE, edgesBaseOnlyVisible);
                }
            }
        }
        for (var nodeView : this.view.getNodeViews()) {
            var node = nodeView.getModel();
            var group = this.network.getRow(node).get(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class);
            switch (group) {
                case 2 -> {
                    nodeView.setVisualProperty(NODE_VISIBLE, nodesBaseDeltaVisible);
                }
                case 1 -> {
                    nodeView.setVisualProperty(NODE_VISIBLE, nodesDeltaOnlyVisible);
                }
                case 0 -> {
                    nodeView.setVisualProperty(NODE_VISIBLE, nodesBaseOnlyVisible);
                }
            }
        }
    }

    public Map<String, String> getNodeInfo(CyNode node) {
        HashMap<String, String> map = new HashMap<>();
        var group = this.network.getRow(node).get(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class);
        map.put("Group", group == 2 ? "BD" : (group == 1 ? "DO" : "BO"));
        return map;
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        if (e.getNetwork() == this.getNetwork()) {
            var eventHelper = this.registrar.getService(CyEventHelper.class);
            eventHelper.fireEvent(new SetCurrentComparisonControllerEvent(this, this));
        }
    }

    public void setGroupVisibiliy(String group, boolean node, boolean visible) {
        switch (group) {
            case BO -> {
                if (node) {
                    this.nodesBaseOnlyVisible = visible;
                } else {
                    this.edgesBaseOnlyVisible = visible;
                }
            }
            case DO -> {
                if (node) {
                    this.nodesDeltaOnlyVisible = visible;
                } else {
                    this.edgesDeltaOnlyVisible = visible;
                }
            }
            case BD -> {
                if (node) {
                    this.nodesBaseDeltaVisible = visible;
                } else {
                    this.edgesBaseDeltaVisible = visible;
                }
            }
        }
        this.updateVisibility();
    }
}
