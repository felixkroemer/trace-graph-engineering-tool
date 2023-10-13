package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.cytoscape.application.CyApplicationManager;
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

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class NetworkComparisonController extends NetworkController {
    private CyNetwork networkA;
    private CyNetwork networkB;
    private CySubNetwork network;
    private CyNetworkView view;

    private VisualStyle defaultVisualStyle;

    public NetworkComparisonController(CyNetwork networkA, CyNetwork networkB, CySubNetwork network,
                                       CyServiceRegistrar registrar) {
        super(registrar, network);
        this.networkA = networkA;
        this.networkB = networkB;
        this.network = network;
        this.defaultVisualStyle = createDefaultVisualStyle();

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
            boolean bd = this.networkA.containsNode(node);
            boolean od = this.networkB.containsNode(node);
            var row = defaultNodeTable.getRow(node.getSUID());
            row.set(Columns.COMPARISON_GROUP_MEMBERSHIP, bd && od ? 2 : (bd ? 0 : 1));
        }
        for (CyEdge edge : this.network.getEdgeList()) {
            boolean bd = this.networkA.containsEdge(edge);
            boolean od = this.networkB.containsEdge(edge);
            var row = defaultEdgeTable.getRow(edge.getSUID());
            row.set(Columns.COMPARISON_GROUP_MEMBERSHIP, bd && od ? 2 : (bd ? 0 : 1));
        }

        this.initView();

        var mapper = registrar.getService(VisualMappingManager.class);
        mapper.setVisualStyle(this.defaultVisualStyle, this.view);
        this.applyStyleAndLayout();
    }

    public void initTables() {
        this.network.getDefaultNodeTable().createColumn(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class, false);
        this.network.getDefaultEdgeTable().createColumn(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class, false);

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

    }

    @Override
    public void updateNetwork(Parameter parameter) {

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
}
