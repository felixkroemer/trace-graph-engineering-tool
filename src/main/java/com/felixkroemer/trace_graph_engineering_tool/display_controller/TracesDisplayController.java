package com.felixkroemer.trace_graph_engineering_tool.display_controller;

import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceSetEvent;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceExtension;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.*;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;

import java.awt.*;
import java.util.*;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class TracesDisplayController extends AbstractDisplayController {

    public static final String RENDERING_MODE_TRACES = "RENDERING_MODE_TRACES";
    private static final Color[] colors = generateColorList();
    
    private int length;
    private static int colorIndex = 0;
    private CyServiceRegistrar registrar;
    private Set<CyEdge> multiEdges;
    private Map<CyEdge, TraceExtension> traceMapping;

    public TracesDisplayController(CyServiceRegistrar registrar, CyNetworkView view, TraceGraph traceGraph,
                                   int length) {
        super(registrar, view, traceGraph);
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
        CyNode node = traceGraph.findNode(sourceTable, index);
        CyNode nextNode;
        int found = 0;
        while (true) {
            index = up ? index - 1 : index + 1;
            nextNode = traceGraph.findNode(sourceTable, index);
            if (nextNode == null) {
                return;
            }
            if (nextNode != node) {
                found++;
                // also collect trailing rows that map to the same node
                if (found == length + 1) {
                    break;
                }
            }
            if (up) {
                trace.addBefore(nextNode, index);
            } else {
                trace.addAfter(nextNode, index);
            }
            node = nextNode;
        }
    }

    public static Set<TraceExtension> calculateTraces(CyIdentifiable identifiable, TraceGraph traceGraph, int length,
                                                      boolean isEdge) {
        Set<TraceExtension> traces = new HashSet<>();
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
                TraceExtension trace = new TraceExtension(startNode, sourceIndex, getNextColor());
                traces.add(trace);
                findNextNodes(sourceIndex, trace, traceGraph, sourceTable, length, true);
                findNextNodes(sourceIndex, trace, traceGraph, sourceTable, length, false);
                //TODO: add case where the node in question is also in other places but the middle of the trace
                // (loop in trace)
                for (var node : trace.getSequence()) {
                    foundIndices.add(node.getValue1());
                }
            }
        }
        return traces;
    }

    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
        if (event.getSelectedNodes().size() == 1 && event.nodesChanged()) {
            this.hideAllEdges();
            this.displayTraces(event.getSelectedNodes(), event.getSelectedEdges(), event.getNetwork());
        }
        if (event.getSelectedEdges().size() == 1) {
            var trace = this.traceMapping.get(event.getSelectedEdges().iterator().next());
            if (trace != null) {
                var eventHelper = registrar.getService(CyEventHelper.class);
                eventHelper.fireEvent(new ShowTraceEvent(this, trace, networkView.getModel()));
            }
        }
    }

    @Override
    public void init() {
        this.hideAllEdges();
        var selectedNodes = CyTableUtil.getNodesInState(networkView.getModel(), CyNetwork.SELECTED, true);
        this.displayTraces(selectedNodes, Collections.emptyList(), networkView.getModel());
    }

    public void displayTraces(Collection<CyNode> selectedNodes, Collection<CyEdge> selectedEdges, CyNetwork network) {
        network.removeEdges(this.multiEdges);
        this.multiEdges.clear();
        this.traceMapping.clear();
        Set<TraceExtension> traces = null;
        if (selectedNodes.size() == 1) {
            traces = calculateTraces(selectedNodes.iterator().next(), traceGraph, length, false);
        }
        if (selectedEdges.size() == 1) {
            traces = calculateTraces(selectedEdges.iterator().next(), traceGraph, length, true);
        }
        if (traces != null) {
            drawTraces(traces);
            CyEventHelper helper = registrar.getService(CyEventHelper.class);
            helper.fireEvent(new ShowTraceSetEvent(this, traces, network));
        }
    }

    @Override
    public void disable() {
        networkView.getModel().removeEdges(this.multiEdges);
    }

    @Override
    public String getID() {
        return RENDERING_MODE_TRACES;
    }

    public void drawTraces(Set<TraceExtension> traces) {
        colorIndex = 0;
        var usedEdges = new HashSet<CyEdge>();
        var network = networkView.getModel();
        for (var trace : traces) {
            for (int i = 0; i < trace.getSequence().size() - 1; i++) {
                // is null if the edge is a self edge
                CyEdge edge = this.traceGraph.getEdge(trace.getSequence().get(i).getValue0(),
                        trace.getSequence().get(i + 1).getValue0());
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

}

