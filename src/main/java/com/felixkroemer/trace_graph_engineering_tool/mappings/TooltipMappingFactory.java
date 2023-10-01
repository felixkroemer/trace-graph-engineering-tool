package com.felixkroemer.trace_graph_engineering_tool.mappings;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;

public class TooltipMappingFactory implements VisualMappingFunctionFactory {

    private final CyServiceRegistrar reg;
    private TraceGraph traceGraph;

    public TooltipMappingFactory(final CyServiceRegistrar serviceRegistrar) {
        this.reg = serviceRegistrar;
    }

    @Override
    public <K, V> VisualMappingFunction<K, V> createVisualMappingFunction(String attributeName,
                                                                          Class<K> attrValueType,
                                                                          VisualProperty<V> vp) {
        return (VisualMappingFunction<K, V>) new TooltipMapping(traceGraph.getPDM());
    }

    public void setTraceGraph(TraceGraph traceGraph) {
        this.traceGraph = traceGraph;
    }

    @Override
    public Class<?> getMappingFunctionType() {
        return TooltipMapping.class;
    }
}