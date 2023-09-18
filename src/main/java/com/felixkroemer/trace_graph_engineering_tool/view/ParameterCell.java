package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.work.TaskManager;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static java.awt.Dialog.ModalityType.APPLICATION_MODAL;
import static org.cytoscape.util.swing.IconManager.ICON_EDIT;

public class ParameterCell extends JPanel implements PropertyChangeListener {

    private JCheckBox checkBox;
    private JLabel label;
    private JButton editButton;
    private TaskManager manager;

    public ParameterCell(Parameter parameter, CyServiceRegistrar reg) {
        IconManager iconManager = reg.getService(IconManager.class);
        TaskManager<?, ?> manager = reg.getService(TaskManager.class);


        setLayout(new BorderLayout());
        this.checkBox = new JCheckBox();
        this.add(this.checkBox, BorderLayout.WEST);
        this.checkBox.setSelected(parameter.isEnabled());
        this.label = new JLabel(parameter.getName());
        this.add(this.label, BorderLayout.CENTER);
        this.label.setHorizontalAlignment(JLabel.CENTER);
        this.editButton = new JButton(ICON_EDIT);
        this.editButton.setFont(iconManager.getIconFont(14.0f));
        this.editButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                SelectBinsDialog d = new SelectBinsDialog();
                d.setTitle("Select Bins");
                d.setContentPane(new SelectBinsPanel());
                d.setModalityType(APPLICATION_MODAL);
                int res = d.showDialog();
                // parameter.setBins(...)
            });
        });
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
