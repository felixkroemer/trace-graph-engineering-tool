package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.source_table.TraceGraphSourceTable;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.SelectMatchingPDMDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


import javax.swing.*;
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
        this.logger = LoggerFactory.getLogger(CyNetwork.NAME);
        this.manager = reg.getService(TraceGraphManager.class);
        this.networkFactory = reg.getService(CyNetworkFactory.class);
        this.networkTableManager = reg.getService(CyNetworkTableManager.class);
        this.tableManager = reg.getService(CyTableManager.class);
        this.registrar = reg;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        try {
            ParameterDiscretizationModelDTO dto = parsePDM();
            loadPDM(dto);
        } catch (JsonSyntaxException e) {
            loadTrace();
        }
    }

    public TraceGraph createTraceGraphAndPDM(ParameterDiscretizationModelDTO dto) {
        var pdm = new ParameterDiscretizationModel(dto);
        var subNetwork = createRootNetworkForPDM(pdm, dto.getName());
        return new TraceGraph(subNetwork, pdm);
    }

    public TraceGraph createTraceGraphAndPDM(List<String> parameters) {
        var pdm = new ParameterDiscretizationModel(parameters);
        var subNetwork = createRootNetworkForPDM(pdm, "PDM");
        return new TraceGraph(subNetwork, pdm);
    }

    private void updatePDM(ParameterDiscretizationModel pdm, ParameterDiscretizationModelDTO dto) {
        pdm.setParameterBins(dto.getParameters());
        if(dto.getCsvs() != null) {
            var subNetwork = Util.createSubNetwork(pdm);
            TraceGraph traceGraph = new TraceGraph(subNetwork, pdm);
            loadTracesToTraceGraph(dto, traceGraph);
            TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
            manager.registerTraceGraph(traceGraph.getPDM(), controller);
        }
    }

    public void loadPDM(ParameterDiscretizationModelDTO dto) {
        var params = dto.getParameters().stream().map(ParameterDTO::getName).collect(Collectors.toSet());
        var matchingPDMs = manager.findPDM(params);
        if (!matchingPDMs.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                new SelectMatchingPDMDialog(matchingPDMs, () -> {
                    TraceGraph traceGraph = createTraceGraphAndPDM(dto);
                    if(dto.getCsvs() != null) {
                        loadTracesToTraceGraph(dto, traceGraph);
                    } else {
                        traceGraph.setPlaceholder();
                    }
                    TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
                    manager.registerTraceGraph(traceGraph.getPDM(), controller);
                }, (pdm) -> {
                    try {
                        updatePDM(pdm, dto);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).showDialog();
            });
        } else {
            TraceGraph traceGraph = this.createTraceGraphAndPDM(dto);
            if(dto.getCsvs() != null) {
                loadTracesToTraceGraph(dto, traceGraph);
            } else {
                traceGraph.setPlaceholder();
            }
            TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
            manager.registerTraceGraph(traceGraph.getPDM(), controller);
        }
    }

    private void loadTracesToTraceGraph(ParameterDiscretizationModelDTO dto, TraceGraph traceGraph) {
        if (dto.getCsvs() == null) {
            return;
        }
        for (String csv : dto.getCsvs()) {
            File path = null;
            try {
                path = new File(traceFile.getParentFile(), csv);
                var sourceTable = new TraceGraphSourceTable(csv, Files.lines(path.toPath()).count() - 1, registrar);
                sourceTable.setTitle(csv);
                Util.parseCSV(sourceTable, path);
                loadTraceToTraceGraph(sourceTable, traceGraph);
            } catch (Exception e) {
                logger.error("Could not load Trace with path " + path);
            }
        }
    }

    public void loadTrace() throws Exception {
        CyTable sourceTable = new TraceGraphSourceTable(traceFile.getName(), Files.lines(traceFile.toPath()).count() - 1, registrar);
        Util.parseCSV(sourceTable, traceFile);
        List<String> params = new ArrayList<>();
        sourceTable.getColumns().forEach(c -> {
            if (!c.getName().equals(Columns.SOURCE_ID)) params.add(c.getName());
        });
        var pdms = manager.findPDM(params);
        if (pdms.isEmpty()) {
            this.loadTraceToNewPDM(sourceTable);
        } else {
            SwingUtilities.invokeLater(() -> {
                new SelectMatchingPDMDialog(pdms, () -> {
                    this.loadTraceToNewPDM(sourceTable);
                }, (pdm) -> {
                    var subNetwork = Util.createSubNetwork(pdm);
                    TraceGraph traceGraph = new TraceGraph(subNetwork, pdm);
                    this.loadTraceToTraceGraph(sourceTable, traceGraph);
                    TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
                    manager.registerTraceGraph(traceGraph.getPDM(), controller);
                }).showDialog();
            });
        }
    }

    private void loadTraceToNewPDM(CyTable sourceTable) {
        var parameterNames = sourceTable.getColumns().stream().map(CyColumn::getName).collect(Collectors.toList());
        var traceGraph = this.createTraceGraphAndPDM(parameterNames);
        loadTraceToTraceGraph(sourceTable, traceGraph);
        TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
        manager.registerTraceGraph(traceGraph.getPDM(), controller);
    }


    /*
    Create a and set a root network for a pdm with the preferred name, create all parameter columns in the shared
    node table and set the TG network marker in the network table.
     */
    private CySubNetwork createRootNetworkForPDM(ParameterDiscretizationModel pdm, String preferredName) {
        var subNetwork = (CySubNetwork) networkFactory.createNetwork();
        var rootNetwork = subNetwork.getRootNetwork();
        var rootNetworkName = manager.getAvailableRootNetworkName(preferredName);
        rootNetwork.getDefaultNetworkTable().getRow(rootNetwork.getSUID()).set(CyNetwork.NAME, rootNetworkName);
        pdm.setRootNetwork(rootNetwork);
        var sharedNodeTable = rootNetwork.getSharedNodeTable();
        pdm.forEach(p -> sharedNodeTable.createColumn(p.getName(), Integer.class, false));
        var localNetworkTable = subNetwork.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS);
        localNetworkTable.createColumn(Columns.NETWORK_TG_MARKER, Integer.class, true);
        return subNetwork;
    }

    public void loadTraceToTraceGraph(CyTable sourceTable, TraceGraph traceGraph) {
        this.tableManager.addTable(sourceTable);
        this.networkTableManager.setTable(traceGraph.getNetwork(), CyNode.class, sourceTable.getTitle(), sourceTable);
        traceGraph.addSourceTable(sourceTable);
    }

    private ParameterDiscretizationModelDTO parsePDM() throws Exception {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String pdmString = Files.readString(traceFile.toPath());
        ParameterDiscretizationModelDTO dto = gson.fromJson(pdmString, ParameterDiscretizationModelDTO.class);
        return dto;
    }

}
