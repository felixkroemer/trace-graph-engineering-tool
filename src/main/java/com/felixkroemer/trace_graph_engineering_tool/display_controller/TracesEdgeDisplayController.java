package com.felixkroemer.trace_graph_engineering_tool.display_controller;

import com.felixkroemer.trace_graph_engineering_tool.controller.RenderingController;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEvent;
import com.felixkroemer.trace_graph_engineering_tool.model.DrawableSubtrace;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels.EdgeDisplayControllerPanel;
import com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels.TracesPanel;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.*;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualStyle;
import org.javatuples.Pair;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class TracesEdgeDisplayController extends EdgeDisplayController {

    public static final String RENDERING_MODE_TRACES = "RENDERING_MODE_TRACES";
    public static final String TRACES = "traces";
    private static final Color[] colors = generateColorList();
    private static int colorIndex = 0;
    private int length;
    private CyServiceRegistrar registrar;
    private List<DrawableSubtrace> traces;
    private Pair<Integer, Integer> displayRange;
    private Set<CyEdge> multiEdges;
    private Set<CyEdge> highlightedEdges;
    private Map<CyEdge, DrawableSubtrace> traceMapping;

    public TracesEdgeDisplayController(CyServiceRegistrar registrar, CyNetworkView view, TraceGraph traceGraph,
                                       int length, RenderingController renderingController) {
        super(registrar, view, traceGraph, renderingController);
        this.registrar = registrar;
        this.length = length;
        this.multiEdges = new HashSet<>();
        this.highlightedEdges = new HashSet<>();
        this.traceMapping = new HashMap<>();
    }

    // https://stackoverflow.com/questions/470690/how-to-automatically-generate-n-distinct-colors
    private static Color[] generateColorList() {
        return new Color[]{new Color(0, 0, 0), new Color(87, 87, 87), new Color(173, 35, 35), new Color(42, 75, 215), new Color(29, 105, 20), new Color(129, 74, 25), new Color(129, 38, 192), new Color(160, 160, 160), new Color(129, 197, 122), new Color(157, 175, 255), new Color(41, 208, 208), new Color(255, 146, 51), new Color(255, 238, 51), new Color(233, 222, 187), new Color(255, 205, 243), new Color(255, 255, 255)};
    }

    public static void findNextNodes(int index, DrawableSubtrace subTrace, TraceGraph traceGraph, CyTable trace,
                                     int length, boolean up) {
        CyNode currentNode = traceGraph.findNode(trace, index);
        CyNode nextNode;
        int found = 0;
        while (true) {
            index = up ? index - 1 : index + 1;
            if (index < 1 || index > trace.getRowCount() || found == length) {
                return;
            }
            // must exist
            nextNode = traceGraph.findNode(trace, index);

            if (up) {
                subTrace.addBefore(nextNode);
            } else {
                subTrace.addAfter(nextNode);
            }
            if (nextNode != currentNode) {
                found++;
            }
            currentNode = nextNode;
        }
    }

    public static List<DrawableSubtrace> calculateTraces(CyIdentifiable identifiable, TraceGraph traceGraph, int length,
                                                         boolean isEdge) {
        List<DrawableSubtrace> subTraces = new ArrayList<>();
        Collection<Integer> situations;
        for (CyTable trace : traceGraph.getTraces()) {
            Set<Integer> foundIndices = new HashSet<>();
            CyNode startNode;
            if (isEdge) {
                CyEdge edge = (CyEdge) identifiable;
                situations = traceGraph.getEdgeAux(edge).getSituations(trace);
                startNode = ((CyEdge) identifiable).getSource();
            } else {
                startNode = ((CyNode) identifiable);
                situations = traceGraph.getNodeAux(startNode).getSituations(trace);
            }
            if (situations == null) {
                continue;
            } else {
                situations = new ArrayList<>(situations);
            }
            var iterator = situations.iterator();
            while (iterator.hasNext()) {
                int sourceIndex = iterator.next();
                iterator.remove();
                if (foundIndices.contains(sourceIndex)) {
                    continue;
                }
                DrawableSubtrace subTrace = new DrawableSubtrace(trace, startNode, sourceIndex, traceGraph, getNextColor());
                subTraces.add(subTrace);
                findNextNodes(sourceIndex, subTrace, traceGraph, trace, length, true);
                findNextNodes(sourceIndex, subTrace, traceGraph, trace, length, false);
                for (int i = 0; i < subTrace.getSequence().size(); i++) {
                    foundIndices.add(subTrace.getStartIndex() + i);
                }
            }
        }
        return subTraces;
    }

    private static Color getNextColor() {
        if (colorIndex == colors.length) {
            colorIndex = 0;
        }
        colorIndex++;
        return colors[colorIndex - 1];
    }

    private void colorEdge(CyEdge e, Color color) {
        networkView.getEdgeView(e).batch(v -> {
            v.setVisualProperty(EDGE_WIDTH, 2.0);
            v.setVisualProperty(EDGE_STROKE_UNSELECTED_PAINT, color);
            v.setVisualProperty(EDGE_TARGET_ARROW_UNSELECTED_PAINT, color);
            v.setVisualProperty(EDGE_VISIBLE, true);
        });
    }

    @Override
    public void prepareForOperation() {
        this.networkView.getModel().removeEdges(this.multiEdges);
        this.multiEdges.clear();
    }

    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
        if (event.nodesChanged() && event.getSelectedNodes().size() == 1) {
            this.traces = this.calculateTraces(event.getSelectedNodes(), event.getSelectedEdges(), event.getNetwork());
            if (this.traces != null) {
                this.displayRange = new Pair<>(0, Math.min(traces.size(), 12));
                this.pcs.firePropertyChange(TracesEdgeDisplayController.TRACES, null, this.traces);
                drawTraces();
            }
        }
        if (event.edgesChanged() && event.getSelectedEdges().size() == 1) {
            var trace = this.traceMapping.get(event.getSelectedEdges().iterator().next());
            if (trace != null) {
                var eventHelper = registrar.getService(CyEventHelper.class);
                eventHelper.fireEvent(new ShowTraceEvent(this, trace, networkView.getModel()));
            }
        }
        if (event.getSelectedNodes().isEmpty()) {
            this.traces = null;
            this.displayRange = null;
            this.pcs.firePropertyChange(new PropertyChangeEvent(this, TracesEdgeDisplayController.TRACES, null, null));
            this.unhighlightEdges();
        }
    }

    public void init() {
        this.hideAllEdges();
        var selectedNodes = CyTableUtil.getNodesInState(networkView.getModel(), CyNetwork.SELECTED, true);
        this.traces = this.calculateTraces(selectedNodes, Collections.emptyList(), networkView.getModel());
        if (this.traces != null) {
            this.displayRange = new Pair<>(0, Math.min(traces.size(), 12));
            this.pcs.firePropertyChange(new PropertyChangeEvent(this, TracesEdgeDisplayController.TRACES, null, this.traces));
            drawTraces();
        }
    }

    @Override
    public void dispose() {
        networkView.getModel().removeEdges(this.multiEdges);
    }

    public List<DrawableSubtrace> calculateTraces(Collection<CyNode> selectedNodes, Collection<CyEdge> selectedEdges,
                                                  CyNetwork network) {
        network.removeEdges(this.multiEdges);
        this.multiEdges.clear();
        this.traceMapping.clear();

        if (!selectedNodes.isEmpty() || !selectedEdges.isEmpty()) {
            List<DrawableSubtrace> traces = new LinkedList<>();
            if (selectedNodes.size() == 1) {
                traces = calculateTraces(selectedNodes.iterator().next(), traceGraph, length, false);
            }
            if (selectedEdges.size() == 1) {
                traces = calculateTraces(selectedEdges.iterator().next(), traceGraph, length, true);
            }
            traces.sort(Comparator.comparingInt(DrawableSubtrace::getWeight).reversed());
            return traces;
        }
        return null;
    }

    @Override
    public VisualStyle adjustVisualStyle(VisualStyle defaultVisualStyle) {
        return defaultVisualStyle;
    }

    @Override
    public String getID() {
        return RENDERING_MODE_TRACES;
    }

    public void drawTraces() {
        this.drawTraces(this.displayRange.getValue0(), this.displayRange.getValue1());
    }

    public void drawTraces(int from, int to) {
        this.hideAllEdges();
        if (this.traces == null) {
            return;
        }
        colorIndex = 0;
        var usedEdges = new HashSet<CyEdge>();
        var network = networkView.getModel();
        for (int i = from; i < to; i++) {
            var trace = traces.get(i);
            for (int j = 0; j < trace.getSequence().size() - 1; j++) {
                // is null if the edge is a self edge
                CyEdge edge = this.traceGraph.getEdge(trace.getSequence().get(j), trace.getSequence().get(j + 1));
                if (edge != null) {
                    if (usedEdges.contains(edge)) {
                        edge = network.addEdge(edge.getSource(), edge.getTarget(), true);
                        var eventHelper = registrar.getService(CyEventHelper.class);
                        eventHelper.flushPayloadEvents();
                        this.colorEdge(edge, trace.getColor());
                        this.multiEdges.add(edge);
                    } else {
                        this.colorEdge(edge, trace.getColor());
                    }
                    usedEdges.add(edge);
                    this.traceMapping.put(edge, trace);
                }
            }
        }
    }

    public void highlightTrace(DrawableSubtrace trace) {
        this.unhighlightEdges();
        if (!traces.contains(trace)) {
            throw new IllegalArgumentException("Trace does not exist");
        }
        var uniqueSequence = trace.getUniqueSequence();
        for (int i = 0; i < uniqueSequence.size() - 1; i++) {
            var source = uniqueSequence.get(i);
            var target = uniqueSequence.get(i + 1);
            List<CyEdge> edges = this.traceGraph.getNetwork()
                                                .getConnectingEdgeList(source, target, CyEdge.Type.DIRECTED);
            for (CyEdge edge : edges) {
                var view = networkView.getEdgeView(edge);
                if (edge.getTarget() == target && edge.getSource() == source && view.getVisualProperty(EDGE_STROKE_UNSELECTED_PAINT) == trace.getColor()) {
                    networkView.getEdgeView(edge).setVisualProperty(EDGE_WIDTH, 6.0);
                    this.highlightedEdges.add(edge);
                }
            }
        }
    }

    public void unhighlightEdges() {
        for (CyEdge edge : this.highlightedEdges) {
            // edge may have been deleted already
            var view = networkView.getEdgeView(edge);
            if (view != null) {
                view.setVisualProperty(EDGE_WIDTH, 2.0);
            }
        }
        this.highlightedEdges.clear();
    }

    public EdgeDisplayControllerPanel getSettingsPanel() {
        return new TracesPanel(this);
    }

    public Pair<Integer, Integer> getDisplayRange() {
        return this.displayRange;
    }

    public void setDisplayRange(int from, int to) {
        this.displayRange = new Pair<>(from, to);
        this.drawTraces();
    }

    public int getLength() {
        return this.length;
    }

    public void setLength(int length) {
        this.length = length;
        var network = this.networkView.getModel();
        var selectedNodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
        var selectedEdges = CyTableUtil.getEdgesInState(network, CyNetwork.SELECTED, true);
        this.traces = this.calculateTraces(selectedNodes, selectedEdges, network);
        this.pcs.firePropertyChange(TracesEdgeDisplayController.TRACES, null, this.traces);
        drawTraces();
    }

    public List<DrawableSubtrace> getTraces() {
        return this.traces;
    }

    public void addObserver(PropertyChangeListener l) {
        this.pcs.addPropertyChangeListener(TracesEdgeDisplayController.TRACES, l);
    }

    public void removeObserver(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}

