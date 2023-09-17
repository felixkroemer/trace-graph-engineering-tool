package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.cytoscape.util.swing.IconManager.ICON_EDIT;

public class ParameterCell extends JPanel implements PropertyChangeListener {

    private JCheckBox checkBox;
    private JLabel label;
    private JButton editButton;

    public ParameterCell(Parameter parameter, CyServiceRegistrar reg) {
        IconManager iconManager = reg.getService(IconManager.class);

        setLayout(new BorderLayout());
        this.checkBox = new JCheckBox();
        this.add(this.checkBox, BorderLayout.WEST);
        this.checkBox.setSelected(parameter.isEnabled());
        this.label = new JLabel(parameter.getName());
        this.add(this.label, BorderLayout.CENTER);
        this.label.setHorizontalAlignment(JLabel.CENTER);
        this.editButton = new JButton(ICON_EDIT);
        this.editButton.setFont(iconManager.getIconFont(14.0f));
        this.add(this.editButton, BorderLayout.EAST);
        this.setPreferredSize(new Dimension(getPreferredSize().height, getPreferredSize().height));
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
