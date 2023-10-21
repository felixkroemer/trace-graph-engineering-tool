package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.*;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadTraceTask extends AbstractTask {

    @Tunable(description = "The trace to load", params = "input=true", required = true)
    public File traceFile;

    private final Logger logger;

    private final TraceGraphManager manager;
    private final CyTableFactory tableFactory;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkTableManager networkTableManager;
    private final CyTableManager tableManager;
    private final CyServiceRegistrar registrar;

    public LoadTraceTask(CyServiceRegistrar reg) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.manager = reg.getService(TraceGraphManager.class);
        this.tableFactory = reg.getService(CyTableFactory.class);
        this.networkFactory = reg.getService(CyNetworkFactory.class);
        this.networkTableManager = reg.getService(CyNetworkTableManager.class);
        this.tableManager = reg.getService(CyTableManager.class);
        this.registrar = reg;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        CyTable sourceTable = tableFactory.createTable(traceFile.getName(), Columns.SOURCE_ID, Long.class, true, true);
        sourceTable.setTitle(traceFile.getName());
        Util.parseCSV(sourceTable, traceFile);
        List<String> params = new ArrayList<>();
        sourceTable.getColumns().forEach(c -> {
            if (!c.getName().equals(Columns.SOURCE_ID)) params.add(c.getName());
        });
        ParameterDiscretizationModel pdm = manager.findPDM(params);
        if (pdm != null) {
            this.tableManager.addTable(sourceTable);
            var subNetwork = Util.createSubNetwork(pdm, Util.getSubNetworkName(Collections.singleton(sourceTable)));
            this.networkTableManager.setTable(subNetwork, CyNode.class, sourceTable.getTitle(), sourceTable);
            var traceGraph = new TraceGraph(subNetwork, pdm);
            traceGraph.init(sourceTable);
            TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
            manager.registerTraceGraph(pdm, controller);
        } else {
            throw new Error("No matching PDM was found.");
        }
    }
}
