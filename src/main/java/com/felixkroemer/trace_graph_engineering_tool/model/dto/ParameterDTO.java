package com.felixkroemer.trace_graph_engineering_tool.model.dto;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;

import java.util.List;

public class ParameterDTO {

    private String name;
    private List<Double> bins;

    public ParameterDTO(Parameter parameter) {
        this.name = parameter.getName();
        this.bins = parameter.getBins();
    }

    public String getName() {
        return name;
    }

    public List<Double> getBins() {
        return bins;
    }
}
