package com.felixkroemer.trace_graph_engineering_tool.model;

public class Profiler {

    private static Profiler instance;
    private long addSourceTableResult;
    private long updateTraceGraphResult;
    private long impactedSituations;
    private long foundNodes;
    private long notFoundInSubnetworkNodes;
    private long notFoundInRootNetworkNodes;
    private long leftOverNodes;

    public long getFoundNodes() {
        return foundNodes;
    }

    public long getNotFoundInSubnetworkNodes() {
        return notFoundInSubnetworkNodes;
    }

    public long getNotFoundInRootNetworkNodes() {
        return notFoundInRootNetworkNodes;
    }

    public void reset() {
        this.addSourceTableResult = 0;
        this.updateTraceGraphResult = 0;
        this.impactedSituations = 0;
        this.foundNodes = 0;
        this.notFoundInSubnetworkNodes = 0;
        this.notFoundInRootNetworkNodes = 0;
        this.leftOverNodes = 0;
    }

    private Profiler() {
    }

    public long getAddSourceTableResult() {
        return this.addSourceTableResult;
    }

    public void setAddSourceTableResult(long result) {
        this.addSourceTableResult = result;
    }

    public long getUpdateTraceGraphResult() {
        return this.updateTraceGraphResult;
    }

    public void setUpdateTraceGraphResult(long result) {
        this.updateTraceGraphResult = result;
    }

    public void addImpactedSituation() {
        this.impactedSituations += 1;
    }

    public long getImpactedSituations() {
        return this.impactedSituations;
    }

    public void resetImpactedSituations() {
        this.impactedSituations = 0;
    }

    public void addNodeFound() {
        this.foundNodes += 1;
    }

    public void addNodeNotFoundInSubnetwork() {
        this.notFoundInSubnetworkNodes += 1;
    }

    public void addNodeNotFoundInRootNetwork() {
        this.notFoundInRootNetworkNodes += 1;
    }

    public void resetFoundNodes() {
        this.foundNodes = 0;
        this.notFoundInSubnetworkNodes = 0;
        this.notFoundInRootNetworkNodes = 0;
    }

    public static Profiler getInstance() {
        if (instance == null) {
            instance = new Profiler();
        }
        return instance;
    }

    public long getLeftOverNodes() {
        return leftOverNodes;
    }

    public void setLeftOverNodes(long leftOverNodes) {
        this.leftOverNodes = leftOverNodes;
    }
}
