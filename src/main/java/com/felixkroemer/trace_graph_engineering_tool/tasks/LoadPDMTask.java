package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;

public class LoadPDMTask extends AbstractTask {

    @Tunable(description = "The trace to load", params = "input=true", required = true)
    public File traceFile;

    private final Logger logger;

    private final TraceGraphManager manager;
    private final CyTableFactory tableFactory;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkTableManager networkTableManager;
    private final CyTableManager tableManager;
    private final CyServiceRegistrar registrar;

    public LoadPDMTask(CyServiceRegistrar reg) {
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
        ParameterDiscretizationModelDTO dto = parsePDM();

        ParameterDiscretizationModel pdm = new ParameterDiscretizationModel(dto);
        var subNetwork = (CySubNetwork) networkFactory.createNetwork();
        var rootNetwork = subNetwork.getRootNetwork();
        pdm.setRootNetwork(rootNetwork);

        var sharedNodeTable = rootNetwork.getSharedNodeTable();
        pdm.forEach(p -> sharedNodeTable.createColumn(p.getName(), Integer.class, false));
        var localNetworkTable = subNetwork.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS);
        localNetworkTable.createColumn(Columns.NETWORK_TG_MARKER, Integer.class, true);

        rootNetwork.getDefaultNetworkTable().getRow(rootNetwork.getSUID()).set(CyNetwork.NAME, pdm.getName());

        var traceGraph = new TraceGraph(subNetwork, pdm);
        for (String csv : dto.getCsvs()) {
            var path = new File(traceFile.getParentFile(), csv);
            var sourceTable = tableFactory.createTable(csv, Columns.SOURCE_ID, Long.class, true, true);
            sourceTable.setTitle(csv);
            Util.parseCSV(sourceTable, path);
            this.tableManager.addTable(sourceTable);
            this.networkTableManager.setTable(subNetwork, CyNode.class, "" + sourceTable.hashCode(), sourceTable);
            traceGraph.init(sourceTable);
        }

        subNetwork.getRow(subNetwork).set(CyNetwork.NAME, Util.getSubNetworkName(traceGraph.getSourceTables()));

        TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
        manager.registerTraceGraph(pdm, controller);
    }

    private ParameterDiscretizationModelDTO parsePDM() throws Exception {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String pdmString = Files.readString(traceFile.toPath());
        ParameterDiscretizationModelDTO dto = gson.fromJson(pdmString, ParameterDiscretizationModelDTO.class);
        return dto;
    }

}
