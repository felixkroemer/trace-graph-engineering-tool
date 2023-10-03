package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import com.felixkroemer.trace_graph_engineering_tool.model.Trace;

import java.util.Set;

public interface TracesProvider {

    public long getNetworkSUID();

    public Set<Trace> getTraces();
}
