import com.mindfusion.charting.GridType;
import com.mindfusion.charting.Series2D;
import com.mindfusion.charting.swing.LineChart;
import com.mindfusion.drawing.DashStyle;
import com.mindfusion.drawing.SolidBrush;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PAM5Chart extends LineChart {
    private static final Logger LOGGER = LogManager.getLogger(PAM5Chart.class);
    private static final String NAME = "PAM5";

    private final int cacheSize = 512;
    private double time;
    private List<Double> mainSeriesXData;
    private List<Double> mainSeriesYData;
    private List<Double> decodedSeriesXData;
    private List<Double> decodedSeriesYData;
    private List<String> decodedSeriesNames;
    private Series2D mainSeries;
    private Series2D decodedSeries;

    private final double yMaxValue = 3.0;
    private final double yMinValue = -3.5;
    private final double xMaxValue = 20.0; // may be changed
    private final double xMinValue = 0.0; // may be changed

    PAM5Chart() {
        setName(NAME);

        // Axis
        getYAxis().setMaxValue(yMaxValue);
        getYAxis().setMinValue(yMinValue);
        getYAxis().setOrigin(0.0);
        getYAxis().setInterval(1.0);
        getYAxis().setTitle("V, В");

        getXAxis().setMaxValue(xMaxValue);
        getXAxis().setMinValue(xMinValue);
        getXAxis().setOrigin(0.0);
        getXAxis().setInterval(1.0);
        getXAxis().setTitle("T, сек");

        // Grid
        setGridType(GridType.Vertical);
        getTheme().setGridLineStyle(DashStyle.Dash);
        getTheme().setGridLineColor(new Color(186, 186, 186));

        // Series
        getTheme().setHighlightStroke(new SolidBrush(new Color(255, 147, 66)));
        getTheme().setCommonSeriesStrokes(
                Arrays.asList(
                        new SolidBrush(new Color(206, 0, 0)),
                        new SolidBrush(new Color(255, 255, 255))
                )
        );
        getTheme().setCommonSeriesFills(
                Arrays.asList(
                        new SolidBrush(new Color(206, 0, 0)),
                        new SolidBrush(new Color(0, 106, 0))
                )
        );
        getTheme().setCommonSeriesStrokeThicknesses(
                Arrays.asList(5.0, 0.0)
        );
        getTheme().setCommonSeriesStrokeDashStyles(
                Arrays.asList(DashStyle.Solid, DashStyle.Solid)
        );
        getTheme().setDataLabelsFontSize(20);

        setShowLegend(false);
    }

    public void initSeries() {
        mainSeriesYData = generateSeries(cacheSize, 0.0);
        mainSeriesXData = generateSeries(cacheSize, 0.0);
        decodedSeriesXData = generateSeries(cacheSize, 0.0);
        decodedSeriesYData = generateSeries(cacheSize, -3.0);

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
        getSeries().add(mainSeries);
        getSeries().add(decodedSeries);
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

    public void nextPoint(int b) {
        mainSeriesXData.remove(0);
        mainSeriesYData.remove(0);
        mainSeriesXData.remove(0);
        mainSeriesYData.remove(0);
        decodedSeriesXData.remove(0);
        decodedSeriesNames.remove(0);

        switch (b) {
            case 0 -> {
                mainSeriesXData.add(time);
                mainSeriesYData.add(-2.0);
                time += 1.0;
                mainSeriesXData.add(time);
                mainSeriesYData.add(-2.0);
                LOGGER.debug("New chart update to 00 (-2)");

                decodedSeriesXData.add(time - 0.5);
                decodedSeriesNames.add("0  0");
            }
            case 1 -> {
                mainSeriesXData.add(time);
                mainSeriesYData.add(-1.0);
                time += 1.0;
                mainSeriesXData.add(time);
                mainSeriesYData.add(-1.0);
                LOGGER.debug("New chart update to 01 (-1)");

                decodedSeriesXData.add(time - 0.5);
                decodedSeriesNames.add("0  1");
            }
            case 10 -> {
                mainSeriesXData.add(time);
                mainSeriesYData.add(1.0);
                time += 1.0;
                mainSeriesXData.add(time);
                mainSeriesYData.add(1.0);
                LOGGER.debug("New chart update to 10 (1)");

                decodedSeriesXData.add(time - 0.5);
                decodedSeriesNames.add("1  0");
            }
            case 11 -> {
                mainSeriesXData.add(time);
                mainSeriesYData.add(2.0);
                time += 1.0;
                mainSeriesXData.add(time);
                mainSeriesYData.add(2.0);
                LOGGER.debug("New chart update to 11 (2)");

                decodedSeriesXData.add(time - 0.5);
                decodedSeriesNames.add("1  1");
            }
            case -1 -> {
                mainSeriesXData.add(time);
                mainSeriesYData.add(0.0);
                time += 1;
                mainSeriesXData.add(time);
                mainSeriesYData.add(0.0);

                decodedSeriesXData.add(time - 0.5);
                decodedSeriesNames.add(" ");
                LOGGER.debug("New chart update to -1 (0)");
            }
            default -> {
                LOGGER.error("Incorrect input value: {}", b);
                throw new IllegalArgumentException("Incorrect input value " + b);
            }
        }
        updateSeries();
    }

    public void updateSeries() {
        double maxXValue = mainSeriesXData.get(cacheSize - 1);
        if (maxXValue > getXAxis().getMaxValue() - 2.0) {
            getXAxis().setMaxValue(maxXValue + 2.0);
            getXAxis().setMinValue(maxXValue - 18.0);
        }
        repaint();
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public double getTime() {
        return time;
    }

    public List<Double> getMainSeriesXData() {
        return mainSeriesXData;
    }

    public List<Double> getMainSeriesYData() {
        return mainSeriesYData;
    }

    public List<Double> getDecodedSeriesXData() {
        return decodedSeriesXData;
    }

    public List<Double> getDecodedSeriesYData() {
        return decodedSeriesYData;
    }

    public List<String> getDecodedSeriesNames() {
        return decodedSeriesNames;
    }

    public double getyMaxValue() {
        return yMaxValue;
    }

    public double getyMinValue() {
        return yMinValue;
    }

    public double getxMaxValue() {
        return xMaxValue;
    }

    public double getxMinValue() {
        return xMinValue;
    }
}
