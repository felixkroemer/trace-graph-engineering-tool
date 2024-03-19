package com.felixkroemer.trace_graph_engineering_tool.util;

import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.opencsv.CSVReader;
import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;

import static com.felixkroemer.trace_graph_engineering_tool.model.Columns.NETWORK_COMPARISON_MARKER;
import static com.felixkroemer.trace_graph_engineering_tool.model.Columns.NETWORK_TG_MARKER;

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

    public static CySubNetwork createSubNetwork(ParameterDiscretizationModel pdm) {
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

    public static void deselectAll(CyNetworkView view) {
        for (var edgeView : view.getEdgeViews()) {
            view.getModel().getRow(edgeView.getModel()).set(CyNetwork.SELECTED, false);
        }
        for (var nodeView : view.getNodeViews()) {
            view.getModel().getRow(nodeView.getModel()).set(CyNetwork.SELECTED, false);
        }
    }

    public static void showDialog(JPanel panel, String title, Dimension dimension) {
        var dialog = createDialog(panel, title);
        dialog.setPreferredSize(dimension);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    public static void showDialog(JPanel panel, String title) {
        var dialog = createDialog(panel, title);
        dialog.setVisible(true);
    }

    private static JDialog createDialog(JPanel panel, String title) {
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setContentPane(panel);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        return dialog;
    }

    public static void clearFacadeRows(CyNetwork network) {
        var root = ((CySubNetwork) network).getRootNetwork();
        Object rootDefaultEdgeTable = root.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS);
        Object rootDefaultNodeTable = root.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS);

        Object[] fields = {rootDefaultEdgeTable, rootDefaultNodeTable};
        for (var f : fields) {
            try {
                // Assuming the field you want to access is named 'mapFieldName'
                Field field = f.getClass().getSuperclass().getDeclaredField("facadeRows");
                field.setAccessible(true); // Bypasses Java access checking

                // Access the Map and clear it
                Map<?, ?> map = (Map<?, ?>) field.get(f);
                map.clear();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static void rehash(CyNetwork network) {
        var root = ((CySubNetwork) network).getRootNetwork();
        try {
            var rootNetworkNodePointersField = root.getClass().getSuperclass().getSuperclass()
                                                   .getDeclaredField("nodePointers");
            var subnetworkNodePointersField = root.getClass().getSuperclass().getSuperclass()
                                                  .getDeclaredField("nodePointers");
            var method = rootNetworkNodePointersField.getType().getDeclaredMethod("rehash", int.class);
            rootNetworkNodePointersField.setAccessible(true);
            subnetworkNodePointersField.setAccessible(true);
            method.setAccessible(true);
            var rootNetworkNodePointers = rootNetworkNodePointersField.get(root);
            var subnetworkNodePointers = subnetworkNodePointersField.get(network);
            method.invoke(rootNetworkNodePointers, 100000);
            method.invoke(subnetworkNodePointers, 100000);
        } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
