package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import org.cytoscape.application.CyUserLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ParameterDiscretizationModel {

    private final Logger logger;

    private String name;
    private String version;
    private String csv;
    private String description;
    private List<Parameter> parameters;

    public ParameterDiscretizationModel(ParameterDiscretizationModelDTO dto) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
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

    public int getParameterCount() {
        return this.parameters.size();
    }

    public void forEach(Consumer<Parameter> consumer) {
        for (Parameter param : this.parameters) {
            consumer.accept(param);
        }
    }

    public String getCSV() {
        return this.csv;
    }

    public String getName() {
        return this.name;
    }

    public Parameter getParameter(String name) {
        for (Parameter p : this.parameters) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    public void setParameterEnabled(String name, boolean enabled) throws IllegalArgumentException {
        Parameter p = this.getParameter(name);
        if (p != null) {
            if (p.isEnabled()) {
                if (!enabled) {
                    p.enable();
                } else {
                    logger.warn("Parameter {} is already enabled", name);
                }
            } else {
                if (enabled) {
                    p.disable();
                } else {
                    logger.warn("Parameter {} is already disabled", name);
                }
            }
        } else {
            throw new IllegalArgumentException(String.format("Parameter %s is unknown.", name));
        }
    }

    public void setBins(String name, List<Double> bins) {
        Parameter p = this.getParameter(name);
        if (p != null) {
            p.setBins(bins);

        } else {
            throw new IllegalArgumentException(String.format("Parameter %s is unknown.", name));
        }
    }
}

