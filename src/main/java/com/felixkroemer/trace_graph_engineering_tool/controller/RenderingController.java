package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.display_controller.*;
import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentEdgeDisplayControllerEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEventListener;
import com.felixkroemer.trace_graph_engineering_tool.mappings.TooltipMapping;
import com.felixkroemer.trace_graph_engineering_tool.model.*;
import com.felixkroemer.trace_graph_engineering_tool.util.Mappings;
import com.felixkroemer.trace_graph_engineering_tool.view.TraceGraphPanel;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyDisposable;
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
import org.javatuples.Pair;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;

import static com.felixkroemer.trace_graph_engineering_tool.display_controller.DefaultEdgeDisplayController.RENDERING_MODE_FULL;
import static com.felixkroemer.trace_graph_engineering_tool.display_controller.FollowEdgeDisplayController.RENDERING_MODE_FOLLOW;
import static com.felixkroemer.trace_graph_engineering_tool.display_controller.TracesEdgeDisplayController.RENDERING_MODE_TRACES;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_VISIBLE;

public class RenderingController implements SelectedNodesAndEdgesListener, PropertyChangeListener,
        ShowTraceEventListener, CyDisposable {

    private CyServiceRegistrar registrar;
    private TraceGraphController traceGraphController;
    private VisualStyle defaultStyle;
    private CyNetworkView view;
    private AbstractEdgeDisplayController displayController;
    private TraceGraph traceGraph;
    private FilteredState filteredState;

    public RenderingController(CyServiceRegistrar registrar, TraceGraphController traceGraphController) {
        this.registrar = registrar;
        this.traceGraphController = traceGraphController;
        this.traceGraph = this.traceGraphController.getTraceGraph();
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
        this.setMode(RENDERING_MODE_FOLLOW);

        this.traceGraph.getPDM().addObserver(this);
        this.traceGraph.getPDM().forEach(p -> {
            p.addObserver(this);
        });

        registrar.registerService(this, SelectedNodesAndEdgesListener.class);
        registrar.registerService(this, ShowTraceEventListener.class);

        var mapper = registrar.getService(VisualMappingManager.class);
        mapper.setVisualStyle(this.defaultStyle, this.view);

        // TODO: set initial FilteredState
        this.filteredState = new FilteredState(this.view);
        this.hideNodes();
    }


    public VisualStyle createDefaultVisualStyle() {
        var VisualStyleFactory = registrar.getService(VisualStyleFactory.class);
        VisualStyle style = VisualStyleFactory.createVisualStyle("default-tracegraph");

        VisualMappingFunctionFactory visualMappingFunctionFactory =
                registrar.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");

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

        style.addVisualMappingFunction(sizeMapping);
        style.addVisualMappingFunction(colorMapping);
        style.addVisualMappingFunction(new TooltipMapping(traceGraph.getPDM()));

        // ignored, because CyEdgeViewImpl has a boolean visible that decides if the edge is drawn
        // visible is only set in fireViewChangedEvent in response to setVisualProperty
        // setVisualProperty is never called when applying default values of a style
        // => hide edges manually
        // style.setDefaultValue(EDGE_VISIBLE, false);
        style.setDefaultValue(EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);

        return style;
    }

    /**
     * Called when network is split or merged or when nodes are hidden/revealed
     */
    public void onNetworkChanged() {
        this.hideNodes();
        if (this.displayController != null) {
            this.displayController.init();
        }
        this.updateVisualStyle();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            //UIState
            case Parameter.VISIBLE_BINS, ParameterDiscretizationModel.PERCENTILE_FILTER -> {
                this.onNetworkChanged();
                var taskManager = registrar.getService(TaskManager.class);
                taskManager.execute(NetworkController.createLayoutTask(registrar, this.view));
            }
        }
    }

    public void updateVisualStyle() {
        var newStyle = createDefaultVisualStyle();
        newStyle = displayController.adjustVisualStyle(newStyle);
        var visualMappingManager = registrar.getService(VisualMappingManager.class);
        visualMappingManager.setVisualStyle(newStyle, this.view);
        visualMappingManager.removeVisualStyle(this.defaultStyle);
        this.defaultStyle = newStyle;
    }

    public void hideNodesUsingPercentiles() {
        var percentile = traceGraph.getPDM().getPercentile();
        var nodeTable = traceGraph.getNetwork().getDefaultNodeTable();
        var suids = nodeTable.getColumn("SUID").getValues(Long.class);
        var values = nodeTable.getColumn(percentile.getValue0()).getValues(Integer.class);
        var cutOff = values.stream().mapToInt(Integer::intValue).sum() * percentile.getValue1() / 100;
        List<Pair<Long, Integer>> pairs = new ArrayList<>(suids.size());
        for (int i = 0; i < suids.size(); i++) {
            pairs.add(new Pair<>(suids.get(i), values.get(i)));
        }
        pairs.sort(Comparator.comparing(Pair::getValue1));
        Collections.reverse(pairs);
        var sum = 0;
        for (var pair : pairs) {
            var node = traceGraph.getNetwork().getNode(pair.getValue0());
            view.getNodeView(node).setVisualProperty(NODE_VISIBLE, sum <= cutOff);
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

    private void setDisplayController(AbstractEdgeDisplayController displayController) {
        if (this.displayController != null) {
            if (this.displayController.getClass() == displayController.getClass()) {
                return;
            } else {
                this.displayController.dispose();
                this.defaultStyle.apply(this.view);
            }
        }

        var previousDisplayController = this.displayController;
        this.displayController = displayController;

        var visualStyle = displayController.adjustVisualStyle(this.defaultStyle);
        var mapper = registrar.getService(VisualMappingManager.class);
        if (visualStyle != null) {
            mapper.setVisualStyle(visualStyle, this.view);
        } else {
            mapper.setVisualStyle(createDefaultVisualStyle(), this.view);
        }

        this.displayController.init();
        var eventHelper = registrar.getService(CyEventHelper.class);
        eventHelper.fireEvent(new SetCurrentEdgeDisplayControllerEvent(this, previousDisplayController,
                this.displayController));
    }

    public void setMode(String mode) {
        switch (mode) {
            case RENDERING_MODE_FULL -> {
                this.setDisplayController(new DefaultEdgeDisplayController(registrar, view, this.traceGraph, this));
            }
            case RENDERING_MODE_FOLLOW -> {
                this.setDisplayController(new FollowEdgeDisplayController(registrar, view, this.traceGraph, this));
            }
            case RENDERING_MODE_TRACES -> {
                this.setDisplayController(new TracesEdgeDisplayController(this.registrar, view, this.traceGraph, 2,
                        this));
            }
        }
    }

    @Override
    public void handleEvent(SelectedNodesAndEdgesEvent event) {
        if (event.getNetwork() == this.traceGraph.getNetwork()) {
            this.displayController.handleNodesSelected(event);
        }
    }

    @Override
    public void dispose() {
        if (this.displayController != null) {
            this.displayController.dispose();
        }
        var visualMappingManager = registrar.getService(VisualMappingManager.class);
        visualMappingManager.removeVisualStyle(this.defaultStyle);
        registrar.unregisterService(this, SelectedNodesAndEdgesListener.class);
        registrar.unregisterService(this, ShowTraceEventListener.class);
        this.traceGraph.getPDM().removeObserver(this);
        this.traceGraph.getPDM().forEach(p -> {
            p.removeObserver(this);
        });
    }

    @Override
    public void handleEvent(ShowTraceEvent e) {
        if (e.getNetwork() != this.traceGraph.getNetwork()) {
            return;
        }
        if (!(this.displayController instanceof ShortestTraceEdgeDisplayController)) {
            this.setDisplayController(new ShortestTraceEdgeDisplayController(registrar, view, traceGraph,
                    e.getTrace(), this));
        }
    }

    public TraceGraphPanel getSettingsPanel() {
        return this.displayController.getSettingsPanel();
    }

    public TraceGraphController getTraceGraphController() {
        return this.traceGraphController;
    }

    public FilteredState getFilteredState() {
        return this.filteredState;
    }
}
