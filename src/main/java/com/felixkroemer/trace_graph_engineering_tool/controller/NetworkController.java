package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import org.cytoscape.model.CyDisposable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.opencl.layout.CLLayoutContext;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableModel;

import java.util.Map;
import java.util.stream.Collectors;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public abstract class NetworkController implements CyDisposable {

    protected CyServiceRegistrar registrar;
    protected CyNetwork network;
    protected ParameterDiscretizationModel pdm;

    public NetworkController(CyServiceRegistrar registrar, CyNetwork network, ParameterDiscretizationModel pdm) {
        this.registrar = registrar;
        this.network = network;
        this.pdm = pdm;
    }

    public abstract CyNetworkView getView();

    public abstract VisualStyle getVisualStyle();

    private TaskIterator createLayoutTask() {
        var layoutManager = registrar.getService(CyLayoutAlgorithmManager.class);
        // available as preinstalled app
        CyLayoutAlgorithm layoutFactory = layoutManager.getLayout("force-directed-cl");
        var context = (CLLayoutContext) layoutFactory.getDefaultLayoutContext();
        //context.numIterations = 300;
        context.defaultSpringLength = 100;
        //context.numIterationsEdgeRepulsive = 10;
        context.defaultNodeMass = 10;
        var views = getView().getNodeViews().stream().filter(v -> v.getVisualProperty(NODE_VISIBLE))
                             .collect(Collectors.toSet());
        return layoutFactory.createTaskIterator(getView(), context, views, null);
    }

    /*
    Can not be called in constructor because the view may not exist yet. Must be called by implementing classes when
    they are done initiating.
     */
    protected void registerNetwork() {
        var networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.addNetwork(this.network);
        var networkViewManager = registrar.getService(CyNetworkViewManager.class);
        networkViewManager.addNetworkView(this.getView());
        this.applyStyleAndLayout();
    }

    private TaskIterator createStyleTask() {
        return new TaskIterator(new AbstractTask() {
            @Override
            public void run(TaskMonitor taskMonitor) {
                getVisualStyle().apply(getView());
            }
        });
    }

    public void applyStyleAndLayout() {
        TaskIterator iterator = new TaskIterator();
        // applying the style would not be necessary if the style mappings used table values because the RowsSetEvent
        // would trigger a style refresh in NetworkMediator and RowsSetViewUpdater. Setting an aux value will not trigger
        // a refresh automatically
        iterator.append(this.createStyleTask());
        iterator.append(this.createLayoutTask());
        var taskManager = this.registrar.getService(SynchronousTaskManager.class);
        taskManager.execute(iterator);
    }

    @Override
    public abstract void dispose();

    public CyNetwork getNetwork() {
        return this.network;
    }

    public ParameterDiscretizationModel getPDM() {
        return this.pdm;
    }

    public abstract void updateNetwork(Parameter parameter);

    public abstract TreeTableModel createSituationTableModel(CyNode node, DefaultMutableTreeTableNode root);

    public abstract TreeTableModel createNetworkTableModel();

    public abstract Map<String, String> getNodeInfo(CyNode node);

    public void focusNode(CyNode node, boolean select) {
        getView().setVisualProperty(NETWORK_CENTER_X_LOCATION, getView().getNodeView(node)
                                                                        .getVisualProperty(NODE_X_LOCATION));
        getView().setVisualProperty(NETWORK_CENTER_Y_LOCATION, getView().getNodeView(node)
                                                                        .getVisualProperty(NODE_Y_LOCATION));
        if (select) {
            Util.deselectAll(getView());
            getView().getModel().getRow(node).set(CyNetwork.SELECTED, true);
        }
    }

    public SelectBinsController createSelectBinsController(Parameter parameter) {
        return new SelectBinsController(parameter, registrar);
    }
}
