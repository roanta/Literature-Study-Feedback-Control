package io.feedback

import rx.lang.scala.Observable

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

  /**
   * Setpoint for controller at `step`. This allows
   * a step function implementation the setpoint.
   */
  def setpoint(step: Int): Double

  /**
   * The data for this controller. Each observed value represents
   * a single pass through the controller. Plots read no more
   * than `steps` events emitted by this observable.
   */
  def data: Observable[Double]
}