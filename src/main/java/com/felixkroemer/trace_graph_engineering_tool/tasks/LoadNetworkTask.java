package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVReader;
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
import java.io.FileReader;
import java.nio.file.Files;
import java.util.List;

public class LoadNetworkTask extends AbstractTask {

    @Tunable(description = "The pdm to load", params = "input=true", required = true)
    public File pdmFile;

    private final Logger logger;

    private final TraceGraphManager manager;
    private final CyTableFactory tableFactory;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkTableManager networkTableManager;
    private final CyTableManager tableManager;

    public LoadNetworkTask(CyServiceRegistrar reg) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.manager = reg.getService(TraceGraphManager.class);
        this.tableFactory = reg.getService(CyTableFactory.class);
        this.networkFactory = reg.getService(CyNetworkFactory.class);
        this.networkTableManager = reg.getService(CyNetworkTableManager.class);
        this.tableManager = reg.getService(CyTableManager.class);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        ParameterDiscretizationModelDTO dto = parsePDM();
        List<String> csvs = dto.getCsvs();

        ParameterDiscretizationModel pdm = new ParameterDiscretizationModel(dto);
        for (String csv : csvs) {
            CyTable table = parseCSV(dto, csv);
            CyRootNetwork rootNetwork = pdm.getRootNetwork();
            CySubNetwork subNetwork = null;
            if (rootNetwork == null) {
                subNetwork = (CySubNetwork) networkFactory.createNetwork();
                rootNetwork = subNetwork.getRootNetwork();
                pdm.setRootNetwork(rootNetwork);
                var sharedNodeTable = rootNetwork.getSharedNodeTable();
                pdm.forEach(p -> sharedNodeTable.createColumn(p.getName(), Integer.class, false));
                var sharedNetworkTable = rootNetwork.getDefaultNetworkTable();
                sharedNetworkTable.createColumn(Columns.NETWORK_TG_MARKER, Integer.class, true);
            } else {
                subNetwork = rootNetwork.addSubNetwork();
            }

            this.tableManager.addTable(table);
            this.networkTableManager.setTable(subNetwork, CyNode.class, "com.felixkroemer", table);

            TraceGraph traceGraph = new TraceGraph(subNetwork, pdm, table);
            manager.registerTraceGraph(pdm, traceGraph);
        }
    }

    private ParameterDiscretizationModelDTO parsePDM() throws Exception {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String pdmString = Files.readString(pdmFile.toPath());
        ParameterDiscretizationModelDTO dto = gson.fromJson(pdmString, ParameterDiscretizationModelDTO.class);
        return dto;
    }

    private CyTable parseCSV(ParameterDiscretizationModelDTO dto, String csv) throws Exception {
        CyTable table = tableFactory.createTable("Source", Columns.SOURCE_ID, Long.class, true, true);
        for (ParameterDTO param : dto.getParameters()) {
            table.createColumn(param.getName(), Double.class, false);
        }
        boolean header = true;
        try (CSVReader reader = new CSVReader(new FileReader(new File(pdmFile.getParentFile(), csv)))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                CyRow row = table.getRow(Long.parseLong(line[0]));
                for (int i = 1; i < line.length; i++) {
                    // csv has some empty entries
                    if (!line[i].isEmpty()) {
                        row.set(dto.getParameters().get(i - 1).getName(), Double.parseDouble(line[i]));
                    } else {
                        row.set(dto.getParameters().get(i - 1).getName(), 0.0);
                    }
                }
            }
        }
        return table;
    }
}
