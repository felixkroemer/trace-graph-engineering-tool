package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.source_table.TraceGraphSourceTable;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LoadPDMTask extends AbstractTask {

    @Tunable(description = "The trace to load", params = "input=true", required = true)
    public File traceFile;

    private final Logger logger;

    private final TraceGraphManager manager;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkTableManager networkTableManager;
    private final CyTableManager tableManager;
    private final CyServiceRegistrar registrar;

    public LoadPDMTask(CyServiceRegistrar reg) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.manager = reg.getService(TraceGraphManager.class);
        this.networkFactory = reg.getService(CyNetworkFactory.class);
        this.networkTableManager = reg.getService(CyNetworkTableManager.class);
        this.tableManager = reg.getService(CyTableManager.class);
        this.registrar = reg;
    }

    public void initSubNetwork(CySubNetwork subNetwork, CyRootNetwork rootNetwork, ParameterDiscretizationModel pdm) {
        var sharedNodeTable = rootNetwork.getSharedNodeTable();
        pdm.forEach(p -> sharedNodeTable.createColumn(p.getName(), Integer.class, false));
        var localNetworkTable = subNetwork.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS);
        localNetworkTable.createColumn(Columns.NETWORK_TG_MARKER, Integer.class, true);
    }

    public void loadPDM(ParameterDiscretizationModel pdm, ParameterDiscretizationModelDTO dto, String name) throws Exception {
        var subNetwork = createAndInitSubnetwork(pdm, name);
        var traceGraph = new TraceGraph(subNetwork, pdm);
        for (String csv : dto.getCsvs()) {
            var path = new File(traceFile.getParentFile(), csv);
            // exclude header
            var sourceTable = new TraceGraphSourceTable(csv, Files.lines(path.toPath()).count() - 1, registrar);
            sourceTable.setTitle(csv);
            Util.parseCSV(sourceTable, path);
            this.tableManager.addTable(sourceTable);
            this.networkTableManager.setTable(subNetwork, CyNode.class, "" + sourceTable.hashCode(), sourceTable);
            traceGraph.addSourceTable(sourceTable);
        }
        TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
        manager.registerTraceGraph(pdm, controller);
    }

    public void loadTrace() throws Exception {
        CyTable sourceTable = new TraceGraphSourceTable(traceFile.getName(),
                Files.lines(traceFile.toPath()).count() - 1, registrar);
        Util.parseCSV(sourceTable, traceFile);
        List<String> params = new ArrayList<>();
        sourceTable.getColumns().forEach(c -> {
            if (!c.getName().equals(Columns.SOURCE_ID)) params.add(c.getName());
        });
        var pdm = manager.findPDM(params);
        if (pdm == null) {
            var parameterNames = sourceTable.getColumns().stream().map(CyColumn::getName).collect(Collectors.toList());
            pdm = new ParameterDiscretizationModel(parameterNames);
            var subNetwork = createAndInitSubnetwork(pdm, "New PDM");
            addTraceGraphToPDM(pdm, sourceTable, subNetwork);
        } else {
            addTraceGraphToPDM(pdm, sourceTable);
        }
    }

    private CySubNetwork createAndInitSubnetwork(ParameterDiscretizationModel pdm, String preferredName) {
        var subNetwork = (CySubNetwork) networkFactory.createNetwork();
        var rootNetwork = subNetwork.getRootNetwork();
        var rootNetworkName = manager.getAvailableRootNetworkName(preferredName);
        rootNetwork.getDefaultNetworkTable().getRow(rootNetwork.getSUID()).set(CyNetwork.NAME, rootNetworkName);
        pdm.setRootNetwork(rootNetwork);
        this.initSubNetwork(subNetwork, rootNetwork, pdm);
        return subNetwork;
    }

    public void addTraceGraphToPDM(ParameterDiscretizationModel pdm, CyTable sourceTable) {
        var subNetwork = Util.createSubNetwork(pdm);
        this.addTraceGraphToPDM(pdm, sourceTable, subNetwork);
    }

    public void addTraceGraphToPDM(ParameterDiscretizationModel pdm, CyTable sourceTable, CyNetwork subNetwork) {
        this.tableManager.addTable(sourceTable);
        this.networkTableManager.setTable(subNetwork, CyNode.class, sourceTable.getTitle(), sourceTable);
        var traceGraph = new TraceGraph(subNetwork, pdm);
        traceGraph.addSourceTable(sourceTable);
        TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
        manager.registerTraceGraph(pdm, controller);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        ParameterDiscretizationModel pdm;
        try {
            ParameterDiscretizationModelDTO dto = parsePDM();
            pdm = new ParameterDiscretizationModel(dto);
            loadPDM(pdm, dto, dto.getName());
        } catch (JsonSyntaxException e) {
            loadTrace();
        }
    }

    private ParameterDiscretizationModelDTO parsePDM() throws Exception {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String pdmString = Files.readString(traceFile.toPath());
        ParameterDiscretizationModelDTO dto = gson.fromJson(pdmString, ParameterDiscretizationModelDTO.class);
        return dto;
    }

}
