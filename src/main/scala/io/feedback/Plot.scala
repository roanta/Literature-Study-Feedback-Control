package io.feedback

import javafx.geometry.Insets
import javafx.scene.chart.XYChart.{Data, Series}
import javafx.scene.chart.{Chart, LineChart, NumberAxis}
import javafx.scene.control.Tab
import javafx.scene.layout.{HBox, VBox, Priority}
import rx.lang.scala.Observable

class Plot(title: String, timeSeries: TimeSeries) extends Tab(title) {
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
    chart.setCreateSymbols(true)
    chart
  }

  val series1 = new Series[Number, Number]
  series1.setName(timeSeries.labels(0))
  val series2 = new Series[Number, Number]
  series2.setName(timeSeries.labels(1))
  val series3 = new Series[Number, Number]
  series3.setName(timeSeries.labels(2))

  chart.getData.add(series1)
  chart.getData.add(series2)
  chart.getData.add(series3)

  val data: Observable[(Data[Number, Number], Data[Number, Number], Data[Number, Number])] =
    Observable.from(0 to timeSeries.size).zipWith(timeSeries.data) { case (x, (y1, y2, y3)) =>
      (new Data(x.asInstanceOf[Number], y1.asInstanceOf[Number]),
        new Data(x.asInstanceOf[Number], y2.asInstanceOf[Number]),
        new Data(x.asInstanceOf[Number], y3.v.asInstanceOf[Number]))
    }
  data.subscribe { x =>
    series1.getData.add(x._1)
    series2.getData.add(x._2)
    series3.getData.add(x._3)
  }
//
//  // flush time-series data to chart
//  timeSeries.foreach { ts =>
//    val series = new Series[Number, Number]
//    series.setName(ts.label)
//
//    chart.getData.add(series)
//
//    Observable.from(0 to ts.size)
//      .zipWith(ts.data)((_, _))
//      .map { case (x, y) =>
//        new Data(x.asInstanceOf[Number], y.asInstanceOf[Number]) }
//      .subscribe(series.getData.add(_))
//  }

  // flush ui to tab
  private[this] val box = new VBox(chart)
  box.setPadding(new Insets(15.0))
  VBox.setVgrow(chart, Priority.ALWAYS)
  setContent(box)
}

object Plot {
  def apply(ps: PlotSource): Plot =
    new Plot(ps.seriesLabel, ps.series)
}