package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.model.source_table.TraceGraphSourceTable;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class LoadTraceTask extends AbstractTask {

    @Tunable(description = "The trace to load", params = "input=true", required = true)
    public File traceFile;

    private final TraceGraphManager manager;
    private final CyNetworkTableManager networkTableManager;
    private final CyTableManager tableManager;
    private final CyServiceRegistrar registrar;

    public LoadTraceTask(CyServiceRegistrar reg) {
        this.manager = reg.getService(TraceGraphManager.class);
        this.networkTableManager = reg.getService(CyNetworkTableManager.class);
        this.tableManager = reg.getService(CyTableManager.class);
        this.registrar = reg;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        // CyTable sourceTable = tableFactory.createTable(traceFile.getName(), Columns.SOURCE_ID, Long.class, true,
        // true);
        CyTable sourceTable = new TraceGraphSourceTable(traceFile.getName(), Files.lines(traceFile.toPath()).count(),
                registrar);
        Util.parseCSV(sourceTable, traceFile);
        List<String> params = new ArrayList<>();
        sourceTable.getColumns().forEach(c -> {
            if (!c.getName().equals(Columns.SOURCE_ID)) params.add(c.getName());
        });
        ParameterDiscretizationModel pdm = manager.findPDM(params);
        if (pdm != null) {
            this.tableManager.addTable(sourceTable);
            var subNetwork = Util.createSubNetwork(pdm);
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
