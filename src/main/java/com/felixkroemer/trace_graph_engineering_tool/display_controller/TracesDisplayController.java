package com.felixkroemer.trace_graph_engineering_tool.display_controller;

import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceSetEvent;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceExtension;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.*;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.beans.PropertyChangeSupport;
import java.util.*;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class TracesDisplayController extends AbstractDisplayController {

    private static Color[] colors = generateColorList();

    private Logger logger;

    private int length;
    private static int colorIndex = 0;
    private HashMap<CyEdge, Integer> edgeVisits;
    private PropertyChangeSupport pcs;
    private CyServiceRegistrar registrar;
    private boolean enableVisitWidth;

    public TracesDisplayController(CyServiceRegistrar registrar, CyNetworkView view, TraceGraph traceGraph,
                                   int length) {
        super(registrar, view, traceGraph);
        this.registrar = registrar;
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.length = length;
        this.edgeVisits = new HashMap<>();
        this.pcs = new PropertyChangeSupport(this);
        this.enableVisitWidth = false;

        this.hideAllEdges();
        var selectedNodes = CyTableUtil.getNodesInState(view.getModel(), CyNetwork.SELECTED, true);
        this.displayTraces(selectedNodes, Collections.emptyList(), view.getModel());
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
            if (enableVisitWidth) {
                Integer visits;
                if ((visits = this.edgeVisits.get(e)) != null) {
                    visits = visits + 1;
                } else {
                    visits = 1;
                }
                this.edgeVisits.put(e, visits);
                // crashes if Integer is passed
                v.setVisualProperty(EDGE_WIDTH, visits * 1.5);
            }
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
        this.hideAllEdges();
        this.displayTraces(event.getSelectedNodes(), event.getSelectedEdges(), event.getNetwork());
    }

    public void displayTraces(Collection<CyNode> selectedNodes, Collection<CyEdge> selectedEdges, CyNetwork network) {
        this.edgeVisits.clear();
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
    }

    public void drawTraces(Set<TraceExtension> traces) {
        colorIndex = 0;
        for (var trace : traces) {
            for (int i = 0; i < trace.getSequence().size() - 1; i++) {
                CyEdge edge;
                // is null if the edge is a self edge
                if ((edge = this.traceGraph.getEdge(trace.getSequence().get(i).getValue0(),
                        trace.getSequence().get(i + 1).getValue0())) != null) {
                    this.colorEdge(edge, trace.getColor());
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

