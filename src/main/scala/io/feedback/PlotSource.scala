package io.feedback

import rx.lang.scala.Observable

/**
 * TimeSeries represents a collection of uniformly spaced data points.
 *
 * @param label a identifier for the series
 * @param steps The number of elements in `data`.
 * @param data A stream of doubles which represent the data set.
 */
case class TimeSeries(label: String, size: Int, data: Observable[Double])

/**
 * A `PlotSource` serves as input to a `Plot`.
 */
trait PlotSource {

  /** label for this data source */
  def seriesLabel: String

  /** label for what this data tracks */
  def yLabel: String

  /**
   * Number of event steps. Plots use event time, so
   * this is represents the number of events.
   */
  def steps: Int

  final def time: Observable[Int] = Observable.from(0 to steps)

  /**
   * Setpoint for controller at `step`. This allows
   * a step function implementation the setpoint.
   */
  def setpoint(step: Int): Double

  /**
   * The process output that we are observing.
   */
  def data: Observable[Double]

  /**
   * A collection of [[TimeSeries]] to plot.
   */
  def series: Seq[TimeSeries] = Seq(
    TimeSeries("setpoint", steps, time.map(setpoint(_))),
    TimeSeries(yLabel, steps, data)
  )
}