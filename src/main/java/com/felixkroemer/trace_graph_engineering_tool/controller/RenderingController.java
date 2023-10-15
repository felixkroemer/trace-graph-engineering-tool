package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.display_manager.*;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEventListener;
import com.felixkroemer.trace_graph_engineering_tool.mappings.TooltipMappingFactory;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Mappings;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.vizmap.*;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class RenderingController implements SelectedNodesAndEdgesListener, PropertyChangeListener,
        ShowTraceEventListener {

    public static final String RENDERING_MODE_FULL = "RENDERING_MODE_FULL";
    public static final String RENDERING_MODE_FOLLOW = "RENDERING_MODE_SELECTED";
    public static final String RENDERING_MODE_TRACES = "RENDERING_MODE_TRACES";
    public static final String RENDERING_MODE_SHORTEST_TRACE = "RENDERING_MODE_SHORTEST_TRACE";

    private CyServiceRegistrar registrar;
    private VisualStyle defaultStyle;
    private CyNetworkView view;
    private AbstractDisplayController displayManager;
    private TraceGraph traceGraph;
    private String mode;

    public RenderingController(CyServiceRegistrar registrar, TraceGraph traceGraph) {
        this.registrar = registrar;
        this.traceGraph = traceGraph;
        this.defaultStyle = createDefaultVisualStyle();

        // NetworkViewRenderer gets added to manager on registration, mapped to id
        // reg.getService(CyNetworkViewFactory.class, "(id=org.cytoscape.ding-extension)") does not work,
        // CyNetworkViewFactory is never registered by Ding
        // instead, DefaultNetworkViewFactory is retrieved, which
        // retrieves the CyNetworkViewFactory of the default NetworkViewRenderer (which is ding)
        var manager = this.registrar.getService(CyApplicationManager.class);
        var tgNetworkViewRenderer = manager.getNetworkViewRenderer("org.cytoscape.ding-extension");
        var networkViewFactory = tgNetworkViewRenderer.getNetworkViewFactory();
        this.view = networkViewFactory.createNetworkView(traceGraph.getNetwork());
        this.displayManager = new FollowDisplayController(registrar, this.view, this.traceGraph);
        this.mode = RENDERING_MODE_FOLLOW;

        this.traceGraph.getPDM().forEach(p -> {
            p.addObserver(this);
        });

        registrar.registerService(this, SelectedNodesAndEdgesListener.class);
        registrar.registerService(this, ShowTraceEventListener.class);

        var mapper = registrar.getService(VisualMappingManager.class);
        mapper.setVisualStyle(this.defaultStyle, this.view);
    }


    public VisualStyle createDefaultVisualStyle() {
        var VisualStyleFactory = registrar.getService(VisualStyleFactory.class);
        VisualStyle style = VisualStyleFactory.createVisualStyle("default");

        VisualMappingFunctionFactory visualMappingFunctionFactory =
                registrar.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
        TooltipMappingFactory tooltipMappingFunctionFactory =
                (TooltipMappingFactory) registrar.getService(VisualMappingFunctionFactory.class,
                        "(mapping" + ".type" + "=tooltip)");
        tooltipMappingFunctionFactory.setTraceGraph(this.traceGraph);

        VisualMappingFunction<Integer, Double> sizeMapping = Mappings.createSizeMapping(1, 2000,
                visualMappingFunctionFactory);
        VisualMappingFunction<Integer, Paint> colorMapping = Mappings.createColorMapping(1, 1600,
                visualMappingFunctionFactory);
        VisualMappingFunction<CyRow, String> tooltipMapping =
                Mappings.createTooltipMapping(tooltipMappingFunctionFactory);

        style.addVisualMappingFunction(sizeMapping);
        style.addVisualMappingFunction(colorMapping);
        style.addVisualMappingFunction(tooltipMapping);

        style.setDefaultValue(EDGE_VISIBLE, false);
        style.setDefaultValue(EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);

        return style;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            //UIState
            case "visibleBins" -> {
                this.hideNodes();
            }
        }
    }

    public void hideNodes() {
        Map<Parameter, Set<Integer>> visibleBins = new HashMap<>();
        for (Parameter param : this.traceGraph.getPDM().getParameters()) {
            visibleBins.put(param, param.getVisibleBins());
        }
        // do not hide any nodes if no bins are selected
        if (visibleBins.values().stream().allMatch(Set::isEmpty)) {
            for (var nodeView : this.view.getNodeViews()) {
                nodeView.setVisualProperty(NODE_VISIBLE, true);
            }
            return;
        }
        var nodeTable = traceGraph.getNetwork().getDefaultNodeTable();
        for (var node : traceGraph.getNetwork().getNodeList()) {
            boolean match = visibleBins.entrySet().stream().allMatch(entry -> {
                if (entry.getValue().isEmpty()) {
                    return true;
                } else {
                    var row = nodeTable.getRow(node.getSUID());
                    var value = row.get(entry.getKey().getName(), Integer.class);
                    return entry.getValue().contains(value);
                }
            });
            view.getNodeView(node).setVisualProperty(NODE_VISIBLE, match);
        }
    }

    public CyNetworkView getView() {
        return this.view;
    }

    public VisualStyle getVisualStyle() {
        return this.defaultStyle;
    }

    private void setDisplayManager(AbstractDisplayController displayManager) {
        if (this.displayManager != null) {
            if (this.displayManager.getClass() == displayManager.getClass()) {
                return;
            } else {
                this.displayManager.disable();
            }
        }
        //this.deselectAll();
        this.defaultStyle.apply(this.view);
        this.displayManager = displayManager;
    }

    public void setMode(String mode) {
        switch (mode) {
            case RENDERING_MODE_FULL -> {
                this.setDisplayManager(new DefaultDisplayController(registrar, view, this.traceGraph));
            }
            case RENDERING_MODE_FOLLOW -> {
                this.setDisplayManager(new FollowDisplayController(registrar, view, this.traceGraph));
            }
            case RENDERING_MODE_TRACES -> {
                this.setDisplayManager(new TracesDisplayController(this.registrar, view, this.traceGraph, 2));
            }
        }
    }

    private void deselectAll() {
        for (var edgeView : this.view.getEdgeViews()) {
            this.view.getModel().getRow(edgeView.getModel()).set(CyNetwork.SELECTED, false);
        }
        for (var nodeView : this.view.getNodeViews()) {
            this.view.getModel().getRow(nodeView.getModel()).set(CyNetwork.SELECTED, false);
        }
    }

    @Override
    public void handleEvent(SelectedNodesAndEdgesEvent event) {
        if (event.getNetwork() == this.traceGraph.getNetwork()) {
            this.displayManager.handleNodesSelected(event);
        }
    }

    public void focusNode(CyNode node) {
        view.setVisualProperty(NETWORK_CENTER_X_LOCATION, view.getNodeView(node).getVisualProperty(NODE_X_LOCATION));
        view.setVisualProperty(NETWORK_CENTER_Y_LOCATION, view.getNodeView(node).getVisualProperty(NODE_Y_LOCATION));
    }

    public void destroy() {
        registrar.unregisterService(this, SelectedNodesAndEdgesListener.class);
        registrar.unregisterService(this, ShowTraceEventListener.class);
    }

    @Override
    public void handleEvent(ShowTraceEvent e) {
        if (e.getNetwork() != this.traceGraph.getNetwork()) {
            return;
        }
        if (!(this.displayManager instanceof ShortestTraceDisplayController)) {
            this.setDisplayManager(new ShortestTraceDisplayController(registrar, view, traceGraph, e.getNodes()));
        }
    }
}
