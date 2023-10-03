package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.display_manager.TracesProvider;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;

import java.util.HashMap;
import java.util.Map;

import static com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController.NETWORK_TYPE_DEFAULT;

public class ShowTraceDetailsNodeTaskFactory extends AbstractNodeViewTaskFactory {

    private CyServiceRegistrar reg;
    private Map<Long, TracesProvider> tracesProviders;

    public ShowTraceDetailsNodeTaskFactory(CyServiceRegistrar reg) {
        this.tracesProviders = new HashMap<>();
        this.reg = reg;
    }

    @Override
    public boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
        var manager = this.reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(networkView.getModel());
        if (controller != null) {
            return controller.getNetworkType(networkView.getModel()).equals(NETWORK_TYPE_DEFAULT);
        } else {
            return false;
        }
    }

    public void handleTracesProviderRegistration(TracesProvider provider, Map<Object, Object> serviceProps) {
        this.tracesProviders.put(provider.getNetworkSUID(), provider);
    }

    public void handleTracesProviderDeregistration(TracesProvider provider, Map<Object, Object> serviceProps) {
        this.tracesProviders.remove(provider.getNetworkSUID());
    }

    @Override
    public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView networkView) {
        return new TaskIterator(new ShowTraceDetailsTask(reg, nodeView, networkView,
                this.tracesProviders.get(networkView.getModel().getSUID())));
    }
}
