package com.felixkroemer.trace_graph_engineering_tool.display_controller;

import com.felixkroemer.trace_graph_engineering_tool.controller.RenderingController;
import com.felixkroemer.trace_graph_engineering_tool.model.NodeAuxiliaryInformation;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.model.*;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualStyle;

import java.util.*;
import java.util.stream.Collectors;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class FollowEdgeDisplayController extends AbstractEdgeDisplayController {

    public static final String RENDERING_MODE_FOLLOW = "RENDERING_MODE_SELECTED";
    private CyNode previousNode;
    private Path path;

    public FollowEdgeDisplayController(CyServiceRegistrar registrar, CyNetworkView view, TraceGraph traceGraph, RenderingController renderingController) {
        super(registrar, view, traceGraph, renderingController);
    }

    @Override
    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
        if (event.edgesChanged() && event.getSelectedEdges().size() == 1 && this.previousNode != null) {
            CyEdge edge = event.getSelectedEdges().iterator().next();
            CyNode neighbor = edge.getTarget() == previousNode ? edge.getSource() : edge.getTarget();
            traceGraph.getNetwork().getRow(edge).set(CyNetwork.SELECTED, false);
            traceGraph.getNetwork().getRow(neighbor).set(CyNetwork.SELECTED, true);
            networkView.setVisualProperty(NETWORK_CENTER_X_LOCATION, networkView.getNodeView(neighbor).getVisualProperty(NODE_X_LOCATION));
            networkView.setVisualProperty(NETWORK_CENTER_Y_LOCATION, networkView.getNodeView(neighbor).getVisualProperty(NODE_Y_LOCATION));
            this.path.addNode(traceGraph.getNodeAux(neighbor));
        } else if (event.nodesChanged() && event.getSelectedNodes().size() == 1) {
            var node = event.getSelectedNodes().iterator().next();
            this.previousNode = node;
            showEdgesOfSelectedNodes();
            if (this.path == null) {
                this.path = new Path(traceGraph.getNodeAux(node));
            }
        } else if (event.nodesChanged() && event.getSelectedNodes().isEmpty()) {
            //TODO: find way to detect deselection, selecting an edge also triggers node deselection
            //this.path = null;
        }
    }

    @Override
    public void init() {
        showEdgesOfSelectedNodes();
    }

    @Override
    public void dispose() {
    }

    @Override
    public VisualStyle adjustVisualStyle(VisualStyle defaultVisualStyle) {
        return defaultVisualStyle;
    }

    @Override
    public String getID() {
        return RENDERING_MODE_FOLLOW;
    }

    private void showEdgesOfSelectedNodes() {
        this.hideAllEdges();
        CyNetwork network = networkView.getModel();
        var selectedNodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
        for (var node : selectedNodes) {
            var adjacentEdges = network.getAdjacentEdgeList(node, CyEdge.Type.DIRECTED);
            for (var edge : adjacentEdges) {
                if (edge.getSource() != node) {
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                var nodeTable = this.networkView.getModel().getDefaultNodeTable();
                for (int i = 0; i < this.traceGraph.getPDM().getParameterCount(); i++) {
                    Parameter param = this.traceGraph.getPDM().getParameters().get(i);
                    var sourceValue = nodeTable.getRow(edge.getSource().getSUID()).get(param.getName(), Integer.class);
                    var targetValue = nodeTable.getRow(edge.getTarget().getSUID()).get(param.getName(), Integer.class);
                    if (!sourceValue.equals(targetValue)) {
                        sb.append(param.getName()).append(" : ").append(sourceValue - targetValue).append("\n");
                    }
                }
                var edgeView = networkView.getEdgeView(edge);
                // when TracGraphs are merged, the selectedNodesAndEdges event may be fired before the views are
                // created in NetworkMediator
                // the correct edges will still be displayed when init is called in onNetworkChanged
                if (edgeView != null) {
                    edgeView.setVisualProperty(EDGE_VISIBLE, true);
                    edgeView.setVisualProperty(EDGE_WIDTH, 5.0);
                    edgeView.setVisualProperty(EDGE_TOOLTIP, sb.toString());
                }
            }
        }
    }
}

class Path {

    private List<PathElement> elements;

    public Path(NodeAuxiliaryInformation nodeAux) {
        this.elements = new ArrayList<>();
        this.elements.add(new PathElement(nodeAux));
    }

    void addNode(NodeAuxiliaryInformation nodeAux) {
        if (!this.elements.get(this.elements.size() - 1).addNode(nodeAux)) {
            var element = new PathElement(nodeAux);
            this.elements.add(element);
        }
    }
}

class PathElement {

    Map<CyTable, List<List<Integer>>> x;
    int length;

    public PathElement(NodeAuxiliaryInformation nodeAux) {
        this.x = new HashMap<>();
        this.length = 1;
        for (var trace : nodeAux.getSourceTables()) {
            this.x.put(trace, new LinkedList<>());
            for (var situation : nodeAux.getSourceRows(trace)) {
                var list = new LinkedList<Integer>();
                list.add(situation);
                x.get(trace).add(list);
            }
        }
    }

    boolean addNode(NodeAuxiliaryInformation nodeAux) {
        this.length += 1;
        if (Collections.disjoint(this.x.keySet(), nodeAux.getSourceTables())) {
            return false;
        }
        boolean found = false;
        this.x = this.x.entrySet().stream().filter(e -> nodeAux.getSourceTables().contains(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        for (var entry : this.x.entrySet()) {
            var situations = nodeAux.getSourceRows(entry.getKey());
            for (var list : entry.getValue()) {
                for (var situation : situations) {
                    if (list.get(list.size() - 1) == situation - 1) {
                        list.add(situation);
                        found = true;
                        break;
                    }
                }
            }
        }
        if (!found) {
            return false;
        }
        for (var entry : this.x.entrySet()) {
            entry.getValue().removeIf(l -> l.size() < this.length);
        }
        return true;
    }
}