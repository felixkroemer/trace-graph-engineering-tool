package com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels;

import com.felixkroemer.trace_graph_engineering_tool.view.TraceGraphPanel;
import org.cytoscape.service.util.CyServiceRegistrar;

public class TracesPanel extends TraceGraphPanel {
    private CyServiceRegistrar registrar;

    public TracesPanel(CyServiceRegistrar registrar) {
        this.registrar = registrar;
    }

    @Override
    public String getTitle() {
        return "Traces";
    }
}
