package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.view.model.CyNetworkView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class TracesDisplayManager extends AbstractDisplayManager {

    private static Color[] colors = generateColorList();

    private Logger logger;

    private int length;
    private CyTable nodeTable;
    private CyTable sourceTable;
    private int colorIndex = 0;

    public TracesDisplayManager(CyNetworkView view, TraceGraph traceGraph, int length) {
        super(view, traceGraph);
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.length = length;
        this.nodeTable = this.traceGraph.getNetwork().getDefaultNodeTable();
        this.sourceTable = this.traceGraph.getSourceTable();
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
            v.setVisualProperty(EDGE_WIDTH, 4.0);
            v.setVisualProperty(EDGE_STROKE_UNSELECTED_PAINT, color);
            v.setVisualProperty(EDGE_TARGET_ARROW_UNSELECTED_PAINT, color);
            v.setVisualProperty(EDGE_STROKE_SELECTED_PAINT, color);
            v.setVisualProperty(EDGE_TARGET_ARROW_SELECTED_PAINT, color);
            v.setVisualProperty(EDGE_VISIBLE, true);
        });
    }

    @Override
    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
        if (event.nodesChanged()) {
            this.hideAllEdges();
            for (var node : event.getSelectedNodes()) {
                StringBuilder sb = new StringBuilder();
                List<Integer> sourceRows = nodeTable.getRow(node.getSUID()).getList(Columns.NODE_SOURCE_ROWS,
                        Integer.class);
                colorIndex = 0;
                for (int i : sourceRows) {
                    Color color = getNextColor();
                    sb.append("Color: ").append(color.toString());
                    CyNode previousNode = null;
                    for (int j = -length; j <= length; j++) {
                        if (i + j > 0 && i + j <= this.sourceTable.getRowCount()) {
                            CyNode n = traceGraph.findNode(i + j);
                            sb.append(i + j).append(j < length ? "-> " : "");
                            if (previousNode != null && previousNode != n) {
                                CyEdge edge = traceGraph.getEdge(previousNode, n);
                                this.colorEdge(edge, color);
                            }
                            previousNode = n;
                        }
                    }
                }
                logger.info(sb.toString());
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
