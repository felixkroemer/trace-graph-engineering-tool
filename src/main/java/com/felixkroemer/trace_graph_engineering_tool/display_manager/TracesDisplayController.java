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
import java.util.List;
import java.util.*;

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

    private void showTraceEdges(CyIdentifiable identifiable, boolean isEdge) {
        Set<Trace> traces = new HashSet<>();
        List<CyEdge> edges;
        List<Integer> sourceRows;
        if (isEdge) {
            var table = this.traceGraph.getNetwork().getDefaultEdgeTable();
            sourceRows = table.getRow(identifiable.getSUID()).getList(Columns.EDGE_SOURCE_ROWS, Integer.class);
        } else {
            var table = this.traceGraph.getNetwork().getDefaultNodeTable();
            sourceRows = table.getRow(identifiable.getSUID()).getList(Columns.NODE_SOURCE_ROWS, Integer.class);
        }
        colorIndex = 0;
        this.edgeVisits.clear();
        for (int i : sourceRows) {
            edges = new LinkedList<>();
            Color color = getNextColor();
            CyNode previousNode = null;
            for (int j = -length; j <= length + (isEdge ? 1 : 0); j++) {
                if (i + j > 0 && i + j <= this.sourceTable.getRowCount()) {
                    CyNode n = traceGraph.findNode(i + j);
                    if (previousNode != null && previousNode != n) {
                        CyEdge edge = traceGraph.getEdge(previousNode, n);
                        edges.add(edge);
                        this.colorEdge(edge, color);
                    } else {
                        edges.add(Trace.SELF_EDGE);
                    }
                    previousNode = n;
                }
            }
            traces.add(new Trace(color, edges));
        }
        this.pcs.firePropertyChange("traces", null, traces);
    }

    @Override
    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
        if (event.nodesChanged() || event.edgesChanged()) {
            this.hideAllEdges();
        }
        if (event.getSelectedNodes().size() == 1) {
            this.showTraceEdges(event.getSelectedNodes().iterator().next(), false);
        }
        if (event.getSelectedEdges().size() == 1) {
            this.showTraceEdges(event.getSelectedEdges().iterator().next(), true);
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

