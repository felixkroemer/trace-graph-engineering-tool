package com.felixkroemer.trace_graph_engineering_tool.model.dto;

import java.util.List;

public class ParameterDiscretizationModelDTO {
    private String name;
    private String version;
    private List<String> csvs;
    private String description;
    private List<ParameterDTO> parameters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

