import com.mindfusion.charting.swing.LineChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class MainWindow extends JFrame {
    private static final Logger LOGGER = LogManager.getLogger(MainWindow.class);

    // Functional
    // timer
    private final Timer timer;
    private boolean enableTimeEvent;
    private final int millsTimer = 1000 / 2;
    // random generator
    private final transient RandomGenerator randomGenerator;
    private boolean enableRandomEvent;

    // Components
    private JPanel jButtonPanel01;
    private JMenuBar jMenuBar;
    private AMIChart chart;

    // Input functional
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
        setTitle("AMI Implementation");
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
                // Not used
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
                // Not used
            }
        });

        // Time tick events
        randomGenerator = RandomGeneratorFactory.of("Xoroshiro128PlusPlus").create();
        timer = new Timer(millsTimer, e -> {
            int value = 0;
            if (enableTimeEvent) {
                if (enableRandomEvent) {
                    value = randomGenerator.nextInt(0, 2);
                }
                LOGGER.info("Generated next value: {}", value);
                chart.nextPoint(value);
                lastInput = -1;
            }
        });
    }

    private LineChart initChart() {
        chart = new AMIChart();
        try {
            chart.initSeries();
        } catch (Exception ex) {
            LOGGER.error("Something wend wrong: {}", ex.getMessage(), ex);
            System.exit(1);
        }
        return chart;
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

    private void inputAction(int inputValue) {
        // 0 or 1
        chart.nextPoint(inputValue);

        timer.restart();
        LOGGER.debug("New dataset X = {}", chart.getMainSeriesXData());
        LOGGER.debug("New dataset Y = {}", chart.getMainSeriesYData());
        chart.updateSeries();
        lastInput = -1;
    }
}
