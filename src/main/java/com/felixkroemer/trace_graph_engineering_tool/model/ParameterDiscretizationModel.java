package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class ParameterDiscretizationModel extends AbstractListModel<Parameter> {
    private String name;
    private String version;
    private String csv;
    private String description;
    private List<Parameter> parameters;

    public ParameterDiscretizationModel(ParameterDiscretizationModelDTO dto) {
        this.name = dto.getName();
        this.version = dto.getVersion();
        this.csv = dto.getCsv();
        this.description = dto.getDescription();
        this.parameters = new ArrayList<>(dto.getParameterCount());
        for (ParameterDTO param : dto.getParameters()) {
            this.parameters.add(new Parameter(param));
        }
    }

    public List<Parameter> getParameters() {
        return this.parameters;
    }

    public String getCSV() {
        return this.csv;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public int getSize() {
        return parameters.size();
    }

    @Override
    public Parameter getElementAt(int index) {
        return this.parameters.get(index);
    }
}

