//package io.feedback.exercise
//
//import scala.util.Random
//import io.feedback.PlotSource
//import rx.lang.scala.Observable
//import rx.lang.scala.subjects.BehaviorSubject
//
//class Buffer(maxWip: Int, maxFlow: Int) {
//  private[this] var queued = 0
//  private[this] var wip = 0 // ready pool
//
//  private[this] val rng = new Random
//
//  def work(u: Double): Int = {
//    // Add to ready pool
//    // 0 <= u <= maxWip
//    wip += math.min(math.max(0, math.round(u)), maxWip).toInt
//
//    // Transfer r items from ready pool to queue
//    val r = math.round(rng.nextDouble() * wip).toInt
//    wip -= r
//    queued += r
//
//    // Release s items from queue to downstream process
//    // s <= #items in queue
//    val s = math.min(math.round(rng.nextDouble() * maxFlow).toInt, queued)
//    queued -= s
//
//    queued
//  }
//}
//
//class BufferOpenLoop extends PlotSource {
//  val seriesLabel = "buffer open-loop"
//  val yLabel = "q-length"
//
//  val steps = 5000
//
//  def setpoint(step: Int) = 5.0
//
//  def data: Observable[Double] = {
//    val buffer = new Buffer(10, 10)
//    Observable.from(0 to steps)
//      .map(setpoint)
//      .map(buffer.work)
//  }
//}
//
//class BufferClosedLoop extends PlotSource {
//  val seriesLabel = "buffer closed-loop"
//  val yLabel = "q-length"
//
//  val steps = 500
//
//  def setpoint(step: Int) =
//    if (step < 100) 0.0
//    else if (step < 300) 50.0
//    else 10.0
//
//  val kp = 1.25
//  val ki = 0.01
//  def control(err: Double, cerr: Double): Double = kp*err + ki*cerr
//
//  def data = Observable[Double] { s =>
//    val buffer = new Buffer(10, 10)
//
//    val qlen = BehaviorSubject(0.0)
//    qlen.subscribe(s)
//
//    Observable.from(0 to steps)
//      .map(setpoint)
//      .zipWith(qlen)(_ - _)
//      .scan((0.0, 0.0)) { (c, e) => (e, e + c._2) }
//      .map((control(_, _)).tupled)
//      .map(buffer.work)
//      .map(_.toDouble)
//      .subscribe(qlen)
//  }
//}