package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.view.model.CyNetworkView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class TracesDisplayController extends AbstractDisplayController {

    private static Color[] colors = generateColorList();

    private Logger logger;

    private int length;
    private CyTable sourceTable;
    private int colorIndex = 0;
    private HashMap<CyEdge, Integer> edgeVisits;
    private PropertyChangeSupport pcs;

    public TracesDisplayController(CyNetworkView view, TraceGraph traceGraph, int length) {
        super(view, traceGraph);
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.length = length;
        this.sourceTable = this.traceGraph.getSourceTable();
        this.edgeVisits = new HashMap<>();
        this.pcs = new PropertyChangeSupport(this);
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
            Integer visits;
            if ((visits = this.edgeVisits.get(e)) != null) {
                visits = visits + 1;
            } else {
                visits = 1;
            }
            this.edgeVisits.put(e, visits);
            // crashes if Integer is passed
            v.setVisualProperty(EDGE_WIDTH, visits * 1.5);
            v.setVisualProperty(EDGE_STROKE_UNSELECTED_PAINT, color);
            v.setVisualProperty(EDGE_TARGET_ARROW_UNSELECTED_PAINT, color);
            v.setVisualProperty(EDGE_VISIBLE, true);
        });
    }

    public void addObserver(PropertyChangeListener l) {
        pcs.addPropertyChangeListener("traces", l);
    }

    public void findNextNodes(int index, Trace trace, boolean up) {
        CyNode node = traceGraph.findNode(index);
        CyNode nextNode;
        int found = 0;
        while (true) {
            index = up ? index - 1 : index + 1;
            nextNode = traceGraph.findNode(index);
            if (nextNode == null) {
                return;
            }
            if (nextNode != node) {
                found++;
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

    private Set<Trace> getTraces(CyIdentifiable identifiable, boolean isEdge) {
        Set<Trace> traces = new HashSet<>();
        Set<Integer> sourceRows;
        Set<Integer> foundIndices = new HashSet<>();
        CyNode startNode;
        if (isEdge) {
            var table = this.traceGraph.getNetwork().getDefaultEdgeTable();
            sourceRows = new HashSet<>(table.getRow(identifiable.getSUID()).getList(Columns.EDGE_SOURCE_ROWS,
                    Integer.class));
            startNode = ((CyEdge) identifiable).getSource();
        } else {
            var table = this.traceGraph.getNetwork().getDefaultNodeTable();
            sourceRows = new HashSet<>(table.getRow(identifiable.getSUID()).getList(Columns.NODE_SOURCE_ROWS,
                    Integer.class));
            startNode = ((CyNode) identifiable);
        }
        this.edgeVisits.clear();
        var iterator = sourceRows.iterator();
        while (iterator.hasNext()) {
            int sourceIndex = iterator.next();
            iterator.remove();
            if (foundIndices.contains(sourceIndex)) {
                continue;
            }
            Trace trace = new Trace(startNode, sourceIndex);
            traces.add(trace);
            findNextNodes(sourceIndex, trace, true);
            findNextNodes(sourceIndex, trace, false);
            for (var node : trace.getSequence()) {
                foundIndices.add(node.getValue1());
            }
        }
        return traces;
    }

    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
        if (event.nodesChanged() || event.edgesChanged()) {
            this.hideAllEdges();
        }
        Set<Trace> traces = new HashSet<>();
        if (event.getSelectedNodes().size() == 1) {
            traces.addAll(this.getTraces(event.getSelectedNodes().iterator().next(), false));
        }
        if (event.getSelectedEdges().size() == 1) {
            traces.addAll(this.getTraces(event.getSelectedEdges().iterator().next(), true));
        }
        colorIndex = 0;
        for (var trace : traces) {
            Color color = getNextColor();
            for (int i = 0; i < trace.getSequence().size() - 1; i++) {
                CyEdge edge;
                // is null if the edge is a self edge
                if ((edge = this.traceGraph.getEdge(trace.getSequence().get(i).getValue0(),
                        trace.getSequence().get(i + 1).getValue0())) != null) {
                    this.colorEdge(edge, color);
                }
            }
        }
    }

    @Override
    public void enable() {
        this.hideAllEdges();
    }

    private Color getNextColor() {
        if (colorIndex == colors.length) {
            colorIndex = 0;
        }
        colorIndex++;
        return colors[colorIndex - 1];
    }
}

