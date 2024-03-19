package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentTraceGraphControllerEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.UpdatedPDMEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.UpdatedPDMEventListener;
import com.felixkroemer.trace_graph_engineering_tool.events.UpdatedTraceGraphEvent;
import com.felixkroemer.trace_graph_engineering_tool.model.FilteredState;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.CustomTreeTableModel;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.MultiObjectTreeTableNode;
import com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels.EdgeDisplayControllerPanel;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.*;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.table.CyTableViewManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableModel;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

import static org.cytoscape.view.presentation.property.table.BasicTableVisualLexicon.COLUMN_VISIBLE;

public class TraceGraphController extends NetworkController implements SetCurrentNetworkListener, CyDisposable,
        PropertyChangeListener, UpdatedPDMEventListener {

    private final TraceGraph traceGraph;
    private final RenderingController renderingController;

    public TraceGraphController(CyServiceRegistrar registrar, TraceGraph traceGraph) {
        super(registrar, traceGraph.getNetwork(), traceGraph.getPDM());
        this.traceGraph = traceGraph;
        this.renderingController = new RenderingController(registrar, this);
        this.pdm.getParameters().forEach(p -> p.addObserver(this));

        this.registrar.registerService(this, SetCurrentNetworkListener.class, new Properties());
        this.registrar.registerService(this, UpdatedPDMEventListener.class, new Properties());
        this.registerNetwork();
        var tableManager = registrar.getService(CyTableManager.class);
        var networkTableManager = registrar.getService(CyNetworkTableManager.class);
        for (var trace : traceGraph.getTraces()) {
            tableManager.addTable(trace);
            networkTableManager.setTable(traceGraph.getNetwork(), CyNode.class, "" + trace.hashCode(), trace);
        }
    }

    @Override
    public CyNetworkView getView() {
        return this.renderingController.getView();
    }

    @Override
    public VisualStyle getVisualStyle() {
        return this.renderingController.getVisualStyle();
    }

    @Override
    public void updateNetwork(Parameter changedParameter) {
        this.renderingController.prepareForOperation();
        if (SwingUtilities.isEventDispatchThread()) {
            //traceGraph.onParameterChangedInefficient(changedParameter);
            traceGraph.onParameterChangedEfficient(changedParameter);
            renderingController.onNetworkChanged();
        } else {
            SwingUtilities.invokeLater(() -> {
                //traceGraph.onParameterChangedInefficient(changedParameter);
                traceGraph.onParameterChangedEfficient(changedParameter);
                renderingController.onNetworkChanged();
            });
        }
        CyEventHelper eventHelper = this.registrar.getService(CyEventHelper.class);
        eventHelper.fireEvent(new UpdatedTraceGraphEvent(this));
    }

    @Override
    public Map<String, String> getNodeInfo(CyNode node) {
        return this.traceGraph.getNodeInfo(node);
    }

    @Override
    public TreeTableModel createSituationTableModel(CyNode node, DefaultMutableTreeTableNode root) {
        for (CyTable table : this.traceGraph.getTraces()) {
            var aux = traceGraph.getNodeAux(node);
            var rows = aux.getSituations(table);
            if (rows != null) {
                var tableNode = new DefaultMutableTreeTableNode(table.getTitle());
                root.add(tableNode);
                for (var i : rows) {
                    tableNode.add(new DefaultMutableTreeTableNode("" + i));
                }
            }
        }
        return new DefaultTreeTableModel(root);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case Parameter.ENABLED -> {
                var tableViewManager = registrar.getService(CyTableViewManager.class);
                var nodeTableView = tableViewManager.getTableView(this.network.getDefaultNodeTable());
                Parameter param = (Parameter) evt.getSource();
                var columnView = nodeTableView.getColumnView(param.getName());
                columnView.setVisualProperty(COLUMN_VISIBLE, evt.getNewValue());
                this.updateNetwork((Parameter) evt.getSource());
            }
            case Parameter.BINS -> {
                if (!this.pdm.isUpdating()) {
                    this.updateNetwork((Parameter) evt.getSource());
                }
            }
        }
    }

    @Override
    public TreeTableModel createNetworkTableModel() {
        DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode("Root");
        root.add(new MultiObjectTreeTableNode("Nodes", this.network.getNodeCount()));
        root.add(new MultiObjectTreeTableNode("Edges", this.network.getEdgeCount()));

        var traceNode = new MultiObjectTreeTableNode("Source Tables", "");
        for (CyTable trace : this.traceGraph.getTraces()) {
            var tableNode = new MultiObjectTreeTableNode(trace.getTitle(), "");
            var rowsNode = new MultiObjectTreeTableNode("Rows", trace.getRowCount());
            tableNode.add(rowsNode);
            traceNode.add(tableNode);
        }

        root.add(traceNode);
        return new CustomTreeTableModel(root, 2);
    }

    private void hideUnneededColumns() {
        var tableViewManager = registrar.getService(CyTableViewManager.class);
        var nodeTableView = tableViewManager.getTableView(this.traceGraph.getNetwork().getDefaultNodeTable());
        var columnViews = nodeTableView.getColumnViews();
        Set<String> parameterNames = new HashSet<>();
        this.traceGraph.getPDM().forEach(p -> parameterNames.add(p.getName()));
        for (View<CyColumn> columnView : columnViews) {
            if (!parameterNames.contains(columnView.getModel().getName())) {
                columnView.setVisualProperty(COLUMN_VISIBLE, false);
            }
        }
    }

    public void setMode(String mode) {
        renderingController.setMode(mode);
    }

    public TraceGraph getTraceGraph() {
        return this.traceGraph;
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        if (e.getNetwork() == this.getNetwork()) {
            var eventHelper = this.registrar.getService(CyEventHelper.class);
            eventHelper.fireEvent(new SetCurrentTraceGraphControllerEvent(this, this));
        }
    }

    public TraceGraphController splitTraceGraph(List<CyTable> tables) {
        var subNetwork = Util.createSubNetwork(this.getPDM());
        var networkTableManager = this.registrar.getService(CyNetworkTableManager.class);
        for (CyTable table : tables) {
            networkTableManager.removeTable(this.traceGraph.getNetwork(), CyNode.class, "" + table.hashCode());
            networkTableManager.setTable(subNetwork, CyNode.class, "" + table.hashCode(), table);
        }
        this.renderingController.prepareForOperation();
        TraceGraph newTg = this.traceGraph.extractTraceGraph(subNetwork, new HashSet<>(tables));
        this.renderingController.onNetworkChanged();
        return new TraceGraphController(registrar, newTg);
    }

    public void mergeTraceGraph(TraceGraphController controller) {
        var networkManager = registrar.getService(CyNetworkManager.class);
        var networkTableManager = this.registrar.getService(CyNetworkTableManager.class);
        controller.dispose();
        this.renderingController.prepareForOperation();
        for (var trace : controller.getTraceGraph().getTraces()) {
            networkTableManager.setTable(this.getNetwork(), CyNode.class, "" + trace.hashCode(), trace);
            networkTableManager.removeTable(controller.getNetwork(), CyNode.class, "" + trace.hashCode());
            this.traceGraph.addTrace(trace);
        }
        // delete the network only after its nodes have been transferred, weird error
        // where some edge views are not created occurs otherwise
        networkManager.destroyNetwork(controller.getNetwork());
        this.renderingController.onNetworkChanged();
    }

    /**
     * Creates a panel for configuring the TraceGraphController
     * The generated panel depends on the currently selected DisplayController
     */
    public EdgeDisplayControllerPanel getSettingsPanel() {
        return this.renderingController.getSettingsPanel();
    }

    @Override
    public void dispose() {
        pdm.getParameters().forEach(p -> p.removeObserver(this));
        this.renderingController.dispose();
        this.registrar.unregisterService(this, SetCurrentNetworkListener.class);
        this.registrar.unregisterService(this, UpdatedPDMEventListener.class);
    }

    public FilteredState getFilteredState() {
        return this.renderingController.getFilteredState();
    }

    @Override
    public void handleEvent(UpdatedPDMEvent e) {
        traceGraph.refresh();
        renderingController.onNetworkChanged();
    }
}
