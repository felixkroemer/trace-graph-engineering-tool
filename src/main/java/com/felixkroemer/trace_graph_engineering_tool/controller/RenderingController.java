package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.display_manager.AbstractDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.display_manager.DefaultDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.display_manager.SelectedDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.display_manager.TracesDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Mappings;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.vizmap.*;
import org.cytoscape.work.TaskManager;

import java.awt.*;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE;

public class RenderingController implements SelectedNodesAndEdgesListener {

    private CyServiceRegistrar registrar;
    private VisualStyle style;
    private CyNetworkView view;
    private AbstractDisplayController displayManager;
    private TraceGraph traceGraph;

    public RenderingController(CyServiceRegistrar registrar, TraceGraph traceGraph) {
        this.registrar = registrar;
        this.style = createInitialVisualStyle();
        var networkViewFactory = registrar.getService(CyNetworkViewFactory.class);
        this.view = networkViewFactory.createNetworkView(traceGraph.getNetwork());
        this.traceGraph = traceGraph;
        this.displayManager = new SelectedDisplayController(view, this.traceGraph);
        init();
    }

    public void init() {
        var visualMappingManager = registrar.getService(VisualMappingManager.class);
        visualMappingManager.setVisualStyle(this.style, this.view);
    }

    public VisualStyle createInitialVisualStyle() {
        var VisualStyleFactory = registrar.getService(VisualStyleFactory.class);
        VisualStyle style = VisualStyleFactory.createVisualStyle("default");

        // Ensure we get org.cytoscape.view.vizmap.internal.mappings.PassthroughMappingFactory, then cast to
        // PassthroughMapping
        VisualMappingFunctionFactory visualMappingFunctionFactory =
                registrar.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
/*        TooltipMappingFactory tooltipMappingFunctionFactory =
                (TooltipMappingFactory) registrar.getService(VisualMappingFunctionFactory.class, "(mapping" + ".type" +
                        "=tooltip)");
        tooltipMappingFunctionFactory.setTraceGraphController(this.controller);*/

        VisualMappingFunction<Integer, Double> sizeMapping = Mappings.createSizeMapping(1, 2000,
                visualMappingFunctionFactory);
        VisualMappingFunction<Integer, Paint> colorMapping = Mappings.createColorMapping(1, 1600,
                visualMappingFunctionFactory);
/*
        VisualMappingFunction<CyRow, String> tooltipMapping =
                Mappings.createTooltipMapping(tooltipMappingFunctionFactory);
*/

        style.addVisualMappingFunction(sizeMapping);
        style.addVisualMappingFunction(colorMapping);
        //style.addVisualMappingFunction(tooltipMapping);


        style.setDefaultValue(EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);

        return style;
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

    public void resetStyle() {
        // does not reveal hidden nodes or edges for some reason
        var visualMappingManager = registrar.getService(VisualMappingManager.class);
        visualMappingManager.setVisualStyle(createInitialVisualStyle(), view);
    }

    public void setDisplayManager(AbstractDisplayController displayManager) {
        this.displayManager = displayManager;
    }

    public void setMode(RenderingMode mode) {
        this.resetStyle();
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

}
