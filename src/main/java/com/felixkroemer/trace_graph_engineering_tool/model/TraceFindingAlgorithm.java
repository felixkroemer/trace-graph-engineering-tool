package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.util.CustomLoader;
import com.google.ortools.sat.*;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.javatuples.Pair;

import java.util.*;

public class TraceFindingAlgorithm {

    public static Trace findTraceEfficient(TraceGraph traceGraph, Collection<CyNode> nodes) {
        Pair<Integer, Integer> minimumWindow = null;
        CyTable minimumSourceTable = null;
        for (CyTable sourceTable : traceGraph.getSourceTables()) {
            if (isSolutionInfeasible(nodes, sourceTable, traceGraph)) {
                continue;
            }

            Map<CyNode, Integer> histogram = new HashMap<>(nodes.size());
            nodes.forEach(n -> histogram.put(n, 0));
            Collection<Integer> values = histogram.values();


            int start = 1;
            int end = sourceTable.getRowCount();

            // find initial window
            CyNode prevNodeEnd = null;
            for (int i = 1; i <= sourceTable.getRowCount(); i++) {
                if (!values.contains(0)) {
                    end = i - 1;
                    break;
                } else {
                    var node = traceGraph.findNode(sourceTable, i);
                    if ((prevNodeEnd == null || prevNodeEnd != node) && nodes.contains(node)) {
                        histogram.merge(node, 1, Integer::sum);
                    }
                    prevNodeEnd = node;
                }
            }
            if (minimumWindow == null || end - 1 - start < minimumWindow.getValue1() - minimumWindow.getValue0()) {
                minimumWindow = new Pair<>(start, end - 1);
                minimumSourceTable = sourceTable;
            }

            CyNode prevNodeStart = traceGraph.findNode(sourceTable, 1);
            while (end <= sourceTable.getRowCount()) {
                var node = traceGraph.findNode(sourceTable, end);
                if ((prevNodeEnd != node) && nodes.contains(node)) {
                    histogram.merge(node, 1, Integer::sum);
                }
                end += 1;
                prevNodeEnd = node;
                while (!values.contains(1)) {
                    node = traceGraph.findNode(sourceTable, start);
                    if ((prevNodeStart != node) && nodes.contains(node)) {
                        histogram.merge(node, -1, Integer::sum);
                        if (values.contains(1)) {
                            if (end - 1 - start < minimumWindow.getValue1() - minimumWindow.getValue0()) {
                                minimumWindow = new Pair<>(start, end - 1);
                                minimumSourceTable = sourceTable;
                            }

                        }
                    }
                    start += 1;
                    prevNodeStart = node;
                }
            }

        }

        return createTrace(traceGraph, minimumWindow, minimumSourceTable);
    }

    public static Trace findTraceNaive(TraceGraph traceGraph, Collection<CyNode> nodes) {
        Pair<Integer, Integer> minimumWindow = null;
        CyTable minimumSourceTable = null;
        for (CyTable sourceTable : traceGraph.getSourceTables()) {
            Pair<Integer, Integer> window = null;

            if (isSolutionInfeasible(nodes, sourceTable, traceGraph)) {
                continue;
            }

            var list = nodes.stream().toList();
            var nodeA = list.get(0);
            var nodeB = list.get(1);

            var sourcesA = traceGraph.getNodeAux(nodeA).getSourceRows(sourceTable);
            var sourcesB = traceGraph.getNodeAux(nodeB).getSourceRows(sourceTable);

            if (sourcesA == null || sourcesB == null) {
                continue;
            }

            for (var x : sourcesA) {
                for (var y : sourcesB) {
                    if (window == null) {
                        var lowerBound = x < y ? x : y;
                        var upperBound = lowerBound == x ? y : x;
                        window = new Pair<>(lowerBound, upperBound);
                    } else {
                        if (Math.abs(x - y) < window.getValue1() - window.getValue0()) {
                            var lowerBound = x < y ? x : y;
                            var upperBound = lowerBound == x ? y : x;
                            window = new Pair<>(lowerBound, upperBound);
                        }
                    }
                }
            }

            if (window != null) {
                if (minimumWindow == null || window.getValue1() - window.getValue0() < minimumWindow.getValue1() - minimumWindow.getValue0()) {
                    minimumWindow = window;
                    minimumSourceTable = sourceTable;
                }
            }
        }

        return createTrace(traceGraph, minimumWindow, minimumSourceTable);


    }

    private static boolean isSolutionInfeasible(Collection<CyNode> nodes, CyTable sourceTable, TraceGraph traceGraph) {
        for (var node : nodes) {
            if (traceGraph.getNodeAux(node).getSourceRows(sourceTable) == null) {
                return true;
            }
        }
        return false;
    }

    public static Trace findTraceCPSat(TraceGraph traceGraph, List<CyNode> nodes) {
        Pair<Integer, Integer> minimumWindow = null;
        CyTable minimumSourceTable = null;

        CustomLoader.loadNativeLibraries();

        for (CyTable sourceTable : traceGraph.getSourceTables()) {

            if (isSolutionInfeasible(nodes, sourceTable, traceGraph)) {
                continue;
            }

            BoolVar[] vars = new BoolVar[sourceTable.getRowCount()];
            BoolVar[] starts = new BoolVar[sourceTable.getRowCount()];

            CpModel model = new CpModel();
            for (int i = 0; i < sourceTable.getRowCount(); i++) {
                vars[i] = model.newBoolVar("" + i);
                starts[i] = model.newBoolVar("s" + i);
            }

            for (var node : nodes) {
                var sources = traceGraph.getNodeAux(node).getSourceRows(sourceTable);
                IntVar[] expressions = new IntVar[sources.size()];
                int i = 0;
                for (var x : sources) {
                    expressions[i] = vars[x - 1];
                    i = i + 1;
                }
                model.addGreaterOrEqual(LinearExpr.sum(expressions), 1);
            }

            for (int i = 1; i < vars.length; i++) {
                model.addLessOrEqual(LinearExpr.weightedSum(new Literal[]{vars[i], vars[i - 1], starts[i]},
                        new long[]{1, -1, -1}), 0);
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
                    minimumSourceTable = sourceTable;
                }

            }
        }

        return createTrace(traceGraph, minimumWindow, minimumSourceTable);
    }

    private static Trace createTrace(TraceGraph traceGraph, Pair<Integer, Integer> minimumWindow,
                                     CyTable minimumSourceTable) {
        if (minimumWindow != null) {
            var traceNodes = new ArrayList<CyNode>();
            for (int i = minimumWindow.getValue0(); i <= minimumWindow.getValue1(); i++) {
                traceNodes.add(traceGraph.findNode(minimumSourceTable, i));
            }
            return new Trace(minimumSourceTable, traceNodes, minimumWindow.getValue0());
        } else {
            return null;
        }
    }


}
