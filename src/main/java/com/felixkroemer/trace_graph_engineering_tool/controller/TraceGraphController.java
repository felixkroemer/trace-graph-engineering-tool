package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Mappings;
import com.felixkroemer.trace_graph_engineering_tool.util.TaskMonitorStub;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.*;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class TraceGraphController {

    private Logger logger;

    private List<TraceGraph> networks;
    private CyServiceRegistrar registrar;
    private CyNetworkManager networkManager;
    private CyNetworkViewFactory networkViewFactory;
    private CyNetworkViewManager networkViewManager;
    private CyLayoutAlgorithmManager layoutManager;
    private TaskManager taskManager;
    private CyApplicationManager applicationManager;
    private VisualMappingManager visualMappingManager;
    private VisualStyleFactory visualStyleFactory;
    private VisualMappingFunctionFactory visualMappingFunctionFactory;

    public TraceGraphController(CyServiceRegistrar registrar) {
        this.logger = this.logger = LoggerFactory.getLogger(CyUserLog.NAME);

        this.networks = new LinkedList<>();
        this.registrar = registrar;
        this.networkManager = registrar.getService(CyNetworkManager.class);
        this.networkViewFactory = registrar.getService(CyNetworkViewFactory.class);
        this.networkViewManager = registrar.getService(CyNetworkViewManager.class);
        this.layoutManager = registrar.getService(CyLayoutAlgorithmManager.class);
        this.taskManager = registrar.getService(TaskManager.class);
        this.applicationManager = registrar.getService(CyApplicationManager.class);
        this.visualMappingManager = registrar.getService(VisualMappingManager.class);
        this.visualStyleFactory = registrar.getService(VisualStyleFactory.class);
        // Ensure we get org.cytoscape.view.vizmap.internal.mappings.PassthroughMappingFactory, then cast to
        // PassthroughMapping
        this.visualMappingFunctionFactory = registrar.getService(VisualMappingFunctionFactory.class, "(mapping" +
                ".type=continuous)");
    }

    public void registerTraceGraph(TraceGraph tg) {
        CyNetworkView view = networkViewFactory.createNetworkView(tg.getNetwork());

        CyLayoutAlgorithm layoutFactory = layoutManager.getLayout("grid");
        Object context = layoutFactory.getDefaultLayoutContext();
        TaskIterator iterator = layoutFactory.createTaskIterator(view, context, CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
        Task task = null;
        // do not use taskManager.execute to hide network until initial layout is applied
        try {
            while (iterator.hasNext()) {
                iterator.next().run(new TaskMonitorStub());
            }
        } catch (Exception e) {
            logger.error("Error applying layout");
        }

        networkManager.addNetwork(tg.getNetwork());
        networkViewManager.addNetworkView(view);
        for (CyLayoutAlgorithm l : this.layoutManager.getAllLayouts()) {
            logger.info(l.getName());
        }

        VisualStyle style = visualStyleFactory.createVisualStyle("default");

        VisualMappingFunction sizeMapping = Mappings.createSizeMapping(1, 2000, visualMappingFunctionFactory);
        VisualMappingFunction colorMapping = Mappings.createColorMapping(1, 1600, visualMappingFunctionFactory);

        style.addVisualMappingFunction(sizeMapping);
        style.addVisualMappingFunction(colorMapping);
        this.visualMappingManager.setCurrentVisualStyle(style);


        style.apply(view);
    }

}
