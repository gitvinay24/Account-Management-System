package com.businessmanager.theme;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import java.awt.*;

public final class StitchChartTheme {

    private StitchChartTheme() {
    }

    public static void applyTheme(JFreeChart chart) {
        chart.setBackgroundPaint(Colors.INK_3);
        if (chart.getTitle() != null) {
            chart.getTitle().setPaint(Colors.TEXT);
            chart.getTitle().setFont(Fonts.TITLE_SM);
        }

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(Colors.INK_3);
            chart.getLegend().setItemPaint(Colors.SLATE);
            chart.getLegend().setItemFont(Fonts.BODY_SMALL);
        }

        Plot plot = chart.getPlot();
        plot.setBackgroundPaint(Colors.INK_2);
        plot.setOutlinePaint(Colors.LINE);

        if (plot instanceof CategoryPlot catPlot) {
            catPlot.setRangeGridlinePaint(Colors.LINE);
            catPlot.setDomainGridlinePaint(Colors.LINE);

            CategoryAxis domainAxis = catPlot.getDomainAxis();
            domainAxis.setTickLabelPaint(Colors.SLATE);
            domainAxis.setLabelPaint(Colors.TEXT);
            domainAxis.setTickLabelFont(Fonts.BODY_SMALL);

            ValueAxis rangeAxis = catPlot.getRangeAxis();
            rangeAxis.setTickLabelPaint(Colors.SLATE);
            rangeAxis.setLabelPaint(Colors.TEXT);
            rangeAxis.setTickLabelFont(Fonts.BODY_SMALL);

            if (catPlot.getRenderer() instanceof BarRenderer barRenderer) {
                barRenderer.setSeriesPaint(0, Colors.MARIGOLD);
                barRenderer.setSeriesPaint(1, Colors.SLATE);
                barRenderer.setSeriesPaint(2, Colors.GOOD);
                barRenderer.setShadowVisible(false);
            } else if (catPlot.getRenderer() instanceof LineAndShapeRenderer lineRenderer) {
                lineRenderer.setSeriesPaint(0, Colors.MARIGOLD);
                lineRenderer.setSeriesStroke(0, new BasicStroke(3.0f));
                lineRenderer.setSeriesPaint(1, Colors.SLATE);
                lineRenderer.setSeriesStroke(1, new BasicStroke(3.0f));
            }
        } else if (plot instanceof XYPlot xyPlot) {
            xyPlot.setRangeGridlinePaint(Colors.LINE);
            xyPlot.setDomainGridlinePaint(Colors.LINE);

            xyPlot.getDomainAxis().setTickLabelPaint(Colors.SLATE);
            xyPlot.getDomainAxis().setLabelPaint(Colors.TEXT);
            xyPlot.getRangeAxis().setTickLabelPaint(Colors.SLATE);
            xyPlot.getRangeAxis().setLabelPaint(Colors.TEXT);

            if (xyPlot.getRenderer() instanceof XYLineAndShapeRenderer xyRenderer) {
                xyRenderer.setSeriesPaint(0, Colors.MARIGOLD);
                xyRenderer.setSeriesStroke(0, new BasicStroke(3.0f));
                xyRenderer.setSeriesPaint(1, Colors.SLATE);
                xyRenderer.setSeriesStroke(1, new BasicStroke(3.0f));
            }
        }
    }
}
