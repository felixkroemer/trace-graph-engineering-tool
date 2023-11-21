package com.felixkroemer.trace_graph_engineering_tool.model.dto;

import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;

import java.util.List;
import java.util.stream.Collectors;

public class ParameterDiscretizationModelDTO {
    private String name;
    private List<String> csvs;
    private String description;
    private List<ParameterDTO> parameters;

    public ParameterDiscretizationModelDTO(ParameterDiscretizationModel pdm) {
        this.name = pdm.getName();
        this.csvs = pdm.getCSVs();
        this.parameters = pdm.getParameters().stream().map(ParameterDTO::new).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getCsvs() {
        return csvs;
    }

    public void setCsvs(String csv) {
        this.csvs = csvs;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ParameterDTO> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterDTO> parameters) {
        this.parameters = parameters;
    }

    public int getParameterCount() {
        return this.parameters.size();
    }

}

