package com.felixkroemer.trace_graph_engineering_tool.util;

import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.opencsv.CSVReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

import java.io.File;
import java.io.FileReader;
import java.util.*;

public class Util {

    public static Properties genProperties(Map<String, String> input) {
        Properties props = new Properties();
        input.forEach(props::setProperty);
        return props;
    }

    public static boolean isTraceGraphNetwork(CyNetwork network) {
        return network.getDefaultNetworkTable().getColumn("TraceGraphMarker") != null;
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
                    for(int i = 1; i<params.size(); i++) {
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

}
