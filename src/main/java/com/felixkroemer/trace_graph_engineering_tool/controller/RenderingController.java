package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.display_manager.AbstractDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.display_manager.DefaultDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.display_manager.SelectedDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.display_manager.TracesDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.mappings.TooltipMappingFactory;
import com.felixkroemer.trace_graph_engineering_tool.model.HighlightRange;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Mappings;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.table.CyTableViewManager;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.vizmap.*;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;
import static org.cytoscape.view.presentation.property.table.BasicTableVisualLexicon.COLUMN_VISIBLE;

public class RenderingController implements SelectedNodesAndEdgesListener, PropertyChangeListener {

    public static final String RENDERING_MODE_FULL = "RENDERING_MODE_FULL";
    public static final String RENDERING_MODE_SELECTED = "RENDERING_MODE_SELECTED";
    public static final String RENDERING_MODE_TRACES = "RENDERING_MODE_TRACES";

    private CyServiceRegistrar registrar;
    private VisualStyle defaultStyle;
    private CyNetworkView view;
    private AbstractDisplayController displayManager;
    private TraceGraph traceGraph;
    private Map<Parameter, HighlightRange> highlightRanges;

    public RenderingController(CyServiceRegistrar registrar, TraceGraph traceGraph) {
        this.registrar = registrar;
        this.traceGraph = traceGraph;
        this.highlightRanges = new HashMap<>();
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
        this.displayManager = new SelectedDisplayController(this.view, this.traceGraph);
        try {
            Field rendererId = this.view.getClass().getDeclaredField("rendererId");
            rendererId.setAccessible(true);
            rendererId.set(this.view, "org.cytoscape.ding-extension");
        } catch (NoSuchFieldException | IllegalAccessException e) {
        }

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
            case "enabled" -> {
                var tableViewManager = registrar.getService(CyTableViewManager.class);
                var nodeTableView = tableViewManager.getTableView(traceGraph.getNetwork().getDefaultNodeTable());
                Parameter param = (Parameter) evt.getSource();
                var columnView = nodeTableView.getColumnView(param.getName());
                columnView.setVisualProperty(COLUMN_VISIBLE, evt.getNewValue());
                this.updateTraceGraph();
            }
            case "bins" -> {
                this.updateTraceGraph();
            }
            case "highlightRange" -> {
                var parameter = (Parameter) evt.getSource();
                var range = (HighlightRange) evt.getNewValue();
                if (range != null) {
                    this.highlightRanges.put(parameter, range);
                } else {
                    this.highlightRanges.remove(parameter);
                }
                this.hideUnhighlightedNodes();
            }
        }
    }

    public void hideUnhighlightedNodes() {
        if (highlightRanges.isEmpty()) {
            for (var nodeView : this.view.getNodeViews()) {
                nodeView.setVisualProperty(NODE_VISIBLE, true);
            }
        }
        var nodeTable = traceGraph.getNetwork().getDefaultNodeTable();
        for (var node : traceGraph.getNetwork().getNodeList()) {
            boolean match = this.highlightRanges.entrySet().stream().allMatch(entry -> {
                var row = nodeTable.getRow(node.getSUID());
                var value = row.get(entry.getKey().getName(), Integer.class);
                return value >= entry.getValue().getLowerBound() && value <= entry.getValue().getUpperBound();
            });
            view.getNodeView(node).setVisualProperty(NODE_VISIBLE, match);
        }
    }

    private void updateTraceGraph() {
        TaskIterator iterator = new TaskIterator(new AbstractTask() {
            @Override
            public void run(TaskMonitor taskMonitor) {
                taskMonitor.setProgress(0);
                traceGraph.clearNetwork();
                taskMonitor.setStatusMessage("Recreating network");
                traceGraph.reinitNetwork();
                taskMonitor.setProgress(0.5);
                taskMonitor.setStatusMessage("Applying style");
                defaultStyle.apply(view);
                taskMonitor.setProgress(0.75);
                taskMonitor.setStatusMessage("Applying layout");
                applyWorkingLayout();
            }
        });
        //TODO: dialog does not display anything
        var taskManager = registrar.getService(TaskManager.class);
        taskManager.execute(iterator);
        var eventHelper = registrar.getService(CyEventHelper.class);
        eventHelper.flushPayloadEvents();
    }

    public CyNetworkView getView() {
        return this.view;
    }

    public void applyWorkingLayout() {
        CyNetworkView view = this.displayManager.getNetworkView();
        var layoutManager = registrar.getService(CyLayoutAlgorithmManager.class);
        CyLayoutAlgorithm layoutFactory = layoutManager.getLayout("grid");
        Object context = layoutFactory.getDefaultLayoutContext();
        var taskIterator = layoutFactory.createTaskIterator(view, context, CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
        TaskManager<?, ?> manager = registrar.getService(TaskManager.class);
        manager.execute(taskIterator);
    }

    public void setMode(String mode) {
        this.deselectAll();
        this.view.getEdgeViews().forEach(edge -> {
            this.defaultStyle.apply(this.view.getModel().getDefaultNodeTable().getRow(edge.getSUID()), edge);
        });
        /*        this.defaultStyle.apply(view);*/
        switch (mode) {
            case RENDERING_MODE_FULL -> {
                this.displayManager = new DefaultDisplayController(view, this.traceGraph);
            }
            case RENDERING_MODE_SELECTED -> {
                this.displayManager = new SelectedDisplayController(view, this.traceGraph);
            }
            case RENDERING_MODE_TRACES -> {
                this.displayManager = new TracesDisplayController(this.registrar, view, this.traceGraph, 2);
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

}
