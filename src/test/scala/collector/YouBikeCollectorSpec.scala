package collector

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class YouBikeCollectorSpec extends AnyFlatSpec with Matchers {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "TestSystem")
  implicit val ec:     ExecutionContext     = system.executionContext

}
