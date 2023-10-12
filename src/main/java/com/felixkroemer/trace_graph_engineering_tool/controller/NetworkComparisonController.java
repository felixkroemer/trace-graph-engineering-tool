package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.mappings.TooltipMappingFactory;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.util.Mappings;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.vizmap.*;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

import java.awt.*;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class NetworkComparisonController extends NetworkController{
    private CyNetwork networkA;
    private CyNetwork networkB;
    private CySubNetwork network;
    private CyNetworkView view;

    private VisualStyle defaultVisualStyle;

    public NetworkComparisonController(CyNetwork networkA, CyNetwork networkB, CySubNetwork network, CyServiceRegistrar registrar) {
        super(registrar);
        this.networkA = networkA;
        this.networkB = networkB;
        this.network = network;
        this.defaultVisualStyle = createDefaultVisualStyle();

        this.initTables();

        //TODO: maybe add more efficient batching version
        for(CyNode node: this.networkA.getNodeList()) {
            this.network.addNode(node);
        }
        for(CyNode node: this.networkB.getNodeList()) {
            this.network.addNode(node);
        }

        var defaultNodeTable = this.getNetwork().getDefaultNodeTable();
        for(CyNode node : this.getNetwork().getNodeList()) {
            boolean bd = this.networkA.containsNode(node);
            boolean od = this.networkB.containsNode(node);
            var row = defaultNodeTable.getRow(node.getSUID());
            row.set(Columns.COMPARISON_GROUP_MEMBERSHIP, bd && od ? 2 : (bd ? 0 : 1));
        }

        this.initView();

        var mapper = registrar.getService(VisualMappingManager.class);
        mapper.setVisualStyle(this.defaultVisualStyle, this.view);
        this.applyStyleAndLayout();
    }

    public void initTables() {
        this.getNetwork().getDefaultNodeTable().createColumn(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class, false);

        //TODO map columns of nodes that are compared to the node table of this network;
    }

    @Override
    public CyNetwork getNetwork() {
        return this.network;
    }

    @Override
    public CyNetworkView getView() {
        return this.view;
    }

    @Override
    public VisualStyle getVisualStyle() {
        return this.defaultVisualStyle;
    }

    private VisualStyle createDefaultVisualStyle() {
        var VisualStyleFactory = registrar.getService(org.cytoscape.view.vizmap.VisualStyleFactory.class);
        VisualStyle style = VisualStyleFactory.createVisualStyle("default-comparison");

        style.setDefaultValue(NODE_SIZE, 10.0);
        style.setDefaultValue(EDGE_VISIBLE, false);
        style.setDefaultValue(EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);

        return style;
    }

    public void initView() {
        var manager = this.registrar.getService(CyApplicationManager.class);
        var tgNetworkViewRenderer = manager.getNetworkViewRenderer("org.cytoscape.ding-extension");
        var networkViewFactory = tgNetworkViewRenderer.getNetworkViewFactory();
        this.view = networkViewFactory.createNetworkView(this.getNetwork());
    }
}
