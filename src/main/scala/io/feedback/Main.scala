package io.feedback

import javafx.application.Application
import javafx.scene.control.TabPane
import javafx.scene.Scene
import javafx.stage.Stage
import io.feedback.exercise._

class Viz extends Application {
  val simulations: Seq[Plot] = Seq(
    Plot.fromSource(new BufferOpenLoop),
    Plot.fromSource(new BufferClosedLoop)
  )

  def start(stage: Stage) = {
    val tabPane = new TabPane
    tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE)
    tabPane.getTabs.addAll(simulations:_*)

    stage.setScene(new Scene(tabPane, 1024, 768))
    stage.setTitle("Feedback Control Systems - Simulation")
    stage.show()
  }
}

object Main extends App {
  Application.launch(classOf[Viz])
}