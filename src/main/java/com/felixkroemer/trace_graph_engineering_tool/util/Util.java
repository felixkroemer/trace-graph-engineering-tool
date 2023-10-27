package com.felixkroemer.trace_graph_engineering_tool.util;

import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.opencsv.CSVReader;
import org.cytoscape.model.*;

import java.io.File;
import java.io.FileReader;
import java.util.*;

import static com.felixkroemer.trace_graph_engineering_tool.model.Columns.*;

public class Util {

    public static Properties genProperties(Map<String, String> input) {
        Properties props = new Properties();
        input.forEach(props::setProperty);
        return props;
    }

    public static boolean isTraceGraphNetwork(CyNetwork network) {
        return network.getDefaultNetworkTable().getColumn(NETWORK_TG_MARKER) != null;
    }

    public static boolean isComparisonNetwork(CyNetwork network) {
        return network.getDefaultNetworkTable().getColumn(NETWORK_COMPARISON_MARKER) != null;
    }

    public static boolean isTraceDetailsNetwork(CyNetwork network) {
        return network.getDefaultNetworkTable().getColumn(NETWORK_TRACE_DETAILS_MARKER) != null;
    }

    public static void parseCSV(CyTable table, File csv) throws Exception {
        List<String> params = new ArrayList<>();
        boolean header = true;
        try (CSVReader reader = new CSVReader(new FileReader(csv))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (header) {
                    header = false;
                    params = Arrays.asList(line);
                    for (int i = 1; i < params.size(); i++) {
                        table.createColumn(params.get(i), Double.class, false);
                    }
                    continue;
                }
                CyRow row = table.getRow(Long.parseLong(line[0]));
                for (int i = 1; i < line.length; i++) {
                    // csv has some empty entries
                    if (!line[i].isEmpty()) {
                        row.set(params.get(i), Double.parseDouble(line[i]));
                    } else {
                        row.set(params.get(i), 0.0);
                    }
                }
            }
        }
    }

    public static CyEdge getEdge(CyNetwork network, CyNode source, CyNode target) {
        List<CyEdge> edges = network.getConnectingEdgeList(source, target, CyEdge.Type.DIRECTED);
        for (CyEdge edge : edges) {
            if (edge.getTarget() == target && edge.getSource() == source) {
                return edge;
            }
        }
        return null;
    }

    public static CyNetwork createSubNetwork(ParameterDiscretizationModel pdm) {
        var root = pdm.getRootNetwork();
        var subNetwork = root.addSubNetwork();

        var localNetworkTable = subNetwork.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS);
        localNetworkTable.createColumn(Columns.NETWORK_TG_MARKER, Integer.class, true);

        return subNetwork;
    }

    public static String getSubNetworkName(Collection<CyTable> tables) {
        StringBuilder sb = new StringBuilder();
        for (var table : tables) {
            sb.append(table).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

}
