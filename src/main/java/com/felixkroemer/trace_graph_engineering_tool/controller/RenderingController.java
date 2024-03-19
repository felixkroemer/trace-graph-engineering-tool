package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.display_controller.*;
import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentEdgeDisplayControllerEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEventListener;
import com.felixkroemer.trace_graph_engineering_tool.mappings.ColorMapping;
import com.felixkroemer.trace_graph_engineering_tool.mappings.SizeMapping;
import com.felixkroemer.trace_graph_engineering_tool.mappings.TooltipMapping;
import com.felixkroemer.trace_graph_engineering_tool.model.FilteredState;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels.EdgeDisplayControllerPanel;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyDisposable;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.javatuples.Pair;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static com.felixkroemer.trace_graph_engineering_tool.display_controller.DefaultEdgeDisplayController.RENDERING_MODE_FULL;
import static com.felixkroemer.trace_graph_engineering_tool.display_controller.FollowEdgeDisplayController.RENDERING_MODE_FOLLOW;
import static com.felixkroemer.trace_graph_engineering_tool.display_controller.TracesEdgeDisplayController.RENDERING_MODE_TRACES;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class RenderingController implements SelectedNodesAndEdgesListener, PropertyChangeListener,
        ShowTraceEventListener, CyDisposable {

    private CyServiceRegistrar registrar;
    private TraceGraphController traceGraphController;
    private VisualStyle defaultStyle;
    private CyNetworkView view;
    private EdgeDisplayController displayController;
    private TraceGraph traceGraph;
    private FilteredState filteredState;
    private Set<CyNode> labeledNodes;

    public RenderingController(CyServiceRegistrar registrar, TraceGraphController traceGraphController) {
        this.registrar = registrar;
        this.traceGraphController = traceGraphController;
        this.traceGraph = this.traceGraphController.getTraceGraph();
        this.defaultStyle = createDefaultVisualStyle();
        this.labeledNodes = new HashSet<>();

        // NetworkViewRenderer gets added to manager on registration, mapped to id
        // reg.getService(CyNetworkViewFactory.class, "(id=org.cytoscape.ding-extension)") does not work,
        // CyNetworkViewFactory is never registered by Ding
        // instead, DefaultNetworkViewFactory is retrieved, which
        // retrieves the CyNetworkViewFactory of the default NetworkViewRenderer (which is ding)
        var manager = this.registrar.getService(CyApplicationManager.class);
        var tgNetworkViewRenderer = manager.getNetworkViewRenderer("org.cytoscape.ding");
        var networkViewFactory = tgNetworkViewRenderer.getNetworkViewFactory();
        this.view = networkViewFactory.createNetworkView(traceGraph.getNetwork());
        this.setMode(RENDERING_MODE_FOLLOW);

        this.traceGraph.getPDM().addObserver(this);
        this.traceGraph.getPDM().forEach(p -> p.addObserver(this));

        registrar.registerService(this, SelectedNodesAndEdgesListener.class);
        registrar.registerService(this, ShowTraceEventListener.class);

        var mapper = registrar.getService(VisualMappingManager.class);
        mapper.setVisualStyle(this.defaultStyle, this.view);

        this.filteredState = new FilteredState(this.view);
        this.hideNodes();
    }

    private VisualStyle createDefaultVisualStyle() {
        var VisualStyleFactory = registrar.getService(VisualStyleFactory.class);
        VisualStyle style = this.defaultStyle == null ? VisualStyleFactory.createVisualStyle("default-tracegraph") : this.defaultStyle;

        int maxFrequency = -1;
        int maxVisitDuraton = -1;
        for (CyNode node : this.traceGraph.getNetwork().getNodeList()) {
            int frequency = this.traceGraph.getNodeAux(node).getFrequency();
            if (frequency > maxFrequency)
                maxFrequency = frequency;
            int visitDuration = this.traceGraph.getNodeAux(node).getVisitDuration();
            if (visitDuration > maxVisitDuraton)
                maxVisitDuraton = visitDuration;
        }

        var frequencyMapping = new HashMap<Long, Integer>();
        for (CyNode node : this.traceGraph.getNetwork().getNodeList()) {
            frequencyMapping.put(node.getSUID(), this.traceGraph.getNodeAux(node).getFrequency());
        }

        var visitDurationMapping = new HashMap<Long, Integer>();
        for (CyNode node : this.traceGraph.getNetwork().getNodeList()) {
            visitDurationMapping.put(node.getSUID(), this.traceGraph.getNodeAux(node).getVisitDuration());
        }

        style.addVisualMappingFunction(new SizeMapping(frequencyMapping));
        style.addVisualMappingFunction(new ColorMapping(visitDurationMapping, registrar.getService(CyEventHelper.class)));
        style.addVisualMappingFunction(new TooltipMapping(traceGraph.getPDM()));

        var excessMappingFunctions = style.getAllVisualMappingFunctions().stream().filter(f -> {
            var vp = f.getVisualProperty();
            return !(vp.equals(NODE_SIZE) || vp.equals(NODE_FILL_COLOR) || vp.equals(NODE_TOOLTIP));
        }).collect(Collectors.toSet());
        for (var mappingFunction : excessMappingFunctions) {
            style.removeVisualMappingFunction(mappingFunction.getVisualProperty());
        }

        // ignored, because CyEdgeViewImpl has a boolean visible that decides if the edge is drawn
        // visible is only set in fireViewChangedEvent in response to setVisualProperty
        // setVisualProperty is never called when applying default values of a style
        // => hide edges manually
        // style.setDefaultValue(EDGE_VISIBLE, false);
        style.setDefaultValue(EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);
        style.setDefaultValue(EDGE_STROKE_UNSELECTED_PAINT, Color.BLACK);

        return style;
    }

    /**
     * Called when network is split or merged or when nodes are hidden/revealed
     */
    public void onNetworkChanged() {
        CyEventHelper helper = registrar.getService(CyEventHelper.class);
        helper.flushPayloadEvents();
        this.updateVisualStyle();
        // visual style has already been adjusted by the displayController
        // => we can tell it to initialize
        if (this.displayController != null) {
            this.displayController.init();
        }
        // hideNodes uses setLockedValue rather than setVisualProperty because NODE_VISIBLE would be overridden by
        // vs.apply from RowsSetEvent even if applyStyleAndLayout were not called here
        this.hideNodes();
        this.traceGraphController.applyStyleAndLayout();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case Parameter.VISIBLE_BINS, ParameterDiscretizationModel.PERCENTILE_FILTER -> {
                if (!traceGraph.getPDM().isUpdating()) {
                    this.onNetworkChanged();
                }
            }
        }
    }

    public void updateVisualStyle() {
        var style = createDefaultVisualStyle();
        style = displayController.adjustVisualStyle(style);
        this.defaultStyle = style;
    }

    public void hideNodesUsingPercentiles() {
        var percentile = traceGraph.getPDM().getPercentile();
        List<Pair<Long, Integer>> pairs = new ArrayList<>(this.traceGraph.getNetwork().getNodeCount());
        var valueSum = 0;
        for (CyNode node : this.traceGraph.getNetwork().getNodeList()) {
            var value = 0;
            switch (percentile.getValue0()) {
                case "visitDuration" -> value = this.traceGraph.getNodeAux(node).getVisitDuration();
                case "frequency" -> value = this.traceGraph.getNodeAux(node).getFrequency();
            }
            pairs.add(new Pair<>(node.getSUID(), value));
            valueSum += value;
        }
        var cutOff = valueSum * percentile.getValue1() / 100;
        pairs.sort(Comparator.comparing(Pair::getValue1));
        Collections.reverse(pairs);
        var sum = 0;
        for (var pair : pairs) {
            var node = traceGraph.getNetwork().getNode(pair.getValue0());
            view.getNodeView(node).setLockedValue(NODE_VISIBLE, sum <= cutOff);
            sum += pair.getValue1();
        }
    }

    public void hideNodes() {
        // percentile takes precedence for now
        if (traceGraph.getPDM().getPercentile() != null) {
            this.hideNodesUsingPercentiles();
        } else {
            this.hideNodesUsingBins();
        }
        this.filteredState.update();
    }

    public void hideNodesUsingBins() {
        Map<Parameter, Set<Integer>> visibleBins = new HashMap<>();
        for (Parameter param : this.traceGraph.getPDM().getParameters()) {
            visibleBins.put(param, param.getVisibleBins());
        }
        // do not hide any nodes if no bins are selected
        if (visibleBins.values().stream().allMatch(Set::isEmpty)) {
            for (var nodeView : this.view.getNodeViews()) {
                // use setLockedValue here because the task on the EDT for the rowssetevent
                // will apply the default style to all nodes again after this runs and reveal all nodes.
                nodeView.setLockedValue(NODE_VISIBLE, true);
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
            view.getNodeView(node).setLockedValue(NODE_VISIBLE, match);
        }
    }

    public CyNetworkView getView() {
        return this.view;
    }

    public VisualStyle getVisualStyle() {
        return this.defaultStyle;
    }

    private void setDisplayController(EdgeDisplayController displayController) {
        if (this.displayController != null) {
            if (this.displayController.getClass() == displayController.getClass()) {
                return;
            } else {
                this.displayController.dispose();
            }
        }

        this.displayController = displayController;

        this.updateVisualStyle();
        // style gets applied asynchronously, the visual properties that are set in .init() will be overwritten
        this.defaultStyle.apply(this.view);
        this.displayController.init();

        var eventHelper = registrar.getService(CyEventHelper.class);
        eventHelper.fireEvent(new SetCurrentEdgeDisplayControllerEvent(this, null, this.displayController));
    }

    public void setMode(String mode) {
        switch (mode) {
            case RENDERING_MODE_FULL ->
                    this.setDisplayController(new DefaultEdgeDisplayController(registrar, view, this.traceGraph, this));
            case RENDERING_MODE_FOLLOW ->
                    this.setDisplayController(new FollowEdgeDisplayController(registrar, view, this.traceGraph, this));
            case RENDERING_MODE_TRACES ->
                    this.setDisplayController(new TracesEdgeDisplayController(this.registrar, view, this.traceGraph, 2, this));
            default -> throw new IllegalStateException("Unexpected value: " + mode);
        }
    }

    @Override
    public void handleEvent(SelectedNodesAndEdgesEvent event) {
        if (event.getNetwork() == this.traceGraph.getNetwork()) {
            this.displayController.handleNodesSelected(event);
            for (CyNode node : this.labeledNodes) {
                view.getNodeView(node).setVisualProperty(NODE_LABEL, null);
            }
            if (event.getSelectedNodes().size() > 1 && event.getSelectedNodes().size() < 6) {
                int i = 1;
                for (var node : event.getSelectedNodes()) {
                    labeledNodes.add(node);
                    var nodeView = view.getNodeView(node);
                    nodeView.setVisualProperty(NODE_LABEL, "" + i);
                    i++;
                }
            }
        }
    }

    @Override
    public void dispose() {
        if (this.displayController != null) {
            this.displayController.dispose();
        }
        var visualMappingManager = registrar.getService(VisualMappingManager.class);
        visualMappingManager.removeVisualStyle(this.defaultStyle);
        registrar.unregisterAllServices(this.defaultStyle);
        registrar.unregisterService(this, SelectedNodesAndEdgesListener.class);
        registrar.unregisterService(this, ShowTraceEventListener.class);
        this.traceGraph.getPDM().removeObserver(this);
        this.traceGraph.getPDM().forEach(p -> p.removeObserver(this));
    }

    @Override
    public void handleEvent(ShowTraceEvent e) {
        if (e.getNetwork() != this.traceGraph.getNetwork()) {
            return;
        }
        if (!(this.displayController instanceof ShortestTraceEdgeDisplayController)) {
            this.setDisplayController(new ShortestTraceEdgeDisplayController(registrar, view, traceGraph, e.getTrace(), this));
        }
    }

    public void prepareForOperation() {
        Util.deselectAll(this.view);
        this.displayController.prepareForOperation();
    }

    public EdgeDisplayControllerPanel getSettingsPanel() {
        return this.displayController.getSettingsPanel();
    }

    public TraceGraphController getTraceGraphController() {
        return this.traceGraphController;
    }

    public FilteredState getFilteredState() {
        return this.filteredState;
    }
}
