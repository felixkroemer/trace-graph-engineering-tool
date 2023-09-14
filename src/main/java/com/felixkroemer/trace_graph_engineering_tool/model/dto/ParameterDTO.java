package com.felixkroemer.trace_graph_engineering_tool.model.dto;

import java.util.List;

public class ParameterDTO {
    private String name;
    private String type;
    private List<Double> bins;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Double> getBins() {
        return bins;
    }

    public void setBins(List<Double> bins) {
        this.bins = bins;
    }
}
