package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.util.CustomLoader;
import com.google.ortools.sat.*;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.javatuples.Pair;

import java.util.*;

public class MinimalSubtraceAlgorithm {

    public static SubTrace findMinimalSubtraceEfficient(TraceGraph traceGraph, Collection<CyNode> nodes) {
        Pair<Integer, Integer> minimumWindow = new Pair<>(0, Integer.MAX_VALUE);
        CyTable minimumTrace = null;
        for (CyTable trace : traceGraph.getTraces()) {
            if (isSolutionInfeasible(nodes, trace, traceGraph)) {
                continue;
            }
            Map<CyNode, Integer> index = new LinkedHashMap<>();

            for (int i = 0; i < trace.getRowCount(); i++) {
                var node = traceGraph.findNode(trace, i);
                if (nodes.contains(node)) {
                    index.remove(node);
                    index.put(node, i);
                    if (index.size() == nodes.size()) {
                        int j = index.values().iterator().next();
                        if (minimumWindow.getValue1() - minimumWindow.getValue0() > i - j) {
                            minimumWindow = new Pair<>(j, i);
                            minimumTrace = trace;
                        }
                    }
                }
            }
        }
        return createTrace(traceGraph, minimumWindow, minimumTrace);
    }

    public static SubTrace findMinimalSubtraceNaive(TraceGraph traceGraph, Collection<CyNode> nodes) {
        Pair<Integer, Integer> minimumWindow = null;
        CyTable minimumTrace = null;
        for (CyTable trace : traceGraph.getTraces()) {
            Pair<Integer, Integer> window = null;

            if (isSolutionInfeasible(nodes, trace, traceGraph)) {
                continue;
            }

            var list = nodes.stream().toList();
            var nodeA = list.get(0);
            var nodeB = list.get(1);

            var sourcesA = traceGraph.getNodeAux(nodeA).getSituations(trace);
            var sourcesB = traceGraph.getNodeAux(nodeB).getSituations(trace);

            if (sourcesA == null || sourcesB == null) {
                continue;
            }

            for (var x : sourcesA) {
                for (var y : sourcesB) {
                    if (window == null) {
                        var lowerBound = x < y ? x : y;
                        var upperBound = lowerBound.equals(x) ? y : x;
                        window = new Pair<>(lowerBound, upperBound);
                    } else {
                        if (Math.abs(x - y) < window.getValue1() - window.getValue0()) {
                            var lowerBound = x < y ? x : y;
                            var upperBound = lowerBound.equals(x) ? y : x;
                            window = new Pair<>(lowerBound, upperBound);
                        }
                    }
                }
            }

            if (window != null) {
                if (minimumWindow == null || window.getValue1() - window.getValue0() < minimumWindow.getValue1() - minimumWindow.getValue0()) {
                    minimumWindow = window;
                    minimumTrace = trace;
                }
            }
        }

        return createTrace(traceGraph, minimumWindow, minimumTrace);
    }

    private static boolean isSolutionInfeasible(Collection<CyNode> nodes, CyTable trace, TraceGraph traceGraph) {
        for (var node : nodes) {
            if (traceGraph.getNodeAux(node).getSituations(trace) == null) {
                return true;
            }
        }
        return false;
    }

    public static SubTrace findMinimalSubtraceCPSat(TraceGraph traceGraph, List<CyNode> nodes) {
        Pair<Integer, Integer> minimumWindow = null;
        CyTable minimumTrace = null;

        CustomLoader.loadNativeLibraries();

        for (CyTable trace : traceGraph.getTraces()) {

            if (isSolutionInfeasible(nodes, trace, traceGraph)) {
                continue;
            }

            BoolVar[] vars = new BoolVar[trace.getRowCount()];
            BoolVar[] starts = new BoolVar[trace.getRowCount()];

            CpModel model = new CpModel();
            for (int i = 0; i < trace.getRowCount(); i++) {
                vars[i] = model.newBoolVar("" + i);
                starts[i] = model.newBoolVar("s" + i);
            }

            for (var node : nodes) {
                var sources = traceGraph.getNodeAux(node).getSituations(trace);
                IntVar[] expressions = new IntVar[sources.size()];
                int i = 0;
                for (var x : sources) {
                    expressions[i] = vars[x - 1];
                    i = i + 1;
                }
                model.addGreaterOrEqual(LinearExpr.sum(expressions), 1);
            }

            for (int i = 1; i < vars.length; i++) {
                model.addLessOrEqual(LinearExpr.weightedSum(new Literal[]{vars[i], vars[i - 1], starts[i]}, new long[]{1, -1, -1}), 0);
            }
            model.addImplication(vars[0], starts[0]);

            model.addLessOrEqual(LinearExpr.sum(starts), 1);

            model.minimize(LinearExpr.sum(vars));

            // Create a solver and solve the model.
            CpSolver solver = new CpSolver();
            solver.getParameters().setCpModelPresolve(false);
            solver.getParameters().setLogSearchProgress(true);
            CpSolverStatus status = solver.solve(model);

            if (status == CpSolverStatus.OPTIMAL) {
                int start = -1;
                int end = vars.length - 1;
                for (int i = 0; i < vars.length; i++) {
                    if (start == -1 && solver.value(vars[i]) == 1) {
                        start = i + 1;
                        continue;
                    }
                    if (start != -1 && solver.value(vars[i]) == 0) {
                        end = i;
                        break;
                    }
                }

                if (minimumWindow == null || end - start < minimumWindow.getValue1() - minimumWindow.getValue0()) {
                    minimumWindow = new Pair<>(start, end);
                    minimumTrace = trace;
                }
            }
        }

        return createTrace(traceGraph, minimumWindow, minimumTrace);
    }

    private static SubTrace createTrace(TraceGraph traceGraph, Pair<Integer, Integer> minimumWindow,
                                        CyTable minimumTrace) {
        if (minimumTrace != null) {
            var traceNodes = new ArrayList<CyNode>();
            for (int i = minimumWindow.getValue0(); i <= minimumWindow.getValue1(); i++) {
                traceNodes.add(traceGraph.findNode(minimumTrace, i));
            }
            return new SubTrace(minimumTrace, traceNodes, minimumWindow.getValue0());
        } else {
            return null;
        }
    }
}
