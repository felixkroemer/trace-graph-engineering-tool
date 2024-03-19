package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.model.Profiler;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.*;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

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
        this.profileTraceGraphUpdate();
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
        var iter = setter.createTaskIterator(new TaskIterator(task), Map.of("traceFile", new File(fileName + ".json")));
        SynchronousTaskManager<?> taskManager = this.registrar.getService(SynchronousTaskManager.class);
        taskManager.execute(iter);
        SwingUtilities.invokeAndWait(() -> {
        });
        TraceGraphController controller = (TraceGraphController) registrar.getService(TraceGraphManager.class)
                                                                          .findControllerForNetwork(this.createdNetwork);
        var originalBins = controller.getPDM().getParameter("speed").getBins();
        this.reset();
        var networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.destroyNetwork(this.createdNetwork);

        StopWatch watch = new StopWatch();
        var params = new String[]{"situations", "total", "algo", "nodes", "nodesFound", "nodesNotFoundInSubnetwork", "nodesNotFoundInRootNetwork", "leftOverNodes"};
        for (var p : params) {
            results.put(p, new ArrayList<>());
        }

        var locs = new ArrayList<Double>();
        var trace = controller.getTraceGraph().getTraces().iterator().next();
        var speeds = trace.getColumn("speed").getValues(Double.class);
        Collections.sort(speeds);
        var step = trace.getRowCount() / 12;
        int n = 0;
        locs.add(speeds.get(0));
        for (var speed : speeds) {
            if (n > step) {
                locs.add(speed);
                n = 0;
            }
            n++;
        }

        Profiler.getInstance().reset();

        int preRuns = 1;
        for (int i = 0; i < locs.size(); i++) {
            if (preRuns >= 0) {
                i = 0;
                preRuns -= 1;
            }
            System.out.println("---------------------------------------------------------------");
            System.out.println(i);
            System.out.println("---------------------------------------------------------------");

            task = new LoadPDMTask(this.registrar);
            iter = setter.createTaskIterator(new TaskIterator(task), Map.of("traceFile", new File(fileName + ".json")));
            taskManager.execute(iter);
            SwingUtilities.invokeAndWait(() -> {
            });
            controller = (TraceGraphController) registrar.getService(TraceGraphManager.class)
                                                         .findControllerForNetwork(this.createdNetwork);

/*            var root = ((CySubNetwork) controller.getTraceGraph().getNetwork()).getRootNetwork();
            var rootDefaultNodeTable = root.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);
            var rootDefaultEdgeTable = root.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS);
            var x = registrar.getService(CyNetworkTableManager.class);
            x.setTable(root, CyNode.class, CyNetwork.DEFAULT_ATTRS, null);
            x.setTable(root, CyEdge.class, CyNetwork.DEFAULT_ATTRS, null);*/

            var param = controller.getPDM().getParameter("speed");

            var bins = new ArrayList<>(originalBins);
            bins.add(locs.get(i));
            long sumTotal = 0;
            long sumAlgorithmic = 0;

            int runs = 20;
            for (int j = 0; j < runs; j++) {
                Thread.sleep(10000);

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
                Util.rehash(controller.getNetwork());
                this.reset();
            }
            networkManager.destroyNetwork(this.createdNetwork);
            this.reset();
        }

        printResults();
    }

    public void profileTraceLoading() throws Exception {
        String path = "";
        SynchronousTaskManager<?> taskManager = this.registrar.getService(SynchronousTaskManager.class);
        TunableSetter setter = this.registrar.getService(TunableSetter.class);
        StopWatch watch = new StopWatch();

        var params = new String[]{"size", "total", "algo", "nodes"};
        for (var p : params) {
            results.put(p, new ArrayList<>());
        }

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String pdmString = Files.readString(new File(path + ".json").toPath());
        var pdm = gson.fromJson(pdmString, ParameterDiscretizationModelDTO.class);

        int preRuns = 1;
        for (int size = 5; size <= 90; size += 5) {
            if (preRuns >= 0) {
                size = 5;
                preRuns -= 1;
            }

            System.out.println("---------------------------------------------------------------");
            System.out.println(size);
            System.out.println("---------------------------------------------------------------");

            long sumTotal = 0;
            long sumAlgorithmic = 0;
            int runs = 20;
            for (int i = 0; i < runs; i++) {

                Thread.sleep(5000);

                System.out.println("------------------------------");

                pdm.getCsvs().clear();
                var task = new LoadPDMTask(this.registrar);
                var tempTrace = File.createTempFile(String.format("temp_%d000", size), ".csv");
                pruneFileToLineCount(new File(path + ".csv"), tempTrace, size * 1000 + 1);
                pdm.getCsvs().add(tempTrace.getName());
                var tempPDM = File.createTempFile(String.format("temp_%d000", size), ".json");
                try (FileWriter writer = new FileWriter(tempPDM)) {
                    gson.toJson(pdm, writer);
                    writer.flush();
                }

                var iter = setter.createTaskIterator(new TaskIterator(task), Map.of("traceFile", tempPDM));
                watch.start();
                taskManager.execute(iter);
                SwingUtilities.invokeAndWait(watch::stop);

                var algoResult = Profiler.getInstance().getAddTraceResult();
                sumTotal += watch.getTime();
                sumAlgorithmic += algoResult;

                System.out.printf("Total: %d, Algorithmic: %d\n", watch.getTime(), algoResult);

                if (i == runs - 1) {
                    this.results.get("size").add((long) size * 1000);
                    this.results.get("total").add(sumTotal / runs);
                    this.results.get("algo").add(sumAlgorithmic / runs);
                    this.results.get("nodes").add((long) this.createdNetwork.getNodeCount());
                }

                System.out.println("------------------------------");

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

    public static void pruneFileToLineCount(File sourceFile, File targetFile, int lineCount) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile)); BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
            String line;
            int currentLine = 0;
            while ((line = reader.readLine()) != null && currentLine < lineCount) {
                writer.write(line);
                writer.newLine();
                currentLine++;
            }
        }
    }
}