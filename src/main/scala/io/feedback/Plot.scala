package io.feedback

import javafx.geometry.Insets
import javafx.scene.chart.XYChart.{Data, Series}
import javafx.scene.chart.{Chart, LineChart, NumberAxis}
import javafx.scene.control.Tab
import javafx.scene.layout.{HBox, VBox, Priority}
import rx.lang.scala.Observable

class Plot(title: String, timeSeries: TimeSeries*) extends Tab(title) {
  // Note, we're forced here to use java.lang.Number because
  // we don't have `Axis` implementations for Double. This is
  // fine since we can transparently cast a scala.Double to a
  // java.lang.Numeric.
  private[this] val chart: LineChart[Number, Number] = {
    val xAxis = new NumberAxis
    val yAxis = new NumberAxis
    xAxis.setLabel("Time Steps")
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

    Observable.from(0 to ts.size)
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
  def apply(ps: PlotSource): Plot =
    new Plot(ps.seriesLabel, ps.series:_*)
}