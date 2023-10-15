package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

public class ParameterDiscretizationModel {

    private final Logger logger;

    private String name;
    private String version;
    private String description;
    private List<Parameter> parameters;
    private Map<Long, Long> suidHashMapping;
    private Set<CyTable> sourceTables;
    private CyRootNetwork rootNetwork;

    public ParameterDiscretizationModel(ParameterDiscretizationModelDTO dto) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.name = dto.getName();
        this.version = dto.getVersion();
        this.description = dto.getDescription();
        this.parameters = new ArrayList<>(dto.getParameterCount());
        for (ParameterDTO param : dto.getParameters()) {
            this.parameters.add(new Parameter(param, this));
        }
        this.suidHashMapping = new HashMap<>();
        this.sourceTables = new HashSet<>();
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

    public void addSourceTable(CyTable sourceTable) {
        this.sourceTables.add(sourceTable);
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

    public Map<Long, Long> getSuidHashMapping() {
        return this.suidHashMapping;
    }


    public CyRootNetwork getRootNetwork() {
        return this.rootNetwork;
    }

    // can't be set in constructor because rootnetwork can't be created via api without subnetwork
    public void setRootNetwork(CyRootNetwork rootNetwork) {
        this.rootNetwork = rootNetwork;
    }

    public Set<CyTable> getSourceTables() {
        return this.sourceTables;
    }
}

