package collector

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import helper.ConfigHelper

import java.io.ObjectInputFilter.Config
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object TestYouBikeCollector extends App {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "TestSystem")
  implicit val ec:     ExecutionContext     = system.executionContext

  val collector = new YouBikeCollector(ConfigHelper)(system)
  collector.collectStations().onComplete {
    case Success(_) =>
      println("Data collection completed successfully!")
      system.terminate()
    case Failure(e) =>
      println(s"Error during data collection: ${e.getMessage}")
      e.printStackTrace()
      system.terminate()
  }
}
