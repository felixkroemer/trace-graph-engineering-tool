package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVReader;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
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
        ParameterDiscretizationModel pdm = parsePDM();
        CyTable table = parseCSV(pdm);
        TraceGraph traceGraph = new TraceGraph(this.networkFactory, pdm, table);
        controller.registerTraceGraph(traceGraph);
    }

    private ParameterDiscretizationModel parsePDM() throws Exception {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String pdmString = Files.readString(pdmFile.toPath());
        return gson.fromJson(pdmString, ParameterDiscretizationModel.class);
    }

    private CyTable parseCSV(ParameterDiscretizationModel pdm) throws Exception {
        CyTable table = tableFactory.createTable("data", "id", Long.class, true, true);
        for (Parameter param : pdm.getParameters()) {
            table.createColumn(param.getName(), Double.class, false);
        }
        boolean header = true;
        try (CSVReader reader = new CSVReader(new FileReader(new File(pdmFile.getParentFile(), pdm.getCsv())))) {
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
                        row.set(pdm.getParameters().get(i - 1).getName(), Double.parseDouble(line[i]));
                    } else {
                        row.set(pdm.getParameters().get(i - 1).getName(), 0.0);
                    }
                }
            }
        }
        return table;
    }
}
