package io.feedback

import javafx.geometry.Insets
import javafx.scene.chart.XYChart.{Data, Series}
import javafx.scene.chart.{Chart, LineChart, NumberAxis}
import javafx.scene.control.Tab
import javafx.scene.layout.{HBox, VBox, Priority}
import rx.lang.scala.Observable

/**
 * We use event time, so our time series are uniformly spaced data points.
 */
case class TimeSeries(label: String, numEvents: Int, data: Observable[Double])

class Plot(title: String, yLabel: String, timeSeries: TimeSeries*) extends Tab(title) {
  // Note, we're forced here to use java.lang.Number because
  // we don't have `Axis` implementations for Double. This is
  // fine since we can transparently cast a scala.Double to a
  // java.lang.Numeric.
  private[this] val chart: LineChart[Number, Number] = {
    val xAxis = new NumberAxis
    val yAxis = new NumberAxis
    xAxis.setLabel("Time Steps")
    yAxis.setLabel(yLabel)
    val chart = new LineChart(xAxis, yAxis)
    chart.setTitle(title)
    chart.setAnimated(false)
    chart.setCreateSymbols(false)
    chart
  }

  // flush time-series data to chart
  timeSeries.foreach { ts =>
    val series = new Series[Number, Number]
    series.setName(ts.label)

    chart.getData.add(series)

    Observable.from(0 to ts.numEvents)
      .zipWith(ts.data)((_, _))
      .map { case (x, y) => new Data(x.asInstanceOf[Number], y.asInstanceOf[Number]) }
      .subscribe(series.getData.add(_))
  }

  // flush ui to tab
  private[this] val box = new VBox(chart)
  box.setPadding(new Insets(15.0))
  VBox.setVgrow(chart, Priority.ALWAYS)
  setContent(box)
}

object Plot {
  def fromSource(ds: PlotSource): Plot = {
    val setpoint = TimeSeries(
      label = "setpoint",
      numEvents = ds.steps,
      data = Observable.from(0 to ds.steps)
        .map(ds.setpoint)
        .map(_.toDouble)
    )
    val data = TimeSeries(label = ds.yLabel, numEvents = ds.steps, data = ds.data)
    new Plot(ds.seriesLabel, ds.yLabel, setpoint, data)
  }
}