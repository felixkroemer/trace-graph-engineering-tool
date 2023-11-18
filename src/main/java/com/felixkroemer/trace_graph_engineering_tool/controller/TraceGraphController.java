package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentTraceGraphControllerEvent;
import com.felixkroemer.trace_graph_engineering_tool.model.FilteredState;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.CustomTreeTableModel;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.CustomTreeTableNode;
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
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableModel;

import java.util.*;

import static org.cytoscape.view.presentation.property.table.BasicTableVisualLexicon.COLUMN_VISIBLE;

public class TraceGraphController extends NetworkController implements SetCurrentNetworkListener, CyDisposable {

    private final TraceGraph traceGraph;
    private final RenderingController renderingController;

    public TraceGraphController(CyServiceRegistrar registrar, TraceGraph traceGraph) {
        super(registrar, traceGraph.getNetwork(), traceGraph.getPDM());
        this.traceGraph = traceGraph;
        //TODO set initial filtered state
        this.renderingController = new RenderingController(registrar, this);

        this.registrar.registerService(this, SetCurrentNetworkListener.class, new Properties());
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
        var iterator = new TaskIterator();
        iterator.append(new AbstractTask() {
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                CyEventHelper helper = registrar.getService(CyEventHelper.class);
                //helper.silenceEventSource(traceGraph.getNetwork().getDefaultNodeTable());
                traceGraph.onParameterChanged(changedParameter);
                //helper.unsilenceEventSource(traceGraph.getNetwork().getDefaultNodeTable());
                helper.flushPayloadEvents();
                renderingController.onNetworkChanged();

            }
        });
        var taskManager = this.registrar.getService(SynchronousTaskManager.class);
        taskManager.execute(iterator);
    }

    @Override
    public Map<String, String> getNodeInfo(CyNode node) {
        return this.traceGraph.getNodeInfo(node);
    }

    @Override
    public TreeTableModel createSourceRowTableModel(CyNode node, DefaultMutableTreeTableNode root) {
        for (CyTable table : this.traceGraph.getSourceTables()) {
            var aux = traceGraph.getNodeAux(node);
            var rows = aux.getSourceRows(table);
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
    public TreeTableModel createNetworkTableModel(DefaultMutableTreeTableNode root) {
        root.add(new CustomTreeTableNode("Nodes", this.network.getNodeCount()));
        root.add(new CustomTreeTableNode("Edges", this.network.getEdgeCount()));

        var sourceTablesNode = new CustomTreeTableNode("Source Tables", "");
        for (CyTable sourceTable : this.traceGraph.getSourceTables()) {
            var tableNode = new CustomTreeTableNode(sourceTable.getTitle(), "");
            var rowsNode = new CustomTreeTableNode("Rows", sourceTable.getRowCount());
            tableNode.add(rowsNode);
            sourceTablesNode.add(tableNode);
        }

        root.add(sourceTablesNode);
        return new CustomTreeTableModel(root);
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
        TraceGraph newTg = this.traceGraph.extractTraceGraph(subNetwork, new HashSet<>(tables));
        this.renderingController.onNetworkChanged();
        this.applyStyleAndLayout();
        return new TraceGraphController(registrar, newTg);
    }

    public void mergeTraceGraph(TraceGraphController controller) {
        var networkManager = registrar.getService(CyNetworkManager.class);
        var networkTableManager = this.registrar.getService(CyNetworkTableManager.class);
        controller.dispose();
        var network = controller.getNetwork();
        networkManager.destroyNetwork(network);
        for (var sourceTable : controller.getTraceGraph().getSourceTables()) {
            networkTableManager.setTable(this.getNetwork(), CyNode.class, "" + sourceTable.hashCode(), sourceTable);
            this.traceGraph.init(sourceTable);
        }
        CyEventHelper helper = registrar.getService(CyEventHelper.class);
        helper.flushPayloadEvents();
        this.renderingController.onNetworkChanged();
        this.applyStyleAndLayout();
    }

    /**
     * Creates a panel for configuring the TraceGraphController
     * The generated panel depends on the currently selected DisplayController
     */
    public EdgeDisplayControllerPanel getSettingsPanel() {
        return this.renderingController.getSettingsPanel();
    }

    @Override
    public TaskIterator getApplyStyleTask() {
        var iter = super.getApplyStyleTask();
/*        iter.append(new AbstractTask() {
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                for (var edgeView : getView().getEdgeViews()) {
                    edgeView.setVisualProperty(EDGE_VISIBLE, false);
                }
            }
        });*/
        return iter;
    }

    @Override
    public void dispose() {
        this.renderingController.dispose();
        this.registrar.unregisterService(this, SetCurrentNetworkListener.class);
    }

    public FilteredState getFilteredState() {
        return this.renderingController.getFilteredState();
    }
}
