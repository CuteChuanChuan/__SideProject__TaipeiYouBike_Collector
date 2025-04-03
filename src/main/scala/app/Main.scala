package app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import collector.YouBikeCollector
import helper.ConfigHelper
import org.slf4j.{ Logger, LoggerFactory }

object Main {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(Behaviors.empty, "BikeDataCollectorSystem")

    try {
      val collector = new YouBikeCollector(ConfigHelper)(system)
      collector.startScheduledCollection()

      logger.info("YouBike data collector started. Press CTRL+C to terminate.")

      sys.addShutdownHook {
        logger.info("Terminating actor system...")
        system.terminate()
      }

      Thread.currentThread().join()

    } catch {
      case e: Exception =>
        logger.error(s"Error starting collector: ${e.getMessage}")
        system.terminate()
    }
  }

}
