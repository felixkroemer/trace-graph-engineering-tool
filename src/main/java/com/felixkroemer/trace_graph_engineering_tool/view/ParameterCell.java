package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ParameterCell extends JPanel implements PropertyChangeListener {

    private JCheckBox checkBox;
    private JLabel label;
    private JButton editButton;

    public ParameterCell(Parameter parameter) {
        setLayout(new BorderLayout());
        this.checkBox = new JCheckBox();
        this.add(this.checkBox, BorderLayout.WEST);
        this.checkBox.setSelected(parameter.isEnabled());
        this.label = new JLabel(parameter.getName());
        this.add(this.label, BorderLayout.CENTER);
        this.label.setHorizontalAlignment(JLabel.CENTER);
        this.editButton = new JButton();
        this.editButton.setIcon(UIManager.getIcon("FileView.fileIcon"));
        this.add(this.editButton, BorderLayout.EAST);
    }

    public JCheckBox getCheckBox() {
        return this.checkBox;
    }

    public JLabel getLabel() {
        return this.label;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "enabled":
                this.label.setEnabled((boolean) evt.getNewValue());
                this.editButton.setEnabled((boolean) evt.getNewValue());
                break;
            case "bins":
                break;
        }
    }


}
