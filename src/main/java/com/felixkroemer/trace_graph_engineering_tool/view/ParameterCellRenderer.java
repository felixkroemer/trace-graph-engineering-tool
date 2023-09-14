package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;

import javax.swing.*;
import java.awt.*;

public class ParameterCellRenderer implements ListCellRenderer<Parameter> {

    @Override
    public Component getListCellRendererComponent(JList<? extends Parameter> list, Parameter value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        JPanel panel = new JPanel(new BorderLayout());
        JCheckBox enabled = new JCheckBox();
        panel.add(enabled, BorderLayout.WEST);
        JLabel name = new JLabel(value.getName());
        panel.add(name, BorderLayout.CENTER);
        JButton edit = new JButton();
        edit.setIcon(UIManager.getIcon("FileView.fileIcon"));
        panel.add(edit, BorderLayout.EAST);

        return panel;
    }
}
