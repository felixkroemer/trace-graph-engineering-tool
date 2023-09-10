package com.felixkroemer.trace_graph_engineering_tool.mappings;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;

public class TooltipMappingFactory implements VisualMappingFunctionFactory {

    private final CyServiceRegistrar reg;

    public TooltipMappingFactory(final CyServiceRegistrar serviceRegistrar) {
        this.reg = serviceRegistrar;
    }

    @Override
    public <K, V> VisualMappingFunction<K, V> createVisualMappingFunction(String attributeName,
                                                                          Class<K> attrValueType,
                                                                          VisualProperty<V> vp) {
        TraceGraphController controller = reg.getService(TraceGraphController.class);
        // tr is never null if TooltipMapping is only used for TraceGraphs
        TraceGraph tr = controller.getActiveTraceGraph();
        return (VisualMappingFunction<K, V>) new TooltipMapping(tr.getPDM());
    }

    @Override
    public Class<?> getMappingFunctionType() {
        return TooltipMapping.class;
    }
}