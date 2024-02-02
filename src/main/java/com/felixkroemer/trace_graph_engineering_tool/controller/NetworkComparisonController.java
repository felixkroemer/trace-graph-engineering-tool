package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentComparisonControllerEvent;
import com.felixkroemer.trace_graph_engineering_tool.mappings.ComparisonSizeMapping;
import com.felixkroemer.trace_graph_engineering_tool.mappings.TooltipMapping;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.NodeAuxiliaryInformation;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.CustomTreeTableModel;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.MultiObjectTreeTableNode;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableModel;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class NetworkComparisonController extends NetworkController implements SetCurrentNetworkListener {

    public static final String BO = "BO";
    public static final String DO = "DO";
    public static final String BD = "BD";
    private final CySubNetwork network;
    private CyNetworkView view;
    private boolean nodesBaseOnlyVisible;
    private boolean nodesDeltaOnlyVisible;
    private boolean nodesBaseDeltaVisible;
    private boolean edgesBaseOnlyVisible;
    private boolean edgesDeltaOnlyVisible;
    private boolean edgesBaseDeltaVisible;
    private TreeTableModel treeTableModel;
    private Map<CyNode, NodeAuxiliaryInformation> baseNodeInfoSnapshot;
    private Map<CyNode, NodeAuxiliaryInformation> deltaNodeInfoSnapshot;
    private final VisualStyle defaultVisualStyle;

    public NetworkComparisonController(TraceGraphController baseController, TraceGraphController deltaController,
                                       CySubNetwork network, ParameterDiscretizationModel pdm,
                                       CyServiceRegistrar registrar) {
        super(registrar, network, pdm);
        this.network = network;
        this.nodesBaseOnlyVisible = true;
        this.nodesDeltaOnlyVisible = true;
        this.nodesBaseDeltaVisible = true;
        this.edgesBaseOnlyVisible = true;
        this.edgesDeltaOnlyVisible = true;
        this.edgesBaseDeltaVisible = true;
        this.baseNodeInfoSnapshot = baseController.getTraceGraph().createNodeInfoSnapshot();
        this.deltaNodeInfoSnapshot = deltaController.getTraceGraph().createNodeInfoSnapshot();
        this.treeTableModel = this.initializeTreeTableModel(baseController.createNetworkTableModel(), deltaController.createNetworkTableModel());
        this.defaultVisualStyle = createDefaultVisualStyle(baseController, deltaController);

        this.registrar.registerService(this, SetCurrentNetworkListener.class, new Properties());

        this.initTables();

        var base = baseController.getNetwork();
        var delta = deltaController.getNetwork();

        //TODO: maybe add more efficient batching version
        for (CyNode node : base.getNodeList()) {
            this.network.addNode(node);
        }
        for (CyEdge edge : base.getEdgeList()) {
            this.network.addEdge(edge);
        }
        for (CyNode node : delta.getNodeList()) {
            this.network.addNode(node);
        }
        for (CyEdge edge : delta.getEdgeList()) {
            if (Util.getEdge(network, edge.getSource(), edge.getTarget()) == null)
                this.network.addEdge(edge);
        }

        var defaultNodeTable = this.network.getDefaultNodeTable();
        var defaultEdgeTable = this.network.getDefaultEdgeTable();
        for (CyNode node : this.network.getNodeList()) {
            boolean inBase = base.containsNode(node);
            boolean inDelta = delta.containsNode(node);
            var row = defaultNodeTable.getRow(node.getSUID());
            row.set(Columns.COMPARISON_GROUP_MEMBERSHIP, (inBase && inDelta) ? 2 : (inBase ? 0 : 1));
        }
        // edges are not unique, they are not defined by their source, target, multiedges are possible
        for (CyEdge edge : this.network.getEdgeList()) {
            boolean inBase = Util.getEdge(base, edge.getSource(), edge.getTarget()) != null;
            boolean inDelta = Util.getEdge(delta, edge.getSource(), edge.getTarget()) != null;
            var row = defaultEdgeTable.getRow(edge.getSUID());
            row.set(Columns.COMPARISON_GROUP_MEMBERSHIP, (inBase && inDelta) ? 2 : (inBase ? 0 : 1));
        }

        this.initView();

        var mapper = registrar.getService(VisualMappingManager.class);
        mapper.setVisualStyle(this.defaultVisualStyle, this.view);

        this.registerNetwork();
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
    public void dispose() {
        this.registrar.unregisterService(this, SetCurrentNetworkListener.class);
        var visualMappingManager = registrar.getService(VisualMappingManager.class);
        visualMappingManager.removeVisualStyle(this.defaultVisualStyle);
    }

    @Override
    public void updateNetwork(Parameter parameter) {
    }

    private void createTraceTableModel(CyNode node, DefaultMutableTreeTableNode root,
                                       Map<CyNode, NodeAuxiliaryInformation> info) {
        for (CyTable trace : info.get(node).getTraces()) {
            var aux = info.get(node);
            var rows = aux.getSituations(trace);
            var tableNode = new DefaultMutableTreeTableNode(trace.getTitle());
            root.add(tableNode);
            for (var i : rows) {
                tableNode.add(new DefaultMutableTreeTableNode("" + i));
            }
        }
    }

    @Override
    public TreeTableModel createSituationTableModel(CyNode node, DefaultMutableTreeTableNode root) {
        var group = this.network.getRow(node).get(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class);
        if (group == 0 || group == 2) {
            MultiObjectTreeTableNode base = new MultiObjectTreeTableNode("Base");
            this.createTraceTableModel(node, base, this.baseNodeInfoSnapshot);
            root.add(base);
        }
        if (group == 1 || group == 2) {
            MultiObjectTreeTableNode delta = new MultiObjectTreeTableNode("Delta");
            this.createTraceTableModel(node, delta, this.deltaNodeInfoSnapshot);
            root.add(delta);
        }
        return new DefaultTreeTableModel(root);
    }

    private TreeTableModel initializeTreeTableModel(TreeTableModel baseModel, TreeTableModel deltaModel) {
        DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode("Root");
        var baseRoot = (MutableTreeTableNode) baseModel.getRoot();
        MultiObjectTreeTableNode base = new MultiObjectTreeTableNode("Base");
        while (baseRoot.getChildCount() > 0) {
            base.add((MutableTreeTableNode) baseRoot.getChildAt(0));
        }
        root.add(base);
        var deltaRoot = (MutableTreeTableNode) deltaModel.getRoot();
        MultiObjectTreeTableNode delta = new MultiObjectTreeTableNode("Delta");
        while (deltaRoot.getChildCount() > 0) {
            delta.add((MutableTreeTableNode) deltaRoot.getChildAt(0));
        }
        root.add(delta);
        return new CustomTreeTableModel(root, 2);
    }

    @Override
    public TreeTableModel createNetworkTableModel() {
        return this.treeTableModel;
    }

    private VisualStyle createDefaultVisualStyle(TraceGraphController baseController,
                                                 TraceGraphController deltaController) {
        var visualStyleFactory = registrar.getService(org.cytoscape.view.vizmap.VisualStyleFactory.class);
        VisualStyle style = visualStyleFactory.createVisualStyle("default-comparison");

        VisualMappingFunctionFactory visualMappingFunctionFactory = registrar.getService(VisualMappingFunctionFactory.class, "(mapping.type=discrete)");

        DiscreteMapping<Integer, Paint> nodeColorMapping = (DiscreteMapping<Integer, Paint>) visualMappingFunctionFactory.createVisualMappingFunction(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class, BasicVisualLexicon.NODE_FILL_COLOR);
        nodeColorMapping.putMapValue(0, Color.GREEN);
        nodeColorMapping.putMapValue(1, Color.RED);
        nodeColorMapping.putMapValue(2, Color.BLUE);
        style.addVisualMappingFunction(nodeColorMapping);

        DiscreteMapping<Integer, Paint> edgeColorMapping = (DiscreteMapping<Integer, Paint>) visualMappingFunctionFactory.createVisualMappingFunction(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class, EDGE_STROKE_UNSELECTED_PAINT);
        edgeColorMapping.putMapValue(0, Color.GREEN);
        edgeColorMapping.putMapValue(1, Color.RED);
        edgeColorMapping.putMapValue(2, Color.BLUE);
        style.addVisualMappingFunction(edgeColorMapping);

        var baseMapping = new HashMap<Long, Integer>();
        for (CyNode node : baseController.getNetwork().getNodeList()) {
            baseMapping.put(node.getSUID(), baseController.getTraceGraph().getNodeAux(node).getFrequency());
        }

        var deltaMapping = new HashMap<Long, Integer>();
        for (CyNode node : deltaController.getNetwork().getNodeList()) {
            deltaMapping.put(node.getSUID(), deltaController.getTraceGraph().getNodeAux(node).getFrequency());
        }

        PassthroughMapping<CyRow, Double> nodeSizeMapping = new ComparisonSizeMapping(baseMapping, deltaMapping);
        style.addVisualMappingFunction(nodeSizeMapping);

        style.addVisualMappingFunction(new TooltipMapping(this.pdm));

        style.setDefaultValue(EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);

        return style;
    }

    public void initView() {
        var manager = this.registrar.getService(CyApplicationManager.class);
        var tgNetworkViewRenderer = manager.getNetworkViewRenderer("org.cytoscape.ding");
        var networkViewFactory = tgNetworkViewRenderer.getNetworkViewFactory();
        this.view = networkViewFactory.createNetworkView(this.network);
    }

    public void updateVisibility() {
        for (var edgeView : this.view.getEdgeViews()) {
            var edge = edgeView.getModel();
            var group = this.network.getRow(edge).get(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class);
            switch (group) {
                case 2 -> edgeView.setVisualProperty(EDGE_VISIBLE, edgesBaseDeltaVisible);
                case 1 -> edgeView.setVisualProperty(EDGE_VISIBLE, edgesDeltaOnlyVisible);
                case 0 -> edgeView.setVisualProperty(EDGE_VISIBLE, edgesBaseOnlyVisible);
            }
        }
        for (var nodeView : this.view.getNodeViews()) {
            var node = nodeView.getModel();
            var group = this.network.getRow(node).get(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class);
            switch (group) {
                case 2 -> nodeView.setVisualProperty(NODE_VISIBLE, nodesBaseDeltaVisible);
                case 1 -> nodeView.setVisualProperty(NODE_VISIBLE, nodesDeltaOnlyVisible);
                case 0 -> nodeView.setVisualProperty(NODE_VISIBLE, nodesBaseOnlyVisible);
            }
        }
    }

    @Override
    public Map<String, String> getNodeInfo(CyNode node) {
        HashMap<String, String> map = new HashMap<>();
        var group = this.network.getRow(node).get(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class);
        if (group == 0 || group == 2) {
            var aux = this.baseNodeInfoSnapshot.get(node);
            map.put("Base Visit Duration", "" + aux.getVisitDuration());
            map.put("Base Frequency", "" + aux.getFrequency());
        }
        if (group == 1 || group == 2) {
            var aux = this.deltaNodeInfoSnapshot.get(node);
            map.put("Delta Visit Duration", "" + aux.getVisitDuration());
            map.put("Delta Frequency", "" + aux.getFrequency());
        }
        return map;
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        if (e.getNetwork() == this.getNetwork()) {
            var eventHelper = this.registrar.getService(CyEventHelper.class);
            eventHelper.fireEvent(new SetCurrentComparisonControllerEvent(this, this));
        }
    }

    public boolean getGroupVisibility(String group, boolean node) {
        switch (group) {
            case BO -> {
                if (node) {
                    return this.nodesBaseOnlyVisible;
                } else {
                    return this.edgesBaseOnlyVisible;
                }
            }
            case DO -> {
                if (node) {
                    return this.nodesDeltaOnlyVisible;
                } else {
                    return this.edgesDeltaOnlyVisible;
                }
            }
            case BD -> {
                if (node) {
                    return this.nodesBaseDeltaVisible;
                } else {
                    return this.edgesBaseDeltaVisible;
                }
            }
        }
        return true;
    }

    public void setGroupVisibility(String group, boolean node, boolean visible) {
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
