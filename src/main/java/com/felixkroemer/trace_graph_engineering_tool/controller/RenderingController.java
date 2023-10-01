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
import org.cytoscape.application.CyUserLog;
import org.cytoscape.event.CyEventHelper;
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
import org.cytoscape.work.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;
import static org.cytoscape.view.presentation.property.table.BasicTableVisualLexicon.COLUMN_VISIBLE;

public class RenderingController implements SelectedNodesAndEdgesListener, PropertyChangeListener {

    private Logger logger;
    private CyServiceRegistrar registrar;
    private VisualStyle defaultStyle;
    private CyNetworkView view;
    private AbstractDisplayController displayManager;
    private TraceGraph traceGraph;
    private Map<Parameter, HighlightRange> highlightRanges;

    public RenderingController(CyServiceRegistrar registrar, TraceGraph traceGraph) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.registrar = registrar;
        this.traceGraph = traceGraph;
        this.highlightRanges = new HashMap<>();
        this.defaultStyle = createInitialVisualStyle();
        // NetworkViewRenderer gets added to manager on registration, mapped to id
        // reg.getService(CyNetworkViewFactory.class, "(id=foobar)") does not work,
        // CyNetworkViewFactory is never registered by Ding
        // instead, DefaultNetworkViewFactory is retrieved, which
        // retrieves the CyNetworkViewFactory of the default NetworkViewRenderer (which is ding)
        var manager = this.registrar.getService(CyApplicationManager.class);
        var tgNetworkViewRenderer = manager.getNetworkViewRenderer("foobar");
        var networkViewFactory = tgNetworkViewRenderer.getNetworkViewFactory();
        this.view = networkViewFactory.createNetworkView(traceGraph.getNetwork());
        this.displayManager = new SelectedDisplayController(this.view, this.traceGraph);
        try {
            Field rendererId = this.view.getClass().getDeclaredField("rendererId");
            rendererId.setAccessible(true);
            rendererId.set(this.view, "foobar");
        } catch (NoSuchFieldException | IllegalAccessException e) {
        }

        var mapper = registrar.getService(VisualMappingManager.class);
        mapper.setVisualStyle(this.defaultStyle, this.view);
    }


    public VisualStyle createInitialVisualStyle() {
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
                this.resetStyle();
                this.highlightRanges();
            }
        }
    }

    public void highlightRanges() {
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
            var nodeView = view.getNodeView(node);
            if (match) {
/*                //TODO: check how vp dependencies work
                //nodeView.setVisualProperty(NODE_SIZE, 30.0);
                nodeView.setVisualProperty(NODE_BORDER_PAINT, Color.MAGENTA);
                nodeView.setVisualProperty(NODE_BORDER_WIDTH, 3.0);
                nodeView.setVisualProperty(NODE_SHAPE, NodeShapeVisualProperty.HEXAGON);*/
                nodeView.setVisualProperty(NODE_VISIBLE, true);
            } else {
/*                nodeView.setVisualProperty(NODE_TRANSPARENCY, 128);
                nodeView.setVisualProperty(NODE_BORDER_TRANSPARENCY, 128);*/
                nodeView.setVisualProperty(NODE_VISIBLE, false);
            }
        }
    }

    private void updateTraceGraph() {
        TaskIterator iterator = new TaskIterator(new AbstractTask() {
            @Override
            public void run(TaskMonitor taskMonitor) {
                traceGraph.clearNetwork();
                traceGraph.reinitNetwork();
            }
        });
        // throws weird error if run asynchronously
        // runs on awt event thread
        // TODO: check if running async is possible
        var taskManager = registrar.getService(SynchronousTaskManager.class);
        taskManager.execute(iterator);
        this.applyStyle(defaultStyle);
        this.applyWorkingLayout();
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

    private void applyStyle(VisualStyle style) {
        style.apply(view);
    }

    public void resetStyle() {
        this.applyStyle(this.defaultStyle);
    }

    public void setMode(RenderingMode mode) {
        this.resetStyle();
        this.highlightRanges();
        switch (mode) {
            case FULL -> {
                this.displayManager = new DefaultDisplayController(view, this.traceGraph);
            }
            case SELECTED -> {
                this.displayManager = new SelectedDisplayController(view, this.traceGraph);
            }
            case TRACES -> {
                this.displayManager = new TracesDisplayController(this.registrar, view, this.traceGraph, 2);
            }
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
