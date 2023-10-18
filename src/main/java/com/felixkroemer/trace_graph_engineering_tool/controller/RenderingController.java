package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.display_controller.*;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEventListener;
import com.felixkroemer.trace_graph_engineering_tool.mappings.TooltipMappingFactory;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
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
import org.cytoscape.work.TaskManager;

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

    private CyServiceRegistrar registrar;
    private VisualStyle defaultStyle;
    private CyNetworkView view;
    private AbstractDisplayController displayController;
    private TraceGraph traceGraph;

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
        this.displayController = new FollowDisplayController(registrar, this.view, this.traceGraph);

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

        int maxFrequency = -1;
        int maxVisits = -1;
        for (CyRow row : this.traceGraph.getNetwork().getTable(CyNode.class, CyNetwork.LOCAL_ATTRS).getAllRows()) {
            int frequency = row.get(Columns.NODE_FREQUENCY, Integer.class);
            if (frequency > maxFrequency) maxFrequency = frequency;
            int visits = row.get(Columns.NODE_VISITS, Integer.class);
            if (visits > maxVisits) maxVisits = visits;
        }

        VisualMappingFunction<Integer, Double> sizeMapping = Mappings.createSizeMapping(1, maxVisits,
                visualMappingFunctionFactory);
        VisualMappingFunction<Integer, Paint> colorMapping = Mappings.createColorMapping(1, maxFrequency,
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
                var taskManager = registrar.getService(TaskManager.class);
                taskManager.execute(NetworkController.createLayoutTask(registrar, this.view));
            }
        }
    }

    public void updateVisualStyle() {
        this.defaultStyle = createDefaultVisualStyle();
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

    private void setDisplayController(AbstractDisplayController displayController) {
        if (this.displayController != null) {
            if (this.displayController.getClass() == displayController.getClass()) {
                return;
            } else {
                this.displayController.disable();
            }
        }
        //this.deselectAll();
        this.defaultStyle.apply(this.view);
        this.displayController = displayController;
    }

    public void setMode(String mode) {
        switch (mode) {
            case RENDERING_MODE_FULL -> {
                this.setDisplayController(new DefaultDisplayController(registrar, view, this.traceGraph));
            }
            case RENDERING_MODE_FOLLOW -> {
                this.setDisplayController(new FollowDisplayController(registrar, view, this.traceGraph));
            }
            case RENDERING_MODE_TRACES -> {
                this.setDisplayController(new TracesDisplayController(this.registrar, view, this.traceGraph, 2));
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
            this.displayController.handleNodesSelected(event);
        }
    }

    public void destroy() {
        if (this.displayController != null) {
            this.displayController.disable();
        }
        registrar.unregisterService(this, SelectedNodesAndEdgesListener.class);
        registrar.unregisterService(this, ShowTraceEventListener.class);
    }

    @Override
    public void handleEvent(ShowTraceEvent e) {
        if (e.getNetwork() != this.traceGraph.getNetwork()) {
            return;
        }
        if (!(this.displayController instanceof ShortestTraceDisplayController)) {
            this.setDisplayController(new ShortestTraceDisplayController(registrar, view, traceGraph, e.getNodes()));
        }
    }
}
