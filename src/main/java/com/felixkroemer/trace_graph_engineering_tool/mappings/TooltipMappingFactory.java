package com.felixkroemer.trace_graph_engineering_tool.mappings;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;

public class TooltipMappingFactory implements VisualMappingFunctionFactory {

    private final CyServiceRegistrar reg;
    private TraceGraphController controller;

    public TooltipMappingFactory(final CyServiceRegistrar serviceRegistrar) {
        this.reg = serviceRegistrar;
    }

    @Override
    public <K, V> VisualMappingFunction<K, V> createVisualMappingFunction(String attributeName,
                                                                          Class<K> attrValueType,
                                                                          VisualProperty<V> vp) {
        // tr is never null if TooltipMapping is only used for TraceGraphs
        TraceGraph tr = this.controller.getTraceGraph();
        return (VisualMappingFunction<K, V>) new TooltipMapping(tr.getPDM());
    }

    public void setTraceGraphController(TraceGraphController controller) {
        this.controller = controller;
    }

    @Override
    public Class<?> getMappingFunctionType() {
        return TooltipMapping.class;
    }
}