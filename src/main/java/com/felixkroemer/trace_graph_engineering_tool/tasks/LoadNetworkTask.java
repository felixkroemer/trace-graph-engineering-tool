package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
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
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;

public class LoadNetworkTask extends AbstractTask {

    @Tunable(description = "The pdm to load", params = "input=true", required = true)
    public File pdmFile;

    private final Logger logger;

    private final TraceGraphController controller;
    private final CyTableFactory tableFactory;
    private final CyNetworkFactory networkFactory;

    public LoadNetworkTask(CyServiceRegistrar reg) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.controller = reg.getService(TraceGraphController.class);
        this.tableFactory = reg.getService(CyTableFactory.class);
        this.networkFactory = reg.getService(CyNetworkFactory.class);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        ParameterDiscretizationModelDTO dto = parsePDM();
        ParameterDiscretizationModel pdm = new ParameterDiscretizationModel(dto);
        CyTable table = parseCSV(dto);
        CyNetwork network = networkFactory.createNetwork();
        TraceGraph traceGraph = new TraceGraph(network, pdm, table);
        controller.registerTraceGraph(traceGraph);
    }

    private ParameterDiscretizationModelDTO parsePDM() throws Exception {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String pdmString = Files.readString(pdmFile.toPath());
        ParameterDiscretizationModelDTO dto = gson.fromJson(pdmString, ParameterDiscretizationModelDTO.class);
        return dto;
    }

    private CyTable parseCSV(ParameterDiscretizationModelDTO dto) throws Exception {
        CyTable table = tableFactory.createTable("data", Columns.SOURCE_ID, Integer.class, true, true);
        for (ParameterDTO param : dto.getParameters()) {
            table.createColumn(param.getName(), Double.class, false);
        }
        boolean header = true;
        try (CSVReader reader = new CSVReader(new FileReader(new File(pdmFile.getParentFile(), dto.getCsv())))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                CyRow row = table.getRow(Integer.parseInt(line[0]));
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
