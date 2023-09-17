package com.felixkroemer.trace_graph_engineering_tool.view;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ParameterCell extends JPanel implements PropertyChangeListener {

    private JCheckBox checkBox;
    private JLabel label;

    public ParameterCell(String name) {
        setLayout(new BorderLayout());
        this.checkBox = new JCheckBox();
        this.add(this.checkBox, BorderLayout.WEST);
        this.label = new JLabel(name);
        this.add(this.label, BorderLayout.CENTER);
        this.label.setHorizontalAlignment(JLabel.CENTER);
        JButton edit = new JButton();
        edit.setIcon(UIManager.getIcon("FileView.fileIcon"));
        this.add(edit, BorderLayout.EAST);
    }

    public JCheckBox getCheckBox() {
        return this.checkBox;
    }

    public JLabel getLabel() {
        return this.label;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("enabled".equals(evt.getPropertyName())) {
            this.checkBox.setEnabled((boolean) evt.getNewValue());
        }
    }


}
