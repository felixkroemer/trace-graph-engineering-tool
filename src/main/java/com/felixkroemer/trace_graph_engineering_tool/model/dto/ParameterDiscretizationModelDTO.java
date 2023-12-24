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
        this.parameters = pdm.getParameters().stream().map(ParameterDTO::new).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public List<String> getCsvs() {
        return csvs;
    }

    public String getDescription() {
        return description;
    }

    public List<ParameterDTO> getParameters() {
        return parameters;
    }

    public int getParameterCount() {
        return this.parameters.size();
    }
}

