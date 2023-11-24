package com.felixkroemer.trace_graph_engineering_tool.display_controller;

import com.felixkroemer.trace_graph_engineering_tool.controller.RenderingController;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEvent;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceExtension;
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

public class TracesEdgeDisplayController extends AbstractEdgeDisplayController {

    public static final String RENDERING_MODE_TRACES = "RENDERING_MODE_TRACES";
    public static final String TRACES = "traces";
    private static final Color[] colors = generateColorList();

    private int length;
    private static int colorIndex = 0;
    private CyServiceRegistrar registrar;
    private List<TraceExtension> traces;
    private Pair<Integer, Integer> displayRange;
    private Set<CyEdge> multiEdges;
    private Map<CyEdge, TraceExtension> traceMapping;

    public TracesEdgeDisplayController(CyServiceRegistrar registrar, CyNetworkView view, TraceGraph traceGraph,
                                       int length, RenderingController renderingController) {
        super(registrar, view, traceGraph, renderingController);
        this.registrar = registrar;
        this.length = length;
        this.multiEdges = new HashSet<>();
        this.traceMapping = new HashMap<>();
    }

    // https://stackoverflow.com/questions/470690/how-to-automatically-generate-n-distinct-colors
    private static Color[] generateColorList() {
        return new Color[]{new Color(0, 0, 0), new Color(87, 87, 87), new Color(173, 35, 35), new Color(42, 75, 215),
                new Color(29, 105, 20), new Color(129, 74, 25), new Color(129, 38, 192), new Color(160, 160, 160),
                new Color(129, 197, 122), new Color(157, 175, 255), new Color(41, 208, 208), new Color(255, 146, 51),
                new Color(255, 238, 51), new Color(233, 222, 187), new Color(255, 205, 243), new Color(255, 255, 255)};
    }

    private void colorEdge(CyEdge e, Color color) {
        networkView.getEdgeView(e).batch(v -> {
            v.setVisualProperty(EDGE_WIDTH, 2.0);
            v.setVisualProperty(EDGE_STROKE_UNSELECTED_PAINT, color);
            v.setVisualProperty(EDGE_TARGET_ARROW_UNSELECTED_PAINT, color);
            v.setVisualProperty(EDGE_VISIBLE, true);
        });
    }


    public static void findNextNodes(int index, TraceExtension trace, TraceGraph traceGraph, CyTable sourceTable,
                                     int length, boolean up) {
        CyNode currentNode = traceGraph.findNode(sourceTable, index);
        CyNode nextNode;
        int found = 0;
        while (true) {
            index = up ? index - 1 : index + 1;
            if (index < 1 || index > sourceTable.getRowCount() || found == length) {
                return;
            }
            // must exist
            nextNode = traceGraph.findNode(sourceTable, index);
            if (nextNode != currentNode) {
                if (up) {
                    trace.addBefore(nextNode);
                } else {
                    trace.addAfter(nextNode);
                }
                found++;
            }
            currentNode = nextNode;
        }
    }

    public static List<TraceExtension> calculateTraces(CyIdentifiable identifiable, TraceGraph traceGraph, int length
            , boolean isEdge) {
        List<TraceExtension> traces = new ArrayList<>();
        Collection<Integer> sourceRows;
        for (CyTable sourceTable : traceGraph.getSourceTables()) {
            Set<Integer> foundIndices = new HashSet<>();
            CyNode startNode;
            if (isEdge) {
                CyEdge edge = (CyEdge) identifiable;
                sourceRows = traceGraph.getEdgeAux(edge).getSourceRows(sourceTable);
                startNode = ((CyEdge) identifiable).getSource();
            } else {
                startNode = ((CyNode) identifiable);
                sourceRows = traceGraph.getNodeAux(startNode).getSourceRows(sourceTable);
            }
            if (sourceRows == null) {
                continue;
            } else {
                sourceRows = new ArrayList<>(sourceRows);
            }
            var iterator = sourceRows.iterator();
            while (iterator.hasNext()) {
                int sourceIndex = iterator.next();
                iterator.remove();
                if (foundIndices.contains(sourceIndex)) {
                    continue;
                }
                TraceExtension trace = new TraceExtension(sourceTable, startNode, sourceIndex, traceGraph,
                        getNextColor());
                trace.setPrimaryNode(startNode);
                traces.add(trace);
                findNextNodes(sourceIndex, trace, traceGraph, sourceTable, length, true);
                findNextNodes(sourceIndex, trace, traceGraph, sourceTable, length, false);
                for (int i = 0; i < trace.getSequence().size(); i++) {
                    foundIndices.add(trace.getStartIndex() + i);
                }
            }
        }
        return traces;
    }

    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
        if (event.nodesChanged() && event.getSelectedNodes().size() == 1) {
            this.traces = this.calculateTraces(event.getSelectedNodes(), event.getSelectedEdges(), event.getNetwork());
            if (this.traces != null) {
                this.displayRange = new Pair<>(0, Math.min(traces.size(), 12));
                this.pcs.firePropertyChange(new PropertyChangeEvent(this, TracesEdgeDisplayController.TRACES, null,
                        this.traces));
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
            this.pcs.firePropertyChange(new PropertyChangeEvent(this, TracesEdgeDisplayController.TRACES, null,
                    this.traces));
        }
    }

    public void init() {
        this.hideAllEdges();
        var selectedNodes = CyTableUtil.getNodesInState(networkView.getModel(), CyNetwork.SELECTED, true);
        this.traces = this.calculateTraces(selectedNodes, Collections.emptyList(), networkView.getModel());
        if (this.traces != null) {
            this.displayRange = new Pair<>(0, Math.min(traces.size(), 12));
            this.pcs.firePropertyChange(new PropertyChangeEvent(this, TracesEdgeDisplayController.TRACES, null,
                    this.traces));
            drawTraces();
        }
    }

    @Override
    public void dispose() {
        networkView.getModel().removeEdges(this.multiEdges);
    }

    public List<TraceExtension> calculateTraces(Collection<CyNode> selectedNodes, Collection<CyEdge> selectedEdges,
                                                CyNetwork network) {
        network.removeEdges(this.multiEdges);
        this.multiEdges.clear();
        this.traceMapping.clear();

        if (!selectedNodes.isEmpty() || !selectedEdges.isEmpty()) {
            List<TraceExtension> traces = new LinkedList<>();
            if (selectedNodes.size() == 1) {
                traces = calculateTraces(selectedNodes.iterator().next(), traceGraph, length, false);
            }
            if (selectedEdges.size() == 1) {
                traces = calculateTraces(selectedEdges.iterator().next(), traceGraph, length, true);
            }
            traces.sort(Comparator.comparingInt(TraceExtension::getWeight).reversed());
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

    private static Color getNextColor() {
        if (colorIndex == colors.length) {
            colorIndex = 0;
        }
        colorIndex++;
        return colors[colorIndex - 1];
    }

    public EdgeDisplayControllerPanel getSettingsPanel() {
        return new TracesPanel(registrar, this);
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
        // TODO: make range itself an observable attribute, -> dont poll number of traces from panel
        this.pcs.firePropertyChange(new PropertyChangeEvent(this, TracesEdgeDisplayController.TRACES, null,
                this.traces));
        drawTraces();
    }

    public List<TraceExtension> getTraces() {
        return this.traces;
    }

    public void addObserver(PropertyChangeListener l) {
        this.pcs.addPropertyChangeListener(TracesEdgeDisplayController.TRACES, l);
    }

    public void removeObserver(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}

