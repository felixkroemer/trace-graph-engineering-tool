package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.opencsv.CSVReader;
import org.cytoscape.application.CyUserLog;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoadNetworkTask extends AbstractTask {

    @Tunable(description = "The input data to load", params = "input=true", required = true)
    public File traceGraph;

    @Tunable(description = "The pdm to load", params = "input=true", required = true)
    public File pdm;

    private final Logger logger;
    private final TraceGraphController controller;
    private final CyTableFactory tableFactory;

    public LoadNetworkTask(CyServiceRegistrar reg) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.controller = reg.getService(TraceGraphController.class);
        this.tableFactory = reg.getService(CyTableFactory.class);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        CyTable table = parseCSV();
    }

    private CyTable parseCSV() throws Exception {
        CyTable table = tableFactory.createTable("data", "suid", Long.class, true, true);
        List<String> propertyNames = new ArrayList<>();
        boolean header = true;
        try (CSVReader reader = new CSVReader(new FileReader(traceGraph))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (header) {
                    Arrays.stream(line).skip(1).forEach(s -> {
                        table.createColumn(s, Double.class, false);
                    });
                    propertyNames.addAll(Arrays.stream(line).skip(1).toList());
                    header = false;
                    continue;
                }
                CyRow row;
                while ((row = table.getRow(Util.genSUID())) == null) {
                    continue;
                }
                for (int i = 1; i < line.length; i++) {
                    if (!line[i].isEmpty()) {
                        row.set(propertyNames.get(i - 1), Double.parseDouble(line[i]));
                    } else {
                        row.set(propertyNames.get(i - 1), 0.0);
                    }
                }
            }
            //table.getRow(10000L).getAllValues().forEach((k, v) -> logger.info(k + ": " + v));
        }
        return table;
    }
}
