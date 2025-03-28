package collector

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import helper.ConfigHelper
import model.{ Station, StationDim }
import org.slf4j.{ Logger, LoggerFactory }
import repositories.StationRepository
import services.SCDService
import services.converter.{ ModelConverter, StationProtocol }

import java.time.ZonedDateTime
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class YouBikeCollector(configHelper: ConfigHelper)(implicit system: ActorSystem[_]) {

  private implicit val ec:       ExecutionContext  = system.executionContext
  private val scdService:        SCDService        = new SCDService()
  private val stationRepository: StationRepository = new StationRepository(scdService)
  private val logger:            Logger            = LoggerFactory.getLogger(this.getClass)

  def startScheduledCollection(): Unit = {
    val fetchInterval = configHelper.Api.fetchInterval

    system.scheduler.scheduleAtFixedRate(0.seconds, fetchInterval) { () =>
      logger.info(s"Executing scheduled collection at: ${ZonedDateTime.now()}")
      collectStations().onComplete {
        case Success(_)         => logger.info("Scheduled collection completed successfully")
        case Failure(exception) => logger.error(s"Scheduled collection failed: ${exception.getMessage}")
      }
    }
  }

  def collectStations(): Future[Unit] = {
    val retrievedTime = ZonedDateTime.now()

    for {
      jsonData <- fetchApiData()
      stations   = parseJsonData(jsonData)
      fctRecords = stations.map(station => ModelConverter.stationToFct(station, retrievedTime))
      _ <- stationRepository.insertStationFacts(fctRecords)
      dimRecords = stations.map(station => ModelConverter.stationToDim(station, retrievedTime))
      _ <- processStationDims(dimRecords, retrievedTime)
    } yield ()
  }

  private def processStationDims(newDims: Seq[StationDim], timestamp: ZonedDateTime): Future[Unit] = {
    val stationIds = newDims.map(_.stationId)
    stationRepository
      .getCurrentStationDims(stationIds)
      .flatMap { currentDimsMap =>
        val (dimsToUpdate, dimsToInsert) = newDims.partition(dim => currentDimsMap.contains(dim.stationId))
        val dimsWithChanges              = dimsToUpdate.filter { newDim =>
          val currentDimWithSCD = currentDimsMap(newDim.stationId)
          scdService.hasAttributesChanged(currentDimWithSCD.dim, newDim)
        }

        val stationIdsToClose = dimsWithChanges.map(_.stationId)
        val allDimsToInsert   = dimsToInsert ++ dimsWithChanges
        val newMetadata       = scdService.createNewSCDMetadata(timestamp)

        for {
          _ <- if (stationIdsToClose.nonEmpty) stationRepository.closeStationDims(stationIdsToClose, timestamp)
               else Future.successful(())
          _ <- if (allDimsToInsert.nonEmpty) stationRepository.insertStationDims(allDimsToInsert, newMetadata)
               else Future.successful(())
        } yield ()
      }
  }

  private[collector] def fetchApiData()(implicit system: ActorSystem[_]): Future[String] =
    for {
      response <- Http()(system).singleRequest(HttpRequest(uri = ConfigHelper.Api.url))
      strictEntity <- response.entity.toStrict(5.seconds)
      data = strictEntity.getData().utf8String
    } yield data

  private[collector] def parseJsonData(jsonData: String): List[Station] = {
    import StationProtocol._
    import spray.json._
    jsonData.parseJson.convertTo[List[Station]]
  }
}
