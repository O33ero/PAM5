import com.mindfusion.charting.GridType;
import com.mindfusion.charting.Series2D;
import com.mindfusion.charting.swing.LineChart;
import com.mindfusion.drawing.DashStyle;
import com.mindfusion.drawing.SolidBrush;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class MainWindow extends JFrame {
    private static final Logger LOGGER = LogManager.getLogger(MainWindow.class);

    // Functional
    // timer
    Timer timer;
    boolean enableTimeEvent;
    int millsTimer = 1000 / 2;
    // random generator
    RandomGenerator randomGenerator;
    boolean enableRandomEvent;

    // Buttons
    private JPanel jButtonPanel01;
    private JMenuBar jMenuBar;

    // Chart drawing
    private LineChart chart;
    private final int cacheSize = 512;
    private double time;
    private List<Double> mainSeriesXData;
    private List<Double> mainSeriesYData;
    private List<Double> decodedSeriesXData;
    private List<Double> decodedSeriesYData;
    private List<String> decodedSeriesNames;
    private Series2D mainSeries;
    private Series2D decodedSeries;

    // Chart layout
    private final double yMaxValue = 3.0;
    private final double yMinValue = -3.5;
    private final double xMaxValue = 20.0; // may be changed
    private final double xMinValue = 0.0; // may be changed

    private int lastInput = -1; // if == -1 -> no input
    // if == 1 or 0 -> rerender chart

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MainWindow().setVisible(true);
            } catch (Exception ex) {
                LOGGER.error("Something went wrong: {}", ex.getMessage(), ex);
            }
        });
    }

    protected MainWindow() {
        // Main init
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setTitle("PAM5 Implementation");
        setFocusable(true);
        // Menu bar
        setJMenuBar(initMenuBar());

        // Chart
        getContentPane().add(initChart(), BorderLayout.CENTER);

        // Button 0 and 1
        getContentPane().add(initPanel01(), BorderLayout.SOUTH);

        // key binding
        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                int keyValue = -1;
                LOGGER.debug("Pressed: {}", e.getKeyChar());
                if (e.getKeyChar() == '1') {
                    inputAction(1);
                    LOGGER.debug("Input: 1");
                } else if (e.getKeyChar() == '0' || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    inputAction(0);
                    LOGGER.debug("Input: 0");
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        // Time tick events
        randomGenerator = RandomGeneratorFactory.of("Xoroshiro128PlusPlus").create();
        timer = new Timer(millsTimer, e -> {
            int value = -1;
            if (enableTimeEvent) {
                if (enableRandomEvent) {
                    value = randomGenerator.nextInt(0, 2);
                    value = value * 10 + randomGenerator.nextInt(0, 2);
                    value = randomGenerator.nextInt(0, 20) == 1 ? -1 : value;
                }
                LOGGER.info("Generated next value: {}", value);
                nextPoint(value);
                updateSeries();
            }
        });
    }

    private LineChart initChart() {
        chart = new LineChart();
        chart.setName("PAM5");

        // Axis
        chart.getYAxis().setMaxValue(yMaxValue);
        chart.getYAxis().setMinValue(yMinValue);
        chart.getYAxis().setOrigin(0.0);
        chart.getYAxis().setInterval(1.0);
        chart.getYAxis().setTitle("V, В");

        chart.getXAxis().setMaxValue(xMaxValue);
        chart.getXAxis().setMinValue(xMinValue);
        chart.getXAxis().setOrigin(0.0);
        chart.getXAxis().setInterval(1.0);
        chart.getXAxis().setTitle("T, сек");

        // Grid
        chart.setGridType(GridType.Vertical);
        chart.getTheme().setGridLineStyle(DashStyle.Dash);
        chart.getTheme().setGridLineColor(new Color(186, 186, 186));

        // Series
        chart.getTheme().setHighlightStroke(new SolidBrush(new Color(255, 147, 66)));
        chart.getTheme().setCommonSeriesStrokes(
                Arrays.asList(
                        new SolidBrush(new Color(206, 0, 0)),
                        new SolidBrush(new Color(255, 255, 255))
                )
        );
        chart.getTheme().setCommonSeriesFills(
                Arrays.asList(
                        new SolidBrush(new Color(206, 0, 0)),
                        new SolidBrush(new Color(0, 106, 0))
                )
        );
        chart.getTheme().setCommonSeriesStrokeThicknesses(
                Arrays.asList(5.0, 0.0)
        );
        chart.getTheme().setCommonSeriesStrokeDashStyles(
                Arrays.asList(DashStyle.Solid, DashStyle.Solid)
        );
        chart.getTheme().setDataLabelsFontSize(20);

        chart.setShowLegend(false);
        // Series
        try {
            mainSeriesYData = generateSeries(cacheSize, 0.0);
            mainSeriesXData = generateSeries(cacheSize, 0.0);
            decodedSeriesXData = generateSeries(cacheSize, 0.0);
            decodedSeriesYData = generateSeries(cacheSize, -3.0);

            LOGGER.debug("Dataset X = {}", mainSeriesXData);
            LOGGER.debug("Dataset Y = {}", mainSeriesYData);

            mainSeries = new Series2D(
                    mainSeriesXData,
                    mainSeriesYData,
                    new LinkedList<>(Collections.nCopies(cacheSize, " ")));
            mainSeries.setTitle("PAM5");

            decodedSeriesNames = new LinkedList<>(Collections.nCopies(cacheSize, " "));
            decodedSeries = new Series2D(
                    decodedSeriesXData,
                    decodedSeriesYData,
                    decodedSeriesNames
            );
            decodedSeries.setTitle("Decoded PAM5");

            time = 0;

            chart.getSeries().add(mainSeries);
            chart.getSeries().add(decodedSeries);
        } catch (Exception ex) {
            LOGGER.error("Something wend wrong: {}", ex.getMessage(), ex);
            System.exit(1);
        }

        // Return value (may be null)
        return chart;
    }


    private List<Double> generateSeries(int size, double startValue) {
        return new LinkedList<>(Collections.nCopies(size, startValue));
    }

    private List<String> generateNames(int size) {
        List<String> data = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            data.add("P" + i);
        }
        return data;
    }

    private JPanel initPanel01() {
        jButtonPanel01 = new JPanel();
        jButtonPanel01.setName("Input buttons");
        jButtonPanel01.add(initButton("0"));
        jButtonPanel01.add(initButton("1"));

        return jButtonPanel01;
    }


    private JMenuBar initMenuBar() {
        jMenuBar = new JMenuBar();

        // Options
        JMenu options = new JMenu("Options");
        JCheckBoxMenuItem timerEvent = new JCheckBoxMenuItem("Включаить автозаполение по таймеру");
        timerEvent.addActionListener(e -> {
            AbstractButton aButton = (AbstractButton) e.getSource();
            enableTimeEvent = aButton.isSelected();
            LOGGER.debug("enableTimeEvent switched to {}", enableTimeEvent);
            if (enableTimeEvent) {
                timer.start();
            } else {
                timer.stop();
            }
        });

        JCheckBoxMenuItem randomGeneratorEvent = new JCheckBoxMenuItem("Заполнять случайными значениями по таймеру");
        randomGeneratorEvent.addActionListener(e -> {
            AbstractButton aButton = (AbstractButton) e.getSource();
            enableRandomEvent = aButton.isSelected();
            LOGGER.debug("enableRandomEvent switched to {}", enableRandomEvent);
        });
        options.add(timerEvent);
        options.add(randomGeneratorEvent);

        jMenuBar.add(options);
        return jMenuBar;
    }

    private JButton initButton(String content) {
        int buttonValue = Integer.parseInt(content);

        JButton button = new JButton();
        button.setText(content);
        button.setSize(20, 30);
        button.addActionListener(e -> inputAction(buttonValue));

        return button;
    }

    private void nextPoint(int b) {
        mainSeriesXData.remove(0);
        mainSeriesYData.remove(0);
        mainSeriesXData.remove(0);
        mainSeriesYData.remove(0);
        decodedSeriesXData.remove(0);
        decodedSeriesNames.remove(0);

        // 00
        if (b == 0) {
            mainSeriesXData.add(time);
            mainSeriesYData.add(-2.0);
            time += 1.0;
            mainSeriesXData.add(time);
            mainSeriesYData.add(-2.0);
            LOGGER.debug("New chart update to 00 (-2)");

            decodedSeriesXData.add(time - 0.5);
            decodedSeriesNames.add("0  0");
            return;
        }
        // 01
        if (b == 1) {
            mainSeriesXData.add(time);
            mainSeriesYData.add(-1.0);
            time += 1.0;
            mainSeriesXData.add(time);
            mainSeriesYData.add(-1.0);
            LOGGER.debug("New chart update to 01 (-1)");

            decodedSeriesXData.add(time - 0.5);
            decodedSeriesNames.add("0  1");
            return;
        }
        // 10
        if (b == 10) {
            mainSeriesXData.add(time);
            mainSeriesYData.add(1.0);
            time += 1.0;
            mainSeriesXData.add(time);
            mainSeriesYData.add(1.0);
            LOGGER.debug("New chart update to 10 (1)");

            decodedSeriesXData.add(time - 0.5);
            decodedSeriesNames.add("1  0");
            return;
        }
        // 11
        if (b == 11) {
            mainSeriesXData.add(time);
            mainSeriesYData.add(2.0);
            time += 1.0;
            mainSeriesXData.add(time);
            mainSeriesYData.add(2.0);
            LOGGER.debug("New chart update to 11 (2)");

            decodedSeriesXData.add(time - 0.5);
            decodedSeriesNames.add("1  1");
            return;
        }
        // base value
        if (b == -1) {
            mainSeriesXData.add(time);
            mainSeriesYData.add(0.0);
            time += 1;
            mainSeriesXData.add(time);
            mainSeriesYData.add(0.0);

            decodedSeriesXData.add(time - 0.5);
            decodedSeriesNames.add(" ");
            LOGGER.debug("New chart update to -1 (0)");
        }
    }


    private void updateSeries() {
        lastInput = -1;

//        mainSeries.setXData(mainSeriesXData);
//        mainSeries.setYData(mainSeriesYData);
//        chart.getSeries().add(mainSeries);

        double maxXValue = mainSeriesXData.get(cacheSize - 1);
        if (maxXValue > chart.getXAxis().getMaxValue() - 2.0) {
            chart.getXAxis().setMaxValue(maxXValue + 2.0);
            chart.getXAxis().setMinValue(maxXValue - 18.0);
        }
        chart.repaint();
    }

    private void inputAction(int inputValue) {
        if (lastInput == -1) {
            lastInput = inputValue;
            LOGGER.debug("Last Input = {}", lastInput);
        } else {
            if (lastInput == 0) {
                // 01
                if (inputValue == 1) {
                    nextPoint(1);
                }
                // 00
                else {
                    nextPoint(0);
                }
            } else {
                // 11
                if (inputValue == 1) {
                    nextPoint(11);
                }
                // 10
                else {
                    nextPoint(10);
                }
            }
            timer.restart();
            LOGGER.debug("New dataset X = {}", mainSeriesXData);
            LOGGER.debug("New dataset Y = {}", mainSeriesYData);
            updateSeries();
        }
    }
}
