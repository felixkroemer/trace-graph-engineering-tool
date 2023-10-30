package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
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
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableModel;

import java.util.Map;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public abstract class NetworkController {

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

    public TaskIterator createLayoutTask() {
        return createLayoutTask(registrar, this.getView());
    }

    public static TaskIterator createLayoutTask(CyServiceRegistrar registrar, CyNetworkView view) {
        var layoutManager = registrar.getService(CyLayoutAlgorithmManager.class);
        // available as preinstalled app
        CyLayoutAlgorithm layoutFactory = layoutManager.getLayout("force-directed-cl");
        var context = (CLLayoutContext) layoutFactory.getDefaultLayoutContext();
        //context.numIterations = 300;
        context.defaultSpringLength = 100;
        //context.numIterationsEdgeRepulsive = 10;
        context.defaultNodeMass = 10;

        return layoutFactory.createTaskIterator(view, context, CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
    }

    public void registerNetwork() {
        var networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.addNetwork(this.network);
        var networkViewManager = registrar.getService(CyNetworkViewManager.class);
        networkViewManager.addNetworkView(this.getView());
        this.applyStyleAndLayout();
    }

    public void applyStyleAndLayout() {
        TaskIterator iterator = new TaskIterator();
        iterator.append(new AbstractTask() {
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                getVisualStyle().apply(getView());
            }
        });
        iterator.append(this.createLayoutTask());
        var taskManager = this.registrar.getService(TaskManager.class);
        taskManager.execute(iterator);
    }

    public abstract void destroy();

    public CyNetwork getNetwork() {
        return this.network;
    }

    public ParameterDiscretizationModel getPDM() {
        return this.pdm;
    }

    public abstract void updateNetwork(Parameter parameter);

    public abstract TreeTableModel createSourceRowTableModel(CyNode node, DefaultMutableTreeTableNode root);

    public abstract TreeTableModel createNetworkTableModel(DefaultMutableTreeTableNode root);

    public abstract Map<String, String> getNodeInfo(CyNode node);

    public void focusNode(CyNode node) {
        getView().setVisualProperty(NETWORK_CENTER_X_LOCATION,
                getView().getNodeView(node).getVisualProperty(NODE_X_LOCATION));
        getView().setVisualProperty(NETWORK_CENTER_Y_LOCATION,
                getView().getNodeView(node).getVisualProperty(NODE_Y_LOCATION));
    }

    public SelectBinsController createSelectBinsController(Parameter parameter) {
        return new SelectBinsController(parameter, registrar);
    }
}
