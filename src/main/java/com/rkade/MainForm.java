package com.rkade;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class MainForm extends BaseForm implements DeviceListener, ActionListener, FocusListener, ChangeListener {
    private final static Logger logger = Logger.getLogger(MainForm.class.getName());
    private final static List<String> axisLabels = List.of(
            "Axis 1 (Y - Accelerator)",
            "Axis 2 (Z - Brake)",
            "Axis 3 (rX - Clutch)",
            "Axis 4 (rY - Aux 1)",
            "Axis 5 (rZ - Aux 2)",
            "Axis 6 (Slider - Aux 3)",
            "Axis 7 (Dial - Aux 4)");
    private final static List<String> gainLabels = List.of(
            "All",
            "Constant",
            "Ramp",
            "Square",
            "Sine",
            "Triangle",
            "Sawtooth Up",
            "Sawtooth Down",
            "Spring",
            "Damper",
            "Inertia",
            "Friction");
    private List<AxisPanel> axisPanels;
    private List<GainPanel> gainPanels;
    private JPanel mainPanel;
    private JTabbedPane mainTab;
    private JPanel ffbTab;
    private JPanel bottomPanel;
    private JComboBox<String> deviceComboBox;
    private JLabel statusLabel;
    private JComboBox<String> rangeComboBox;
    private JLabel rangeLabel;
    private JButton centerButton;
    private JTextField velocityText;
    private JTextField accText;
    private JLabel accLabel;
    private JLabel degreesLabel;
    private JPanel wheelPanel;
    private JPanel axisPanel;
    private JLabel wheelIconLabel;
    private JLabel wheelRawLabel;
    private JTextField wheelRawTextField;
    private JTextField wheelValueTextField;
    private JLabel wheelValueLabel;
    private JLabel versionLabel;
    private AxisPanel axis1Panel;
    private AxisPanel axis2Panel;
    private AxisPanel axis3Panel;
    private AxisPanel axis4Panel;
    private AxisPanel axis5Panel;
    private AxisPanel axis6Panel;
    private AxisPanel axis7Panel;
    private JScrollPane axisScroll;
    private JPanel axesTab;
    private JButton defaultsButton;
    private JButton loadButton;
    private JButton saveButton;
    private JButton autoCenterButton;
    private JScrollPane ffbScroll;
    private JPanel ffbSubPanel;
    private JPanel gainsPanel;
    private JPanel rightPanel;
    private JPanel miscPanel;
    private JPanel testPanel;
    private JButton constantLeftButton;
    private JButton sineButton;
    private JButton springButton;
    private JButton frictionButton;
    private JButton rampButton;
    private JButton sawtoothUpButton;
    private JButton sawtoothDownButton;
    private JButton inertiaButton;
    private JButton damperButton;
    private JButton triangleButton;
    private JButton constantRightButton;
    private JFormattedTextField maxVelocityFrictionText;
    private JFormattedTextField maxVelocityInertiaText;
    private JFormattedTextField minForceText;
    private JSlider minForceSlider;
    private JSlider maxForceSlider;
    private JFormattedTextField maxForceText;
    private JFormattedTextField cutForceText;
    private JSlider cutForceSlider;
    private JComboBox<String> frequencyCombo;
    private JFormattedTextField maxVelocityDamperText;
    private JLabel minForceLabel;
    private JLabel maxForceLabel;
    private JLabel cutForceLabel;
    private JCheckBox constantSpringCheckBox;
    private ButtonsPanel buttonsPanel;
    private JPanel buttonsTab;
    private BufferedImage wheelImage;
    private double prevWheelRotation = 0.0;
    private Device device = null;
    private volatile boolean isWaitingOnDevice = false;

    public MainForm() {
        try {
            ImageIcon imageIcon = new ImageIcon(ClassLoader.getSystemResource("wheel55.png"));
            wheelImage = toBufferedImage(imageIcon.getImage());
            wheelIconLabel.setIcon(imageIcon);
        } catch (Exception ex) {
            logger.warning(ex.getMessage());
        }
        rangeComboBox.addItem("180");
        rangeComboBox.addItem("270");
        rangeComboBox.addItem("360");
        rangeComboBox.addItem("540");
        rangeComboBox.addItem("720");
        rangeComboBox.addItem("900");
        rangeComboBox.addItem("1080");

        frequencyCombo.addItem("8bit, 31.25kHz");
        frequencyCombo.addItem("9bit, 15.625kHz");
        frequencyCombo.addItem("10bit, 7.85kHz");
        frequencyCombo.addItem("11bit, 3.9kHz");
        frequencyCombo.addItem("12bit, 1.95kHz");
        frequencyCombo.addItem("13bit, 0.97kHz");
        frequencyCombo.addItem("14bit, 0.48kHz");

        setupAxisPanels();
        setupGainPanels();

        controls = List.of(deviceComboBox, rangeComboBox, centerButton, autoCenterButton, saveButton, defaultsButton,
                loadButton, constantLeftButton, constantRightButton, sineButton, springButton, frictionButton,
                rampButton, sawtoothUpButton, sawtoothDownButton, inertiaButton, damperButton, triangleButton,
                constantSpringCheckBox, maxVelocityDamperText, maxVelocityInertiaText, maxVelocityFrictionText,
                minForceText, maxForceText, cutForceText, minForceSlider, maxForceSlider, cutForceSlider, frequencyCombo);

        setupControlListener();

        maxVelocityDamperText.setFormatterFactory(new IntegerFormatterFactory(0, Short.MAX_VALUE - 1));
        maxVelocityInertiaText.setFormatterFactory(new IntegerFormatterFactory(0, Short.MAX_VALUE - 1));
        maxVelocityFrictionText.setFormatterFactory(new IntegerFormatterFactory(0, Short.MAX_VALUE - 1));
        minForceText.setFormatterFactory(new IntegerFormatterFactory(0, 16383));
        maxForceText.setFormatterFactory(new IntegerFormatterFactory(0, 16383));
        cutForceText.setFormatterFactory(new IntegerFormatterFactory(0, 16383));
        minForceSlider.setMinimum(0);
        minForceSlider.setMaximum(16383);
        maxForceSlider.setMinimum(0);
        maxForceSlider.setMaximum(16383);
        cutForceSlider.setMinimum(0);
        cutForceSlider.setMaximum(16383);

        setPanelEnabled(false);
    }

    private void setupAxisPanels() {
        axisPanels = List.of(axis1Panel, axis2Panel, axis3Panel, axis4Panel, axis5Panel, axis6Panel, axis7Panel);
        for (short i = 0; i < axisPanels.size(); i++) {
            AxisPanel panel = axisPanels.get(i);
            if (panel != null) {
                panel.setAxisLabel(axisLabels.get(i));
                panel.setAxisIndex(i);
            }
        }
    }

    private void setupGainPanels() {
        gainPanels = List.of(new GainPanel(), new GainPanel(), new GainPanel(), new GainPanel(), new GainPanel(), new GainPanel(),
                new GainPanel(), new GainPanel(), new GainPanel(), new GainPanel(), new GainPanel(), new GainPanel());
        for (short i = 0; i < gainPanels.size(); i++) {
            GainPanel panel = gainPanels.get(i);
            if (panel != null) {
                gainsPanel.add(panel.$$$getRootComponent$$$());
                panel.setGainLabel(gainLabels.get(i));
                panel.setGainIndex(i);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean status = handleAction(e);
        if (!status) {
            logger.warning("Action failed for:" + e.getActionCommand());
        }
    }

    private boolean handleAction(ActionEvent e) {
        if (device != null) {
            if (e.getActionCommand().equals(centerButton.getActionCommand())) {
                return device.setWheelCenter();
            } else if (e.getActionCommand().equals(rangeComboBox.getActionCommand())) {
                return device.setWheelRange(Short.valueOf(Objects.requireNonNull(rangeComboBox.getSelectedItem()).toString()));
            } else if (e.getActionCommand().equals(frequencyCombo.getActionCommand())) {
                return device.setMiscValue(Device.MISC_FFBBD, (short) (frequencyCombo.getSelectedIndex() + 8));
            } else if (e.getActionCommand().equals(autoCenterButton.getActionCommand())) {
                return doWheelAutoCenter();
            } else if (e.getActionCommand().equals(constantLeftButton.getActionCommand())) {
                return device.doFfbConstantLeft();
            } else if (e.getActionCommand().equals(constantRightButton.getActionCommand())) {
                return device.doFfbConstantRight();
            } else if (e.getActionCommand().equals(sineButton.getActionCommand())) {
                return device.doFfbSine();
            } else if (e.getActionCommand().equals(springButton.getActionCommand())) {
                return device.doFfbSpring();
            } else if (e.getActionCommand().equals(frictionButton.getActionCommand())) {
                return device.doFfbFriction();
            } else if (e.getActionCommand().equals(rampButton.getActionCommand())) {
                return device.doFfbRamp();
            } else if (e.getActionCommand().equals(sawtoothUpButton.getActionCommand())) {
                return device.doFfbSawtoothUp();
            } else if (e.getActionCommand().equals(sawtoothDownButton.getActionCommand())) {
                return device.doFfbSawtoothDown();
            } else if (e.getActionCommand().equals(inertiaButton.getActionCommand())) {
                return device.doFfbInertia();
            } else if (e.getActionCommand().equals(damperButton.getActionCommand())) {
                return device.doFfbDamper();
            } else if (e.getActionCommand().equals(triangleButton.getActionCommand())) {
                return device.doFfbTriangle();
            } else if (e.getActionCommand().equals(constantSpringCheckBox.getActionCommand())) {
                return device.doSetConstantSpring(constantSpringCheckBox.isSelected());
            } else if (e.getActionCommand().equals(saveButton.getActionCommand())) {
                isWaitingOnDevice = true;
                boolean status = device.saveSettings();
                showWaitDialog();
                return status;
            } else if (e.getActionCommand().equals(defaultsButton.getActionCommand())) {
                isWaitingOnDevice = true;
                boolean status = device.loadDefaults();
                showWaitDialog();
                return status;
            } else if (e.getActionCommand().equals(loadButton.getActionCommand())) {
                isWaitingOnDevice = true;
                boolean status = device.loadFromEeprom();
                showWaitDialog();
                return status;
            }
        }
        return true;
    }

    private void showWaitDialog() {
        JLabel validator = new JLabel("<html><body>Please wait, this may take up to 1 minute.</body></html>");
        JOptionPane pane = new JOptionPane(validator, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, new Object[]{}, null);
        final JDialog dialog = pane.createDialog(mainPanel, "Loading Settings...");
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            public Void doInBackground() {
                setPanelEnabled(false);
                int seconds = 0;
                isWaitingOnDevice = true;
                do {
                    try {
                        Thread.sleep(1000);
                        validator.setText(String.format("<html><body>Please wait, this may take up to 1 minute.<br/>Elapsed Seconds: %d</body></html>", ++seconds));
                    } catch (InterruptedException ignored) {
                    }
                } while (isWaitingOnDevice);
                dialog.setVisible(false);
                setPanelEnabled(true);
                return null;
            }
        };
        isWaitingOnDevice = true;
        worker.execute();
        dialog.setVisible(true);
        isWaitingOnDevice = true;
    }

    private boolean handleFocusLost(FocusEvent e) {
        if (device != null) {
            if (e.getSource() == maxVelocityDamperText) {
                return device.setMiscValue(Device.MISC_MAXVD, Short.parseShort(maxVelocityDamperText.getText()));
            } else if (e.getSource() == maxVelocityFrictionText) {
                return device.setMiscValue(Device.MISC_MAXVF, Short.parseShort(maxVelocityFrictionText.getText()));
            } else if (e.getSource() == maxVelocityInertiaText) {
                return device.setMiscValue(Device.MISC_MAXACC, Short.parseShort(maxVelocityInertiaText.getText()));
            } else if (e.getSource() == minForceText) {
                return device.setMiscValue(Device.MISC_MINF, Short.parseShort(minForceText.getText()));
            } else if (e.getSource() == maxForceText) {
                return device.setMiscValue(Device.MISC_MAXF, Short.parseShort(maxForceText.getText()));
            } else if (e.getSource() == cutForceText) {
                return device.setMiscValue(Device.MISC_CUTF, Short.parseShort(cutForceText.getText()));
            }
        }
        return true;
    }

    @Override
    public void focusLost(FocusEvent e) {
        boolean status = handleFocusLost(e);
        if (!status) {
            logger.warning("Focus lost, failed for:" + e.getSource());
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (device != null) {
            boolean status = true;
            if (e.getSource() == minForceSlider) {
                if (!minForceSlider.getValueIsAdjusting()) {
                    status = device.setMiscValue(Device.MISC_MINF, (short) minForceSlider.getValue());
                } else {
                    double percent = ((double) minForceSlider.getValue() / (double) minForceSlider.getMaximum()) * 100;
                    minForceLabel.setText(String.format("%.1f%%", percent));
                    minForceText.setValue(minForceSlider.getValue());
                }
            } else if (e.getSource() == maxForceSlider) {
                if (!maxForceSlider.getValueIsAdjusting()) {
                    status = device.setMiscValue(Device.MISC_MAXF, (short) maxForceSlider.getValue());
                } else {
                    double percent = ((double) maxForceSlider.getValue() / (double) maxForceSlider.getMaximum()) * 100;
                    maxForceLabel.setText(String.format("%.1f%%", percent));
                    maxForceText.setValue(maxForceSlider.getValue());
                }
            } else if (e.getSource() == cutForceSlider) {
                if (!cutForceSlider.getValueIsAdjusting()) {
                    status = device.setMiscValue(Device.MISC_CUTF, (short) cutForceSlider.getValue());
                } else {
                    double percent = ((double) cutForceSlider.getValue() / (double) cutForceSlider.getMaximum()) * 100;
                    cutForceLabel.setText(String.format("%.1f%%", percent));
                    cutForceText.setValue(cutForceSlider.getValue());
                }
            }
            if (!status) {
                logger.warning("State Changed, failed for:" + e.getSource());
            }
        }
    }

    private boolean doWheelAutoCenter() {
        final boolean[] status = {true};
        JLabel validator = new JLabel("<html><body>Please keep hands off wheel!<br>Press OK When AutoCentering is Complete.</body></html>");
        JOptionPane pane = new JOptionPane(validator, JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION);
        final JDialog dialog = pane.createDialog(autoCenterButton, "Please keep hands off wheel!");
        dialog.setModal(true);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            public Void doInBackground() {
                status[0] = device.doAutoCenter();
                return null;
            }
        };
        worker.execute();
        dialog.setVisible(true);
        if (status[0]) {
            return device.setWheelCenter();
        }
        return false;
    }

    @Override
    public void deviceAttached(Device device) {
        this.device = device;
        deviceComboBox.addItem(device.getName());
        device.doSetConstantSpring(false);
        setPanelEnabled(true);
        for (AxisPanel axisPanel : axisPanels) {
            axisPanel.deviceAttached(device);
        }
        for (GainPanel gainPanel : gainPanels) {
            gainPanel.deviceAttached(device);
        }
        buttonsPanel.deviceAttached(device);
    }

    @Override
    public void deviceDetached(Device device) {
        deviceComboBox.removeItem(device.getName());
        this.device = null;
        setPanelEnabled(false);
        for (AxisPanel axisPanel : axisPanels) {
            axisPanel.deviceDetached(device);
        }
        for (GainPanel gainPanel : gainPanels) {
            gainPanel.deviceDetached(device);
        }
        buttonsPanel.deviceDetached(device);
    }

    @Override
    public void deviceUpdated(Device device, String status, DataReport report) {
        if (status != null) {
            statusLabel.setText(status);
        }

        if (report != null) {
            if (report.getReportType() == Device.DATA_REPORT_ID) {
                switch (report) {
                    case WheelDataReport wheelData -> {
                        if (mainTab.getSelectedComponent() == axesTab) {
                            updateWheelPanel(wheelData);
                        }
                    }
                    case AxisDataReport axisData -> {
                        if (mainTab.getSelectedComponent() == axesTab) {
                            updateAxisPanel(axisPanels.get(axisData.getAxis() - 1), device, status, report);
                        }
                    }
                    case GainsDataReport gainsData -> {
                        if (mainTab.getSelectedComponent() == ffbTab) {
                            updateGainPanels(device, status, gainsData);
                        }
                    }
                    case MiscDataReport miscData -> {
                        if (mainTab.getSelectedComponent() == ffbTab) {
                            updateMiscPanel(miscData);
                        }
                    }
                    case ButtonsDataReport buttonsData -> {
                        if (mainTab.getSelectedComponent() == buttonsTab) {
                            buttonsPanel.deviceUpdated(device, status, buttonsData);
                        }
                    }
                    case VersionDataReport versionData ->
                            versionLabel.setText(versionData.getId() + ":" + versionData.getVersion());
                    default -> {
                    }
                }
            }
            if (isWaitingOnDevice) {
                isWaitingOnDevice = false;
            }
        }
    }

    private void updateMiscPanel(MiscDataReport miscData) {
        if (!maxVelocityDamperText.isFocusOwner()) {
            maxVelocityDamperText.setText(String.valueOf(miscData.getMaxVd()));
        }
        if (!maxVelocityFrictionText.isFocusOwner()) {
            maxVelocityFrictionText.setText(String.valueOf(miscData.getMaxVf()));
        }
        if (!maxVelocityInertiaText.isFocusOwner()) {
            maxVelocityInertiaText.setText(String.valueOf(miscData.getMaxAcc()));
        }

        if (!minForceSlider.getValueIsAdjusting()) {
            minForceSlider.setValue(miscData.getMinForce());
            double percent = ((double) minForceSlider.getValue() / (double) minForceSlider.getMaximum()) * 100;
            minForceLabel.setText(String.format("%.1f%%", percent));
        }
        if (!minForceSlider.getValueIsAdjusting() && !minForceText.isFocusOwner()) {
            minForceText.setValue(miscData.getMinForce());
        }

        if (!maxForceSlider.getValueIsAdjusting()) {
            maxForceSlider.setValue(miscData.getMaxForce());
            double percent = ((double) maxForceSlider.getValue() / (double) maxForceSlider.getMaximum()) * 100;
            maxForceLabel.setText(String.format("%.1f%%", percent));
        }
        if (!maxForceSlider.getValueIsAdjusting() && !maxForceText.isFocusOwner()) {
            maxForceText.setValue(miscData.getMaxForce());
        }

        if (!cutForceSlider.getValueIsAdjusting()) {
            cutForceSlider.setValue(miscData.getCutForce());
            double percent = ((double) cutForceSlider.getValue() / (double) cutForceSlider.getMaximum()) * 100;
            cutForceLabel.setText(String.format("%.1f%%", percent));
        }
        if (!cutForceSlider.getValueIsAdjusting() && !cutForceText.isFocusOwner()) {
            cutForceText.setValue(miscData.getCutForce());
        }

        if (!maxForceText.isFocusOwner()) {
            maxForceText.setText(String.valueOf(miscData.getMaxForce()));
        }
        if (!cutForceText.isFocusOwner()) {
            cutForceText.setText(String.valueOf(miscData.getCutForce()));
        }
        if (!frequencyCombo.isFocusOwner()) {
            int index = miscData.getFfbBitDepth() - 8;
            if (frequencyCombo.getSelectedIndex() != index) {
                frequencyCombo.setSelectedIndex(index);
            }
        }
    }

    private void updateWheelPanel(WheelDataReport wheelData) {
        if (Math.abs(wheelData.getAngle() - prevWheelRotation) > 0.2) {
            wheelIconLabel.setIcon(new ImageIcon(rotate(wheelImage, wheelData.getAngle())));
        }
        degreesLabel.setText(String.format("%.1f°", wheelData.getAngle()));
        prevWheelRotation = wheelData.getAngle();
        if (!rangeComboBox.isFocusOwner()) {
            String newRange = String.valueOf(wheelData.getRange());
            String oldRange = (String) rangeComboBox.getSelectedItem();
            if (!newRange.equals(oldRange)) {
                rangeComboBox.setSelectedItem(newRange);
            }
        }
        wheelRawTextField.setText(String.valueOf(wheelData.getRawValue()));
        wheelValueTextField.setText(String.valueOf(wheelData.getValue()));
        velocityText.setText(String.valueOf(wheelData.getVelocity()));
        accText.setText(String.valueOf(wheelData.getAcceleration()));
    }

    private void updateGainPanels(Device device, String status, GainsDataReport report) {
        for (GainPanel gainPanel : gainPanels) {
            gainPanel.deviceUpdated(device, status, report);
        }
    }

    private void updateAxisPanel(AxisPanel axisPanel, Device device, String status, DataReport report) {
        if (axisPanel != null) {
            axisPanel.deviceUpdated(device, status, report);
        }
    }

    public JComponent getRootComponent() {
        return mainPanel;
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setMinimumSize(new Dimension(1060, 400));
        mainPanel.setPreferredSize(new Dimension(1060, 800));
        mainTab = new JTabbedPane();
        mainTab.setMinimumSize(new Dimension(1060, 400));
        mainTab.setName("Inputs");
        mainTab.setPreferredSize(new Dimension(1060, 800));
        mainTab.setTabLayoutPolicy(0);
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(mainTab, gbc);
        axesTab = new JPanel();
        axesTab.setLayout(new BorderLayout(0, 0));
        axesTab.setMinimumSize(new Dimension(1060, 60));
        axesTab.setPreferredSize(new Dimension(1060, 800));
        mainTab.addTab("Axes", axesTab);
        axisScroll = new JScrollPane();
        axisScroll.setAutoscrolls(true);
        axisScroll.setMaximumSize(new Dimension(32767, 32767));
        axisScroll.setMinimumSize(new Dimension(1060, 60));
        axisScroll.setPreferredSize(new Dimension(1060, 590));
        axesTab.add(axisScroll, BorderLayout.WEST);
        axisPanel = new JPanel();
        axisPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        axisPanel.setAutoscrolls(true);
        axisPanel.setMaximumSize(new Dimension(32767, 32767));
        axisPanel.setMinimumSize(new Dimension(1040, 60));
        axisPanel.setPreferredSize(new Dimension(1040, 890));
        axisScroll.setViewportView(axisPanel);
        axisPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        wheelPanel = new JPanel();
        wheelPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        wheelPanel.setMinimumSize(new Dimension(1024, 82));
        wheelPanel.setPreferredSize(new Dimension(1024, 82));
        axisPanel.add(wheelPanel);
        wheelPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Axis 0 (X - Steering)", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        rangeLabel = new JLabel();
        rangeLabel.setText("Range");
        wheelPanel.add(rangeLabel);
        rangeComboBox = new JComboBox();
        rangeComboBox.setActionCommand("rangeChanged");
        rangeComboBox.setEditable(true);
        rangeComboBox.setMinimumSize(new Dimension(100, 30));
        rangeComboBox.setPreferredSize(new Dimension(100, 30));
        wheelPanel.add(rangeComboBox);
        centerButton = new JButton();
        centerButton.setPreferredSize(new Dimension(100, 30));
        centerButton.setText("Set Center");
        wheelPanel.add(centerButton);
        autoCenterButton = new JButton();
        autoCenterButton.setActionCommand("autoCenter");
        autoCenterButton.setText("AutoCenter");
        wheelPanel.add(autoCenterButton);
        wheelIconLabel = new JLabel();
        wheelIconLabel.setAlignmentY(0.0f);
        wheelIconLabel.setDoubleBuffered(true);
        wheelIconLabel.setFocusable(false);
        wheelIconLabel.setMinimumSize(new Dimension(55, 55));
        wheelIconLabel.setPreferredSize(new Dimension(55, 55));
        wheelIconLabel.setRequestFocusEnabled(false);
        wheelIconLabel.setText("");
        wheelIconLabel.setVerticalAlignment(1);
        wheelPanel.add(wheelIconLabel);
        degreesLabel = new JLabel();
        degreesLabel.setFocusable(false);
        degreesLabel.setHorizontalTextPosition(2);
        degreesLabel.setPreferredSize(new Dimension(50, 31));
        degreesLabel.setText("00.00°");
        wheelPanel.add(degreesLabel);
        wheelRawLabel = new JLabel();
        wheelRawLabel.setFocusable(false);
        wheelRawLabel.setText("Raw");
        wheelPanel.add(wheelRawLabel);
        wheelRawTextField = new JTextField();
        wheelRawTextField.setEditable(false);
        wheelRawTextField.setFocusable(false);
        wheelRawTextField.setMinimumSize(new Dimension(25, 30));
        wheelRawTextField.setPreferredSize(new Dimension(65, 30));
        wheelPanel.add(wheelRawTextField);
        wheelValueLabel = new JLabel();
        wheelValueLabel.setFocusable(false);
        wheelValueLabel.setText("Value");
        wheelPanel.add(wheelValueLabel);
        wheelValueTextField = new JTextField();
        wheelValueTextField.setEditable(false);
        wheelValueTextField.setFocusable(false);
        wheelValueTextField.setMinimumSize(new Dimension(25, 30));
        wheelValueTextField.setPreferredSize(new Dimension(65, 30));
        wheelPanel.add(wheelValueTextField);
        final JLabel label1 = new JLabel();
        label1.setFocusable(false);
        label1.setText("Velocity");
        wheelPanel.add(label1);
        velocityText = new JTextField();
        velocityText.setEditable(false);
        velocityText.setFocusable(false);
        velocityText.setMinimumSize(new Dimension(25, 30));
        velocityText.setPreferredSize(new Dimension(65, 30));
        wheelPanel.add(velocityText);
        accLabel = new JLabel();
        accLabel.setFocusable(false);
        accLabel.setText("Acc");
        wheelPanel.add(accLabel);
        accText = new JTextField();
        accText.setEditable(false);
        accText.setFocusable(false);
        accText.setPreferredSize(new Dimension(65, 30));
        wheelPanel.add(accText);
        axis1Panel = new AxisPanel();
        axisPanel.add(axis1Panel.$$$getRootComponent$$$());
        axis2Panel = new AxisPanel();
        axisPanel.add(axis2Panel.$$$getRootComponent$$$());
        axis3Panel = new AxisPanel();
        axisPanel.add(axis3Panel.$$$getRootComponent$$$());
        axis4Panel = new AxisPanel();
        axisPanel.add(axis4Panel.$$$getRootComponent$$$());
        axis5Panel = new AxisPanel();
        axisPanel.add(axis5Panel.$$$getRootComponent$$$());
        axis6Panel = new AxisPanel();
        axisPanel.add(axis6Panel.$$$getRootComponent$$$());
        axis7Panel = new AxisPanel();
        axisPanel.add(axis7Panel.$$$getRootComponent$$$());
        ffbTab = new JPanel();
        ffbTab.setLayout(new BorderLayout(0, 0));
        ffbTab.setAutoscrolls(false);
        ffbTab.setMinimumSize(new Dimension(1060, 60));
        ffbTab.setPreferredSize(new Dimension(1060, 800));
        mainTab.addTab("Force Feedback", ffbTab);
        ffbScroll = new JScrollPane();
        ffbScroll.setPreferredSize(new Dimension(1000, 800));
        ffbTab.add(ffbScroll, BorderLayout.CENTER);
        ffbSubPanel = new JPanel();
        ffbSubPanel.setLayout(new BorderLayout(0, 0));
        ffbSubPanel.setPreferredSize(new Dimension(1000, 800));
        ffbScroll.setViewportView(ffbSubPanel);
        gainsPanel = new JPanel();
        gainsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gainsPanel.setMinimumSize(new Dimension(570, 650));
        gainsPanel.setPreferredSize(new Dimension(575, 650));
        ffbSubPanel.add(gainsPanel, BorderLayout.WEST);
        gainsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Effect Gains", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        rightPanel = new JPanel();
        rightPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        rightPanel.setMinimumSize(new Dimension(520, 310));
        rightPanel.setPreferredSize(new Dimension(520, 400));
        ffbSubPanel.add(rightPanel, BorderLayout.CENTER);
        miscPanel = new JPanel();
        miscPanel.setLayout(new GridBagLayout());
        miscPanel.setMaximumSize(new Dimension(100, 400));
        miscPanel.setMinimumSize(new Dimension(475, 160));
        miscPanel.setPreferredSize(new Dimension(475, 275));
        rightPanel.add(miscPanel);
        miscPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Misc", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label2 = new JLabel();
        label2.setHorizontalAlignment(4);
        label2.setMinimumSize(new Dimension(100, 17));
        label2.setPreferredSize(new Dimension(100, 17));
        label2.setText("Min Force");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        miscPanel.add(label2, gbc);
        final JLabel label3 = new JLabel();
        label3.setHorizontalAlignment(4);
        label3.setMinimumSize(new Dimension(100, 17));
        label3.setPreferredSize(new Dimension(100, 17));
        label3.setText("Max Force");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.EAST;
        miscPanel.add(label3, gbc);
        final JLabel label4 = new JLabel();
        label4.setHorizontalAlignment(4);
        label4.setMinimumSize(new Dimension(100, 17));
        label4.setPreferredSize(new Dimension(100, 17));
        label4.setText("Cut Force");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.EAST;
        miscPanel.add(label4, gbc);
        minForceText = new JFormattedTextField();
        minForceText.setMinimumSize(new Dimension(75, 30));
        minForceText.setPreferredSize(new Dimension(75, 30));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        miscPanel.add(minForceText, gbc);
        maxForceText = new JFormattedTextField();
        maxForceText.setMinimumSize(new Dimension(75, 30));
        maxForceText.setPreferredSize(new Dimension(75, 30));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        miscPanel.add(maxForceText, gbc);
        cutForceText = new JFormattedTextField();
        cutForceText.setMinimumSize(new Dimension(75, 30));
        cutForceText.setPreferredSize(new Dimension(75, 30));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 5;
        miscPanel.add(cutForceText, gbc);
        minForceLabel = new JLabel();
        minForceLabel.setText("0%");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        miscPanel.add(minForceLabel, gbc);
        maxForceLabel = new JLabel();
        maxForceLabel.setText("100%");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        miscPanel.add(maxForceLabel, gbc);
        cutForceLabel = new JLabel();
        cutForceLabel.setText("100%");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        miscPanel.add(cutForceLabel, gbc);
        minForceSlider = new JSlider();
        minForceSlider.setMajorTickSpacing(1024);
        minForceSlider.setMaximum(16383);
        minForceSlider.setMinimumSize(new Dimension(140, 24));
        minForceSlider.setMinorTickSpacing(0);
        minForceSlider.setPaintTicks(true);
        minForceSlider.setPaintTrack(true);
        minForceSlider.setPreferredSize(new Dimension(140, 24));
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        miscPanel.add(minForceSlider, gbc);
        maxForceSlider = new JSlider();
        maxForceSlider.setMajorTickSpacing(1024);
        maxForceSlider.setMaximum(16383);
        maxForceSlider.setMinimumSize(new Dimension(140, 24));
        maxForceSlider.setMinorTickSpacing(0);
        maxForceSlider.setPaintTicks(true);
        maxForceSlider.setPaintTrack(true);
        maxForceSlider.setPreferredSize(new Dimension(140, 24));
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        miscPanel.add(maxForceSlider, gbc);
        cutForceSlider = new JSlider();
        cutForceSlider.setMajorTickSpacing(1024);
        cutForceSlider.setMaximum(16383);
        cutForceSlider.setMinimumSize(new Dimension(140, 24));
        cutForceSlider.setMinorTickSpacing(0);
        cutForceSlider.setPaintTicks(true);
        cutForceSlider.setPaintTrack(true);
        cutForceSlider.setPreferredSize(new Dimension(140, 24));
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        miscPanel.add(cutForceSlider, gbc);
        final JLabel label5 = new JLabel();
        label5.setText("PWM bitdepth/frequency");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.EAST;
        miscPanel.add(label5, gbc);
        frequencyCombo = new JComboBox();
        frequencyCombo.setActionCommand("changeFfbBd");
        frequencyCombo.setPreferredSize(new Dimension(100, 30));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        miscPanel.add(frequencyCombo, gbc);
        final JLabel label6 = new JLabel();
        label6.setHorizontalAlignment(4);
        label6.setMinimumSize(new Dimension(170, 17));
        label6.setPreferredSize(new Dimension(170, 17));
        label6.setText("Max Velocity for Damper");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        miscPanel.add(label6, gbc);
        final JLabel label7 = new JLabel();
        label7.setHorizontalAlignment(4);
        label7.setMinimumSize(new Dimension(170, 17));
        label7.setPreferredSize(new Dimension(170, 17));
        label7.setText("Max Velocity for Friction");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        miscPanel.add(label7, gbc);
        final JLabel label8 = new JLabel();
        label8.setHorizontalAlignment(4);
        label8.setMinimumSize(new Dimension(170, 17));
        label8.setPreferredSize(new Dimension(170, 17));
        label8.setText("Max Velocity for Inertia");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        miscPanel.add(label8, gbc);
        maxVelocityFrictionText = new JFormattedTextField();
        maxVelocityFrictionText.setMinimumSize(new Dimension(75, 30));
        maxVelocityFrictionText.setPreferredSize(new Dimension(75, 30));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        miscPanel.add(maxVelocityFrictionText, gbc);
        maxVelocityInertiaText = new JFormattedTextField();
        maxVelocityInertiaText.setMinimumSize(new Dimension(75, 30));
        maxVelocityInertiaText.setPreferredSize(new Dimension(75, 30));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        miscPanel.add(maxVelocityInertiaText, gbc);
        maxVelocityDamperText = new JFormattedTextField();
        maxVelocityDamperText.setMinimumSize(new Dimension(75, 30));
        maxVelocityDamperText.setPreferredSize(new Dimension(75, 30));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        miscPanel.add(maxVelocityDamperText, gbc);
        constantSpringCheckBox = new JCheckBox();
        constantSpringCheckBox.setHorizontalAlignment(0);
        constantSpringCheckBox.setLabel("Constant Spring");
        constantSpringCheckBox.setText("Constant Spring");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 7;
        miscPanel.add(constantSpringCheckBox, gbc);
        testPanel = new JPanel();
        testPanel.setLayout(new GridBagLayout());
        testPanel.setMaximumSize(new Dimension(400, 400));
        testPanel.setMinimumSize(new Dimension(475, 220));
        testPanel.setPreferredSize(new Dimension(475, 220));
        rightPanel.add(testPanel);
        testPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Test Effects", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        constantLeftButton = new JButton();
        constantLeftButton.setMaximumSize(new Dimension(153, 30));
        constantLeftButton.setMinimumSize(new Dimension(153, 30));
        constantLeftButton.setPreferredSize(new Dimension(153, 30));
        constantLeftButton.setText("Constant Pull Left");
        constantLeftButton.setVerticalAlignment(0);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        testPanel.add(constantLeftButton, gbc);
        sineButton = new JButton();
        sineButton.setMaximumSize(new Dimension(153, 30));
        sineButton.setMinimumSize(new Dimension(153, 30));
        sineButton.setPreferredSize(new Dimension(153, 30));
        sineButton.setText("Sine");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        testPanel.add(sineButton, gbc);
        springButton = new JButton();
        springButton.setMaximumSize(new Dimension(153, 30));
        springButton.setMinimumSize(new Dimension(153, 30));
        springButton.setPreferredSize(new Dimension(153, 30));
        springButton.setText("Spring");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        testPanel.add(springButton, gbc);
        frictionButton = new JButton();
        frictionButton.setMaximumSize(new Dimension(153, 30));
        frictionButton.setMinimumSize(new Dimension(153, 30));
        frictionButton.setPreferredSize(new Dimension(153, 30));
        frictionButton.setText("Friction");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        testPanel.add(frictionButton, gbc);
        rampButton = new JButton();
        rampButton.setMaximumSize(new Dimension(153, 30));
        rampButton.setMinimumSize(new Dimension(153, 30));
        rampButton.setPreferredSize(new Dimension(153, 30));
        rampButton.setText("Ramp");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        testPanel.add(rampButton, gbc);
        sawtoothUpButton = new JButton();
        sawtoothUpButton.setMaximumSize(new Dimension(153, 30));
        sawtoothUpButton.setMinimumSize(new Dimension(153, 30));
        sawtoothUpButton.setPreferredSize(new Dimension(153, 30));
        sawtoothUpButton.setText("Sawtooth Up");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        testPanel.add(sawtoothUpButton, gbc);
        sawtoothDownButton = new JButton();
        sawtoothDownButton.setMaximumSize(new Dimension(153, 30));
        sawtoothDownButton.setMinimumSize(new Dimension(153, 30));
        sawtoothDownButton.setPreferredSize(new Dimension(153, 30));
        sawtoothDownButton.setText("Sawtooth Down");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        testPanel.add(sawtoothDownButton, gbc);
        inertiaButton = new JButton();
        inertiaButton.setMaximumSize(new Dimension(153, 30));
        inertiaButton.setMinimumSize(new Dimension(153, 30));
        inertiaButton.setPreferredSize(new Dimension(153, 30));
        inertiaButton.setText("Inertia");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        testPanel.add(inertiaButton, gbc);
        damperButton = new JButton();
        damperButton.setMaximumSize(new Dimension(153, 30));
        damperButton.setMinimumSize(new Dimension(153, 30));
        damperButton.setPreferredSize(new Dimension(153, 30));
        damperButton.setText("Damper");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        testPanel.add(damperButton, gbc);
        triangleButton = new JButton();
        triangleButton.setMaximumSize(new Dimension(153, 30));
        triangleButton.setMinimumSize(new Dimension(153, 30));
        triangleButton.setPreferredSize(new Dimension(153, 30));
        triangleButton.setText("Triangle");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        testPanel.add(triangleButton, gbc);
        constantRightButton = new JButton();
        constantRightButton.setMaximumSize(new Dimension(153, 30));
        constantRightButton.setMinimumSize(new Dimension(153, 30));
        constantRightButton.setPreferredSize(new Dimension(153, 30));
        constantRightButton.setText("Constant Pull Right");
        constantRightButton.setVerticalAlignment(0);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        testPanel.add(constantRightButton, gbc);
        buttonsTab = new JPanel();
        buttonsTab.setLayout(new BorderLayout(0, 0));
        mainTab.addTab("Buttons", buttonsTab);
        buttonsPanel = new ButtonsPanel();
        buttonsTab.add(buttonsPanel.$$$getRootComponent$$$(), BorderLayout.WEST);
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        bottomPanel.setAutoscrolls(false);
        bottomPanel.setMinimumSize(new Dimension(1060, 75));
        bottomPanel.setPreferredSize(new Dimension(1060, 75));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(bottomPanel, gbc);
        bottomPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        deviceComboBox = new JComboBox();
        deviceComboBox.setEditable(false);
        deviceComboBox.setMinimumSize(new Dimension(200, 30));
        deviceComboBox.setPreferredSize(new Dimension(200, 30));
        bottomPanel.add(deviceComboBox);
        versionLabel = new JLabel();
        versionLabel.setFocusable(false);
        versionLabel.setPreferredSize(new Dimension(80, 17));
        versionLabel.setText("Version");
        bottomPanel.add(versionLabel);
        statusLabel = new JLabel();
        statusLabel.setFocusable(false);
        statusLabel.setMinimumSize(new Dimension(130, 20));
        statusLabel.setPreferredSize(new Dimension(130, 20));
        statusLabel.setRequestFocusEnabled(true);
        statusLabel.setText("Device Not Found...");
        bottomPanel.add(statusLabel);
        defaultsButton = new JButton();
        defaultsButton.setActionCommand("resetDefaults");
        defaultsButton.setMinimumSize(new Dimension(201, 30));
        defaultsButton.setPreferredSize(new Dimension(201, 30));
        defaultsButton.setText("Reset Settings to Defaults");
        bottomPanel.add(defaultsButton);
        loadButton = new JButton();
        loadButton.setActionCommand("loadEEPROM");
        loadButton.setText("Load Settings From EEPROM");
        bottomPanel.add(loadButton);
        saveButton = new JButton();
        saveButton.setActionCommand("saveSettings");
        saveButton.setHorizontalAlignment(0);
        saveButton.setMinimumSize(new Dimension(201, 30));
        saveButton.setPreferredSize(new Dimension(201, 30));
        saveButton.setText("Save Settings to EEPROM");
        bottomPanel.add(saveButton);
        wheelRawLabel.setLabelFor(velocityText);
        wheelValueLabel.setLabelFor(velocityText);
        label1.setLabelFor(velocityText);
        accLabel.setLabelFor(accText);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
