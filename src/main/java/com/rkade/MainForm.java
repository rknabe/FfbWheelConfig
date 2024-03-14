package com.rkade;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class MainForm implements DeviceListener, ActionListener {
    private final static Logger logger = Logger.getLogger(MainForm.class.getName());
    private final static String CENTER_BUTTON = "Set Center";
    private final static String SINE_FFB_BUTTON = "doSineFfb";
    private final List<AxisPanel> axisPanels = new ArrayList<>(7);
    private final List<String> axisLabels = List.of(
            "Axis 1 (Y - Accelerator)",
            "Axis 2 (Z - Brake)",
            "Axis 3 (rX - Clutch)",
            "Axis 4 (rY - Aux 1)",
            "Axis 5 (rZ - Aux 2)",
            "Axis 6 (Slider - Aux 3)",
            "Axis 7 (Dial - Aux 4)");
    private JPanel mainPanel;
    private JTabbedPane Inputs;
    private JPanel ffbPanel;
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
    private JPanel inputsPanel;
    private JButton sineFfbButton;
    private BufferedImage wheelImage;
    private double prevWheelRotation = 0.0;
    private Device device = null;

    public MainForm() {
        centerButton.addActionListener(this);
        try {
            ImageIcon imageIcon = new ImageIcon(ClassLoader.getSystemResource("wheel55.png"));
            wheelImage = toBufferedImage(imageIcon.getImage());
            wheelIconLabel.setIcon(imageIcon);
        } catch (Exception ex) {
            logger.warning(ex.getMessage());
        }
        axisPanels.add(axis1Panel);
        axisPanels.add(axis2Panel);
        axisPanels.add(axis3Panel);
        axisPanels.add(axis4Panel);
        axisPanels.add(axis5Panel);
        axisPanels.add(axis6Panel);
        axisPanels.add(axis7Panel);

        sineFfbButton.addActionListener(this);

        setAxisTitles();
    }

    private void setAxisTitles() {
        for (int i = 0; i < axisPanels.size(); i++) {
            AxisPanel panel = axisPanels.get(i);
            if (panel != null) {
                TitledBorder border = (TitledBorder) panel.getMainPanel().getBorder();
                border.setTitle(axisLabels.get(i));
            }
        }
    }

    private BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(55, 55, BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    private BufferedImage rotate(BufferedImage image, Double degrees) {
        // Calculate the new size of the image based on the angle of rotation
        double radians = Math.toRadians(degrees);
        int newWidth = image.getWidth();
        int newHeight = image.getHeight();

        // Create a new image
        BufferedImage rotate = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotate.createGraphics();
        // Calculate the "anchor" point around which the image will be rotated
        int x = (newWidth - image.getWidth()) / 2;
        int y = (newHeight - image.getHeight()) / 2;
        // Transform the origin point around the anchor point
        AffineTransform at = new AffineTransform();
        at.setToRotation(radians, x + (image.getWidth() / 2.0), y + (image.getHeight() / 2.0));
        at.translate(x, y);
        g2d.setTransform(at);
        // Paint the original image
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return rotate;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (CENTER_BUTTON.equals(e.getActionCommand())) {

        } else if (SINE_FFB_BUTTON.equalsIgnoreCase(e.getActionCommand())) {
            boolean status = device.doFfbSine();
        }
    }

    @Override
    public void deviceAttached(Device device) {
        this.device = device;
        deviceComboBox.addItem(device.getName());
    }

    @Override
    public void deviceDetached(Device device) {
        deviceComboBox.removeItem(device.getName());
        this.device = null;
    }

    @Override
    public void deviceUpdated(Device device, String status, DataReport report) {
        if (status != null) {
            statusLabel.setText(status);
        }

        if (report != null) {
            if (report.getReportType() == DataReport.DATA_REPORT_ID) {
                switch (report) {
                    case WheelDataReport wheelData -> updateWheelPanel(wheelData);
                    case AxisDataReport axisData -> updateAxisPanel(axisPanels.get(axisData.getAxis() - 1), axisData);
                    case VersionDataReport versionData ->
                            versionLabel.setText(versionData.getId() + ":" + versionData.getVersion());
                    default -> {
                    }
                }
            }
        }
    }

    private void updateWheelPanel(WheelDataReport wheelData) {
        if (Math.abs(wheelData.getAngle() - prevWheelRotation) > 0.2) {
            wheelIconLabel.setIcon(new ImageIcon(rotate(wheelImage, wheelData.getAngle())));
        }
        degreesLabel.setText(String.format("%.1f°", wheelData.getAngle()));
        prevWheelRotation = wheelData.getAngle();
        String newRange = String.valueOf(wheelData.getRange());
        String oldRange = (String) rangeComboBox.getSelectedItem();
        if (!newRange.equals(oldRange)) {
            rangeComboBox.removeAllItems();
            rangeComboBox.addItem(newRange);
        }
        wheelRawTextField.setText(String.valueOf(wheelData.getRawValue()));
        wheelValueTextField.setText(String.valueOf(wheelData.getValue()));
        velocityText.setText(String.valueOf(wheelData.getVelocity()));
        accText.setText(String.valueOf(wheelData.getAcceleration()));
    }

    private void updateAxisPanel(AxisPanel axisPanel, AxisDataReport axisData) {
        if (axisPanel != null) {
            axisPanel.getEnabledCheckBox().setSelected(axisData.isEnabled());
            axisPanel.getHasCenterCheckBox().setSelected(axisData.isHasCenter());
            axisPanel.getAutoLimitCheckBox().setSelected(axisData.isAutoLimit());
            if (axisData.isEnabled()) {
                axisPanel.getProgress().setValue(axisData.getRawValue());
                axisPanel.getProgress().setMaximum(axisData.getMax());
                axisPanel.getProgress().setMinimum(axisData.getMin());
                axisPanel.getValueText().setText(String.valueOf(axisData.getValue()));
                axisPanel.getRawText().setText(String.valueOf(axisData.getRawValue()));
                axisPanel.getMinText().setText(String.valueOf(axisData.getMin()));
                axisPanel.getMaxText().setText(String.valueOf(axisData.getMax()));
                axisPanel.getCenterText().setText(String.valueOf(axisData.getCenter()));
                axisPanel.getDzText().setText(String.valueOf(axisData.getDeadZone()));
            }
        }
    }

    public JComponent getRootComponent() {
        return mainPanel;
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ex) {
            logger.warning(ex.getMessage());
        }
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
        mainPanel.setPreferredSize(new Dimension(1060, 700));
        Inputs = new JTabbedPane();
        Inputs.setMinimumSize(new Dimension(1060, 400));
        Inputs.setPreferredSize(new Dimension(1060, 700));
        Inputs.setTabLayoutPolicy(0);
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(Inputs, gbc);
        inputsPanel = new JPanel();
        inputsPanel.setLayout(new BorderLayout(0, 0));
        inputsPanel.setMinimumSize(new Dimension(1060, 60));
        inputsPanel.setPreferredSize(new Dimension(1060, 700));
        Inputs.addTab("Inputs", inputsPanel);
        axisScroll = new JScrollPane();
        axisScroll.setAutoscrolls(true);
        axisScroll.setMaximumSize(new Dimension(32767, 32767));
        axisScroll.setMinimumSize(new Dimension(1060, 60));
        axisScroll.setPreferredSize(new Dimension(1060, 590));
        inputsPanel.add(axisScroll, BorderLayout.WEST);
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
        wheelPanel.add(rangeComboBox);
        centerButton = new JButton();
        centerButton.setPreferredSize(new Dimension(100, 30));
        centerButton.setText("Set Center");
        wheelPanel.add(centerButton);
        wheelIconLabel = new JLabel();
        wheelIconLabel.setAlignmentY(0.0f);
        wheelIconLabel.setPreferredSize(new Dimension(55, 55));
        wheelIconLabel.setRequestFocusEnabled(false);
        wheelIconLabel.setText("");
        wheelIconLabel.setVerticalAlignment(1);
        wheelPanel.add(wheelIconLabel);
        degreesLabel = new JLabel();
        degreesLabel.setHorizontalTextPosition(2);
        degreesLabel.setPreferredSize(new Dimension(50, 31));
        degreesLabel.setText("00.00°");
        wheelPanel.add(degreesLabel);
        wheelRawLabel = new JLabel();
        wheelRawLabel.setText("Raw");
        wheelPanel.add(wheelRawLabel);
        wheelRawTextField = new JTextField();
        wheelRawTextField.setMinimumSize(new Dimension(25, 30));
        wheelRawTextField.setPreferredSize(new Dimension(65, 30));
        wheelPanel.add(wheelRawTextField);
        wheelValueLabel = new JLabel();
        wheelValueLabel.setText("Value");
        wheelPanel.add(wheelValueLabel);
        wheelValueTextField = new JTextField();
        wheelValueTextField.setMinimumSize(new Dimension(25, 30));
        wheelValueTextField.setPreferredSize(new Dimension(65, 30));
        wheelPanel.add(wheelValueTextField);
        final JLabel label1 = new JLabel();
        label1.setText("Velocity");
        wheelPanel.add(label1);
        velocityText = new JTextField();
        velocityText.setMinimumSize(new Dimension(25, 30));
        velocityText.setPreferredSize(new Dimension(65, 30));
        wheelPanel.add(velocityText);
        accLabel = new JLabel();
        accLabel.setText("Acc");
        wheelPanel.add(accLabel);
        accText = new JTextField();
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
        ffbPanel = new JPanel();
        ffbPanel.setLayout(new GridBagLayout());
        ffbPanel.setAutoscrolls(true);
        ffbPanel.setMinimumSize(new Dimension(1024, 768));
        ffbPanel.setPreferredSize(new Dimension(1024, 768));
        Inputs.addTab("Force Feedback", ffbPanel);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ffbPanel.add(spacer1, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        ffbPanel.add(spacer2, gbc);
        sineFfbButton = new JButton();
        sineFfbButton.setActionCommand("doSineFfb");
        sineFfbButton.setText("Sine");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ffbPanel.add(sineFfbButton, gbc);
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        bottomPanel.setAutoscrolls(true);
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
        versionLabel.setPreferredSize(new Dimension(80, 17));
        versionLabel.setText("Version");
        bottomPanel.add(versionLabel);
        statusLabel = new JLabel();
        statusLabel.setPreferredSize(new Dimension(120, 17));
        statusLabel.setText("Device Not Found");
        bottomPanel.add(statusLabel);
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
