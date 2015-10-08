package io.feedback.exercise

import scala.util.Random
import io.feedback.{PlotSource, TimeSeries}
import rx.lang.scala.Observable
import rx.lang.scala.subjects.BehaviorSubject

/**
 * The process/plant state -- aperture defines a window over
 * discrete units.
 */
case class Aperture(min: Int, max: Int) {
  private[this] var aperture = 1
  def widen(): Unit = aperture += 1
  def narrow(): Unit = aperture -= 1
  def value: Int = aperture
}

/**
 * A simple representation of load entering and leaving the system.
 */
case class Load(maxBurst: Int) {
  private[this] val rng = new Random
  private[this] var load: Int = 0

  def step(): Int = {
    // "complete" some of the work randomly
    if (load > 0) load -= rng.nextInt(load)
    // offer load
    load += rng.nextInt(maxBurst)
    load
  }
}

/**
 * Maintain an exponential moving average of Long-typed values over a
 * given window on a user-defined clock.
 */
case class Ema(window: Long) {
  private[this] var time = Long.MinValue
  private[this] var ema = 0D

  def isEmpty: Boolean = time < 0

  /**
   * Update the average with observed value `x`, and return the new average.
   */
  def update(stamp: Long, x: Double): Double = {
    if (time == Long.MinValue) {
      time = stamp
      ema = x
    } else {
      val td = stamp-time
      assert(td >= 0, "Nonmonotonic timestamp")
      time = stamp
      val w = if (window == 0) 0 else math.exp(-td.toDouble/window)
      ema = x*(1-w) + ema*w
    }
    ema
  }

  /**
   * Return the last observation. This is generally only safe to use if you
   * control your own clock, since the current value depends on it.
   */
  def last: Double = ema
}

/**
 * The load band controller tries to maintain the smallest apertuer such that
 * the concurrent average load to each serving unit stays within the load band,
 * delimited by `lowLoad` and `highLoad`.
 *
 * The controller only makes single unit directional changes. That is,
 * we don't take the maginitude of our error into account. This can be
 * seen a simple on-off controller where the load-band represents a dead
 * zone.
 */
class LoadBandClosedLoop extends PlotSource {
  val seriesLabel = "LoadBand bang-bang controller"
  val yLabel = "average load"

  val steps = 1000

  val lowLoad: Double = 0.5
  val highLoad: Double = 2.0

  // The expectation is that a loadband of [0.5, 2.0] will converge on
  // to an average load of 1.0. That is, our setpoint is 1.0.
  def setpoint(step: Int) = 1.0

  def data: Observable[Double] = {
    val plant = Aperture(min = 1, max = 50)
    val loadGen = Load(maxBurst = 20)
    val ema = Ema(window = 5L)

    // The input into the plant is the average load and the output is the
    // new size of the aperture. We make only directional adjustments as required,
    // incrementing or decrementing the aperture by 1.
    // -1 = off / narrow
    // 0 = dead zone
    // 1 = on / widen
    def control(avgLoad: Double): Unit = {
      if (avgLoad >= highLoad && plant.value < plant.max)
        plant.widen()
      else if (avgLoad <= lowLoad && plant.value > plant.min)
        plant.narrow()
    }

    Observable.from(0 to steps).map { step =>
      val load = loadGen.step().toDouble
      val avg = load / plant.value
      control(ema.update(step, avg))
      ema.last
    }
  }

  override def series = super.series ++ Seq(
    TimeSeries("low load", steps, time.map(_ => lowLoad)),
    TimeSeries("high load", steps, time.map(_ => highLoad))
  )
}