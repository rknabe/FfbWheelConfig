package com.rkade;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;
import java.util.logging.Logger;

public class ButtonsPanel extends BaseForm implements DeviceListener, ActionListener, FocusListener, ChangeListener {
    private final static Logger logger = Logger.getLogger(ButtonsPanel.class.getName());
    private JPanel mainButtonPanel;
    private JButton a32Button;
    private JSpinner debounceSpinner;
    private Device device = null;

    public ButtonsPanel() {
        controls = List.of(debounceSpinner, a32Button);
        setPanelEnabled(false);
        setupControlListener();

        SpinnerModel sm = new SpinnerNumberModel(0, 0, 255, 1);
        debounceSpinner.setModel(sm);
    }

    @Override
    public void deviceAttached(Device device) {
        this.device = device;
        setPanelEnabled(true);
    }

    @Override
    public void deviceDetached(Device device) {
        this.device = null;
        setPanelEnabled(false);
    }

    @Override
    public void deviceUpdated(Device device, String status, DataReport report) {
        if (report != null) {
            if (report.getReportType() == Device.DATA_REPORT_ID) {
                if (report instanceof ButtonsDataReport buttonsDataReport) {
                    updateControls(buttonsDataReport);
                }
            }
        }
    }

    private void updateControls(ButtonsDataReport buttonsDataReport) {
        System.out.println(buttonsDataReport);
    }

    @Override
    public void focusLost(FocusEvent e) {
        boolean status = handleFocusLost(e);
        if (!status) {
            logger.warning("Focus lost, failed for:" + e.getSource());
        }
    }

    private boolean handleFocusLost(FocusEvent e) {
        if (e.getSource() == debounceSpinner) {
            //return device.setGainValue(gainIndex, Short.parseShort(gainText.getText()));
        }
        return true;
    }

    @Override
    public void stateChanged(ChangeEvent e) {

    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainButtonPanel = new JPanel();
        mainButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        mainButtonPanel.setMinimumSize(new Dimension(1040, 200));
        mainButtonPanel.setName("mainButtonPanel");
        mainButtonPanel.setPreferredSize(new Dimension(1040, 800));
        debounceSpinner = new JSpinner();
        debounceSpinner.setMinimumSize(new Dimension(88, 35));
        debounceSpinner.setPreferredSize(new Dimension(88, 35));
        mainButtonPanel.add(debounceSpinner);
        a32Button = new JButton();
        a32Button.setMinimumSize(new Dimension(50, 50));
        a32Button.setPreferredSize(new Dimension(50, 50));
        a32Button.setText("32");
        mainButtonPanel.add(a32Button);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainButtonPanel;
    }

}