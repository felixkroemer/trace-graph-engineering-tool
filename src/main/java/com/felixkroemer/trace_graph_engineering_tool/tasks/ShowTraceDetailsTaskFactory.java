package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.display_manager.Trace;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

import java.util.Set;

public class ShowTraceDetailsTaskFactory extends AbstractNetworkViewTaskFactory {

    private CyServiceRegistrar reg;
    private Set<Trace> traces;

    public ShowTraceDetailsTaskFactory(CyServiceRegistrar reg) {
        this.reg = reg;
    }

    public void setTraces(Set<Trace> traces) {
        this.traces = traces;
    }

    @Override
    public boolean isReady(CyNetworkView networkView) {
        return this.traces != null;
    }

    @Override
    public TaskIterator createTaskIterator(CyNetworkView networkView) {
        return new TaskIterator(new ShowTraceDetailsTask(reg, this.traces));
    }
}
