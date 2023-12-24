package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import java.io.File;
import java.io.FileWriter;

public class ExportPDMTask extends AbstractTask {

    @Tunable(description = "Select destination", params = "input=false", required = true)
    public File pdmFile;
    private ParameterDiscretizationModel pdm;

    public ExportPDMTask(ParameterDiscretizationModel pdm) {
        this.pdm = pdm;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(pdmFile)) {
            gson.toJson(new ParameterDiscretizationModelDTO(this.pdm), writer);
            writer.flush();
        }
    }
}
