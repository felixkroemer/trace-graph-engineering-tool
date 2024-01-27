package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.Profiler;
import org.apache.commons.lang3.time.StopWatch;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.*;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProfilingTask extends AbstractTask implements NetworkAddedListener {

    private CyServiceRegistrar registrar;
    private CyNetwork createdNetwork;
    private Map<String, List<Long>> results;

    public ProfilingTask(CyServiceRegistrar reg) {
        this.registrar = reg;
        this.results = new LinkedHashMap<>();
        registrar.registerService(this, NetworkAddedListener.class);
    }

    @Override
    public void run(TaskMonitor monitor) throws Exception {
        this.profileTraceLoading();
        this.registrar.unregisterService(this, NetworkAddedListener.class);
    }

    private void printResults() {
        for (var x : this.results.entrySet()) {
            System.out.printf("%s,", x.getKey());
        }
        System.out.println();
        for (int i = 0; i < this.results.entrySet().iterator().next().getValue().size(); i++) {
            for (var x : this.results.entrySet()) {
                System.out.printf("%d,", x.getValue().get(i));
            }
            System.out.println();
        }
    }

    public void profileTraceGraphUpdate() throws Exception {
        String fileName = "";

        var task = new LoadPDMTask(this.registrar);
        TunableSetter setter = this.registrar.getService(TunableSetter.class);
        var iter = setter.createTaskIterator(new TaskIterator(task), Map.of("traceFile", new File(fileName)));
        SynchronousTaskManager<?> taskManager = this.registrar.getService(SynchronousTaskManager.class);
        taskManager.execute(iter);
        SwingUtilities.invokeAndWait(() -> {
        });
        TraceGraphController controller = (TraceGraphController) registrar.getService(TraceGraphManager.class)
                                                                          .findControllerForNetwork(this.createdNetwork);
        this.reset();
        var networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.destroyNetwork(this.createdNetwork);

        Parameter param = controller.getPDM().getParameter("speed");
        var originalBins = param.getBins();
        StopWatch watch = new StopWatch();
        var params = new String[]{"situations", "total", "algo", "nodes", "nodesFound", "nodesNotFoundInSubnetwork", "nodesNotFoundInRootNetwork", "leftOverNodes"};
        for (var p : params) {
            results.put(p, new ArrayList<>());
        }

        //var locs = new double[]{10.66, 20.39, 20.7, 35.7, 40.82, 50.68, 61.358, 61.5355, 61.55044};
        var locs = new double[]{10.66, 10.66, 15, 20.39, 20.7, 28, 35.7, 40.82, 45, 50.68, 55, 61.358, 61.5355, 61.55044};
        Profiler.getInstance().reset();

        for (int i = 0; i < locs.length; i++) {
            System.out.println("---------------------------------------------------------------");
            System.out.println(i);
            System.out.println("---------------------------------------------------------------");

            task = new LoadPDMTask(this.registrar);
            iter = setter.createTaskIterator(new TaskIterator(task), Map.of("traceFile", new File(fileName)));
            taskManager.execute(iter);
            SwingUtilities.invokeAndWait(() -> {
            });
            controller = (TraceGraphController) registrar.getService(TraceGraphManager.class)
                                                         .findControllerForNetwork(this.createdNetwork);
            param = controller.getPDM().getParameter("speed");

            var bins = new ArrayList<>(originalBins);
            bins.add(locs[i]);
            long sumTotal = 0;
            long sumAlgorithmic = 0;

            int runs = 10;
            for (int j = 0; j < runs; j++) {

                Thread.sleep(12000);

                watch.start();
                param.setBins(bins);
                SwingUtilities.invokeAndWait(watch::stop);

                var algoResult = Profiler.getInstance().getUpdateTraceGraphResult();
                sumTotal += watch.getTime();
                sumAlgorithmic += algoResult;

                System.out.printf("Total: %d, Algorithmic: %d\n", watch.getTime(), algoResult);

                if (j == runs - 1) {
                    this.results.get("situations").add(Profiler.getInstance().getImpactedSituations());
                    this.results.get("total").add(sumTotal / runs);
                    this.results.get("algo").add(sumAlgorithmic / runs);
                    this.results.get("nodes").add((long) this.createdNetwork.getNodeCount());
                    this.results.get("nodesFound").add(Profiler.getInstance().getFoundNodes());
                    this.results.get("nodesNotFoundInSubnetwork")
                                .add(Profiler.getInstance().getNotFoundInSubnetworkNodes());
                    this.results.get("nodesNotFoundInRootNetwork")
                                .add(Profiler.getInstance().getNotFoundInRootNetworkNodes());
                    this.results.get("leftOverNodes").add(Profiler.getInstance().getLeftOverNodes());
                }

                param.setBins(originalBins);
                SwingUtilities.invokeAndWait(() -> {
                });
                watch.reset();
                this.reset();
            }
            networkManager.destroyNetwork(this.createdNetwork);
            this.reset();
        }

        printResults();
    }

    public void profileTraceLoading() throws Exception {
        String fileName = "";
        SynchronousTaskManager<?> taskManager = this.registrar.getService(SynchronousTaskManager.class);
        TunableSetter setter = this.registrar.getService(TunableSetter.class);
        StopWatch watch = new StopWatch();

        var params = new String[]{"size", "total", "algo", "nodes"};
        for (var p : params) {
            results.put(p, new ArrayList<>());
        }

        int preRuns = 1;
        for (int size = 1; size <= 9; size++) {
            System.out.println("---------------------------------------------------------------");
            System.out.println(size);
            System.out.println("---------------------------------------------------------------");

            if (size > 1 && preRuns > 0) {
                size = 1;
                preRuns -= 1;
            }

            long sumTotal = 0;
            long sumAlgorithmic = 0;
            int runs = 10;
            for (int i = 0; i < runs; i++) {

                Thread.sleep(12000);

                var task = new LoadPDMTask(this.registrar);
                var iter = setter.createTaskIterator(new TaskIterator(task), Map.of("traceFile", new File(String.format(fileName, size))));
                watch.start();
                taskManager.execute(iter);
                SwingUtilities.invokeAndWait(watch::stop);

                var algoResult = Profiler.getInstance().getAddSourceTableResult();
                sumTotal += watch.getTime();
                sumAlgorithmic += algoResult;

                System.out.printf("Total: %d, Algorithmic: %d\n", watch.getTime(), algoResult);

                if (i == runs - 1) {
                    this.results.get("size").add((long) size * 10000);
                    this.results.get("total").add(sumTotal / runs);
                    this.results.get("algo").add(sumAlgorithmic / runs);
                    this.results.get("nodes").add((long) this.createdNetwork.getNodeCount());
                }

                var networkManager = registrar.getService(CyNetworkManager.class);
                networkManager.destroyNetwork(this.createdNetwork);
                this.reset();
                watch.reset();
            }
        }

        printResults();
    }

    public void reset() {
        Profiler.getInstance().reset();
        var eventHelper = registrar.getService(CyEventHelper.class);
        eventHelper.flushPayloadEvents();
        for (int i = 0; i < 5; i++) {
            System.runFinalization();
            System.gc();
        }
    }

    @Override
    public void handleEvent(NetworkAddedEvent e) {
        this.createdNetwork = e.getNetwork();
    }
}
