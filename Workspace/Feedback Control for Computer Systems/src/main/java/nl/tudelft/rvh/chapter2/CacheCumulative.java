package nl.tudelft.rvh.chapter2;

import java.util.concurrent.TimeUnit;

import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import nl.tudelft.rvh.ChartTab;
import nl.tudelft.rvh.Tuple;
import nl.tudelft.rvh.rxjavafx.Observables;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

public class CacheCumulative extends ChartTab {
	
	private float k;

	public CacheCumulative() {
		super("Chapter 2 - Cumulative", "Cumulative simulation", "time", "cache size");
	}
	
	@Override
	public HBox bottomBox() {
		this.k = 160;
		
		HBox box = super.bottomBox();
		TextField tf = new TextField(String.valueOf(this.k));
		box.getChildren().add(tf);
		
		Observables.fromNodeEvents(tf, ActionEvent.ACTION)
				.map(event -> tf.getText())
				.map(Integer::parseInt)
				.subscribe(i -> this.k = i, e -> {});
		
		return box;
	}
	
	@Override
	public String seriesName() {
		return "k = " + this.k;
	}

	@Override
	public Observable<Tuple<Number, Number>> runSimulation() {
		Observable<Long> time = Observable.interval(50L, TimeUnit.MILLISECONDS).take(200);

		Func1<Long, Double> setPoint = t -> t < 50 ? 0.6
				: t < 100 ? 0.8
						: t < 150 ? 0.1
								: 0.9;
		Func1<Double, Double> cache = size -> size < 0 ? 0
				: size > 100 ? 1
						: size / 100;

		Observable<Double> feedbackLoop = Observable.create(subscriber -> {
			PublishSubject<Double> hitrate = PublishSubject.create();

			time.map(setPoint)
					.zipWith(hitrate, (a, b) -> a - b)
					.scan((e, cum) -> e + cum)
					.map(cum -> this.k * cum)
					.map(cache)
					.subscribe(hitrate::onNext);

			hitrate.subscribe(subscriber);
			hitrate.onNext(0.0);
		});
		return time.zipWith(feedbackLoop, Tuple<Number, Number>::new);
	}
}