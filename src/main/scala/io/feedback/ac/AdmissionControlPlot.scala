package io.feedback

import rx.lang.scala.Observable
import rx.lang.scala.subjects.BehaviorSubject

class AdmissionControlPlot(plant: Server, controller: AdmissionController) extends PlotSource {
  val seriesLabel = s"${controller.name} controller on ${plant.name} server"
  val yLabel = "throttling"

  val steps = 100

  def setpoint(step: Int): Double = 0.0

  // allowRate, throttled, maxLoad
  val observe = Observable[(Double, Double, MaxHealthLoad)] { s =>
    val subject = BehaviorSubject((100.0, 0.0, MaxHealthLoad.Init))
    subject.subscribe(s)

    time.zipWith(subject) { case (step, (allowRate, v, maxHealthyLoad)) =>
      val (nextAllow, throttled, nextMaxLoad) = controller(step, allowRate, maxHealthyLoad)
      (nextAllow, throttled, nextMaxLoad)
    }.subscribe(subject)
  }

  def data = observe.map(_._2)

  override def series = super.series ++ Seq(
    TimeSeries("allow rate", steps, observe.map(_._1)),
    TimeSeries("max healthy load", steps, observe.map(_._3.v))
  )
}

class WindowedUtil(window: Int) {
  private[this] val buffer = new Array[Double](window)
  private[this] var pos = 0
  private[this] var avg = 0.0
  private[this] var sum = 0.0

  def add(v: Double): Unit = synchronized {
    val oldValue = buffer(pos)
    buffer(pos) = v
    pos = (pos + 1) % window
    avg += (v - oldValue) * 1.0 / window
    sum += v - oldValue
  }

  def getAvg(): Double = synchronized { avg }

  def getSum(): Double = synchronized { sum }
}