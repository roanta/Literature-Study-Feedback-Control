package io.feedback

import scala.util.Random

object Server {
  val assingedCores: Int = 2
  val physicalCores: Int = 4
  // throttling time based on assigned number of cores
  val lowWatermark = assingedCores * 25
  // the maximum throttling time
  val highWatermark = physicalCores * 25
}

/**
  * A server that generates load and may get throttled.
  */
trait Server {
  val name: String

  /**
    * describe throttling characteristics
    *
    * @param step
    * @param load
    * @return throttling time
    */
  def throttle(step: Int, load: Double): Double

  /**
    * describe load
    *
    * @param step
    * @return load for a given step
    */
  def load(step: Int): Double
}

class OverloadServer extends Server{
  import Server._

  private[this] val rng = new Random("seed".hashCode)
  val name = "overload"

  /**
    * Increase throttling with load
    *
    * load [0, 90), no throttling
    * load [90, 100), randomly between no throttling and a small amount of throttling
    * load [100, 300), grows from 0 to lowWatermark throttling
    * load [300, Infinity), grows from lowWatermark to highWatermark throttling
    *
    * @param step
    * @param qps
    * @return throttling time
    */
  def throttle(step: Int, qps: Double): Double = {
    if (qps < 90) 0.0
    else if (qps < 100)
      if (rng.nextBoolean()) rng.nextInt(10) else 0.0
    // up to lowWatermark
    else if (qps < 300) lowWatermark/200.0 * (qps - 100)
    //(lowWatermark, highWatermark]
    else if (qps < 400) (highWatermark - lowWatermark)/100.0 * (qps - 300) + lowWatermark
    else highWatermark
  }

  def load(step: Int): Double = {
    step * 5 + 50
  }
}

class SlowdownServer extends Server{
  import Server._

  private[this] val rng = new Random
  val name = "slowdown"
  val constantLoad = 100.0

  /**
    * throttling at certain steps
    *
    * @param step
    * @param qps
    * @return throttling time
    */
  def throttle(step: Int, qps: Double): Double = {
    // assume rate limit remove throttling
    if (qps < constantLoad) 0.0
    else {
      if (step < 20) 0.0
      // more and more throttling
      else if (step < 50) lowWatermark/30 * (step - 20)
      else if (step < 70) (highWatermark - lowWatermark)/20 * (step - 50) + lowWatermark
      else if (step < 80) highWatermark
      // goes back to normal
      else 0.0
    }
  }

  def load(step: Int): Double = constantLoad
}
