package com.felixkroemer.trace_graph_engineering_tool.model;

public class Columns {
    // number of hypothetical self edges of a node
    public static final String NODE_VISITS = "visits";
    // number of hypothetical ingoing edges, equals number of outgoing edges;
    // except for first and last source row entry of a trace
    public static final String NODE_FREQUENCY = "frequency";

    public static final String EDGE_TRAVERSALS = "traversals";
    //TODO: remove if unnecessary
    public static final String EDGE_SOURCE_ROWS = "edge_source_rows";

    public static final String NETWORK_TG_MARKER = "traceGraphMarker";

    public static final String SOURCE_ID = "id";
}
