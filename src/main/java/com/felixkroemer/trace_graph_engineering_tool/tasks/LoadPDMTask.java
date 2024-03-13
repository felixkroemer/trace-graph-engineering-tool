package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.source_table.Trace;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.SelectMatchingPDMPanel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LoadPDMTask extends AbstractTask {

    private final Logger logger;
    private final TraceGraphManager manager;
    private final CyNetworkFactory networkFactory;
    private final CyServiceRegistrar registrar;
    @Tunable(description = "The trace to load", params = "input=true", required = true)
    public File traceFile;

    public LoadPDMTask(CyServiceRegistrar reg) {
        this.logger = LoggerFactory.getLogger(CyNetwork.NAME);
        this.manager = reg.getService(TraceGraphManager.class);
        this.networkFactory = reg.getService(CyNetworkFactory.class);
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
        var pdm = new ParameterDiscretizationModel(this.registrar, dto);
        var subNetwork = createRootNetworkForPDM(pdm, dto.getName());
        return new TraceGraph(registrar, subNetwork, pdm);
    }

    public TraceGraph createTraceGraphAndPDM(List<String> parameters) {
        var pdm = new ParameterDiscretizationModel(this.registrar, parameters);
        var subNetwork = createRootNetworkForPDM(pdm, "PDM");
        return new TraceGraph(registrar, subNetwork, pdm);
    }

    private void updatePDM(ParameterDiscretizationModel pdm, ParameterDiscretizationModelDTO dto) {
        pdm.setParameterBins(dto.getParameters());
        if (dto.getCsvs() != null) {
            var subNetwork = Util.createSubNetwork(pdm);
            TraceGraph traceGraph = new TraceGraph(registrar, subNetwork, pdm);
            loadTracesToTraceGraph(dto, traceGraph);
            TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
            manager.registerTraceGraph(traceGraph.getPDM(), controller);
        }
    }

    public void importPDM(ParameterDiscretizationModelDTO dto) {
        TraceGraph traceGraph = createTraceGraphAndPDM(dto);
        loadTracesToTraceGraph(dto, traceGraph);
        TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
        manager.registerTraceGraph(traceGraph.getPDM(), controller);
    }

    public void loadPDM(ParameterDiscretizationModelDTO dto) throws Exception {
        var params = dto.getParameters().stream().map(ParameterDTO::getName).collect(Collectors.toSet());
        var matchingPDMs = manager.findPDM(params);
        if (!matchingPDMs.isEmpty()) {
            SwingUtilities.invokeLater(() -> Util.showDialog(new SelectMatchingPDMPanel(matchingPDMs, () -> this.importPDM(dto), (pdm) -> this.updatePDM(pdm, dto), dto.getCsvs() != null), "Select matching PDM"));
        } else {
            if (dto.getCsvs() != null) {
                this.importPDM(dto);
            } else {
                throw new Exception("PDM must have a list of Traces to load initially.");
            }
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
                var trace = new Trace(csv, Files.lines(path.toPath()).count() - 1, registrar);
                //var tableFactory = registrar.getService(CyTableFactory.class);
                //var trace = tableFactory.createTable(csv, "id", Long.class, true, true);
                trace.setTitle(csv);
                Util.parseCSV(trace, path);
                loadTraceToTraceGraph(trace, traceGraph);
            } catch (Exception e) {
                logger.error("Could not load Trace with path " + path);
            }
        }
    }

    public void loadTrace() throws Exception {
        CyTable trace = new Trace(traceFile.getName(), Files.lines(traceFile.toPath()).count() - 1, registrar);
        // code for comparison to default CyTableImpl
        //var tableFactory = registrar.getService(CyTableFactory.class);
        //var trace = tableFactory.createTable(traceFile.getName(), "id", Long.class, true, true);
        Util.parseCSV(trace, traceFile);
        List<String> params = new ArrayList<>();
        trace.getColumns().forEach(c -> {
            if (!c.getName().equals(Columns.SOURCE_ID))
                params.add(c.getName());
        });
        var pdms = manager.findPDM(params);
        if (pdms.isEmpty()) {
            this.loadTraceToNewPDM(trace);
        } else {
            SwingUtilities.invokeLater(() -> Util.showDialog(new SelectMatchingPDMPanel(pdms, () -> this.loadTraceToNewPDM(trace), (pdm) -> {
                var subNetwork = Util.createSubNetwork(pdm);
                TraceGraph traceGraph = new TraceGraph(this.registrar, subNetwork, pdm);
                this.loadTraceToTraceGraph(trace, traceGraph);
                TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
                manager.registerTraceGraph(traceGraph.getPDM(), controller);
            }, true), "Select matching PDM"));
        }
    }

    private void loadTraceToNewPDM(CyTable trace) {
        var parameterNames = trace.getColumns().stream().map(CyColumn::getName).collect(Collectors.toList());
        var traceGraph = this.createTraceGraphAndPDM(parameterNames);
        loadTraceToTraceGraph(trace, traceGraph);
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

    public void loadTraceToTraceGraph(CyTable trace, TraceGraph traceGraph) {
        traceGraph.addTrace(trace);
    }

    private ParameterDiscretizationModelDTO parsePDM() throws Exception {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String pdmString = Files.readString(traceFile.toPath());
        return gson.fromJson(pdmString, ParameterDiscretizationModelDTO.class);
    }
}
