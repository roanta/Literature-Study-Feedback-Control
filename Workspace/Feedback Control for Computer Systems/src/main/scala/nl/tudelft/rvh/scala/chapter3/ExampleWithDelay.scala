package nl.tudelft.rvh.scala.chapter3

import scala.concurrent.duration.DurationInt

import ObsExtensions.extendObservable
import javafx.event.ActionEvent
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import nl.tudelft.rvh.rxscalafx.Observables
import nl.tudelft.rvh.scala.ScalaChartTab
import rx.lang.scala.Observable
import rx.lang.scala.Subscriber
import rx.lang.scala.subjects.BehaviorSubject

class ExampleWithDelay extends ScalaChartTab("Chapter 3 - Example with delay", "Example with delay", "time", "output") {

	private var delay: Int = 1

	override def bottomBox(): HBox = {
		this.delay = 1

		val box = super.bottomBox()
		val tf = new TextField((this.delay - 1).toString)
		box.getChildren.add(tf)

		Observables.fromNodeEvents(tf, ActionEvent.ACTION)
			.map { _ => tf.getText }
			.map { _.toInt }
			.subscribe(i => this.delay = i + 1, _ -> {})

		box
	}

	def seriesName(): String = "delay = " + (delay - 1)

	def simulation(): Observable[(Number, Number)] = {
		val time = Observable.interval(50 milliseconds).take(20)
		def setPoint(time: Long): Double = 1
		val gain = 0.8

		val feedbackLoop = Observable((subscriber: Subscriber[Double]) => {
			val hitrate = BehaviorSubject[Double]

			time.map(setPoint)
				.zipWith(hitrate)(_ - _)
				.map { gain * _ }
				.delay(this.delay, 0.0)
				.subscribe(hitrate)

			hitrate.subscribe(subscriber)
		})
		time.zipWith(feedbackLoop)((_, _))
	}

	def simulationForGitHub(): Observable[Double] = {
		def setPoint(time: Int): Double = 1

		Observable((subscriber: Subscriber[Double]) => {
			val hitrate = BehaviorSubject[Double]

			Observable.from(0 until 20)
				.map(setPoint)
				.zipWith(hitrate)(_ - _)
				.map { 0.8 * _ }
				.delay(this.delay, 0.0)
				.subscribe(hitrate)

			hitrate.subscribe(subscriber)
		})
	}
}