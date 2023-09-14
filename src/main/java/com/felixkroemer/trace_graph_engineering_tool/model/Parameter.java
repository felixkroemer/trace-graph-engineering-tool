package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;

import java.util.List;

public class Parameter {
    private String name;
    private String type;
    private List<Double> bins;

    public Parameter(ParameterDTO dto) {
        this.name = dto.getName();
        this.type = dto.getType();
        this.bins = dto.getBins();
    }

    public String getName() {
        return name;
    }

    public List<Double> getBins() {
        return bins;
    }
}
