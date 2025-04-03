package repositories

import helper.{ ConfigHelper, MongoDBConfig }
import model._
import org.bson.Document
import org.mongodb.scala.model.{ Filters, Updates }
import org.mongodb.scala.{ MongoClient, MongoCollection, MongoDatabase, _ }
import org.slf4j.Logger
import services.SCDService
import services.converter.DocumentConverter

import java.time.format.DateTimeFormatter
import java.time.{ ZoneId, ZonedDateTime }
import scala.concurrent.{ ExecutionContext, Future }

class StationRepository(
  private val scdService:  SCDService,
  private val mongoConfig: MongoDBConfig = ConfigHelper.MongoDB)(implicit ec: ExecutionContext) {

  private val logger: Logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  private val mongoClient:   MongoClient               = MongoClient(mongoConfig.uri)
  private val database:      MongoDatabase             = mongoClient.getDatabase(mongoConfig.database)
  private val dimCollection: MongoCollection[Document] = database.getCollection(mongoConfig.dimCollection)
  private val fctCollection: MongoCollection[Document] = database.getCollection(mongoConfig.fctCollection)

  def insertStationFacts(facts: Seq[StationFct]): Future[Unit] = {
    val docs: Seq[Document] = facts.map(DocumentConverter.fctToDocument)
    insertDocuments(collection = fctCollection, docs = docs, docType = "Fact")
  }

  def insertStationDims(dims: Seq[StationDim], metadata: SCDMetadata): Future[Unit] = {
    val docs: Seq[Document] = dims.map(dim => DocumentConverter.dimToDocument(dim = dim, metadata = metadata))
    insertDocuments(collection = dimCollection, docs = docs, docType = "Dimension")
  }

  def getCurrentStationDims(stationIds: Seq[Int]): Future[Map[Int, StationDimWithSCD]] = {
    val filters = Filters.and(
      Filters.in(DocFields.Dim.STATION_ID, stationIds: _*),
      Filters.eq(DocFields.Metadata.IS_CURRENT, true)
    )

    dimCollection
      .find(filters)
      .toFuture()
      .map(docs =>
        docs.map { doc =>
          val dim      = DocumentConverter.documentToDim(doc)
          val metadata = DocumentConverter.documentToSCDMetadata(doc)
          dim.stationId -> StationDimWithSCD(dim, metadata)
        }.toMap)
  }

  def closeStationDims(stationIds: Seq[Int], closingTime: ZonedDateTime): Future[Unit] = {
    if (stationIds.isEmpty) return Future.successful(())

    val filters = Filters.and(
      Filters.in(DocFields.Dim.STATION_ID, stationIds: _*),
      Filters.eq(DocFields.Metadata.IS_CURRENT, true)
    )

    val closedMetadataValues = scdService.getClosedMetadataValues(closingTime)
    val effectiveToUTC       = closedMetadataValues.effectiveTo.withZoneSameInstant(ZoneId.of("UTC"))
    val effectiveToStr       = effectiveToUTC.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))

    val updates = Updates.combine(
      Updates.set(DocFields.Metadata.IS_CURRENT, closedMetadataValues.isCurrent),
      Updates.set(DocFields.Metadata.EFFECTIVE_TO, effectiveToStr)
    )

    dimCollection
      .updateMany(filters, updates)
      .toFuture()
      .map(_ => ())
  }

  private def insertDocuments(
    collection: MongoCollection[Document],
    docs:       Seq[Document],
    docType:    String): Future[Unit] =
    if (docs.isEmpty) Future.successful(())
    else {
      collection
        .insertMany(docs)
        .toFuture()
        .map { result =>
          logger.info(s"Successfully inserted ${result.getInsertedIds.size()} $docType documents")
          ()
        }
        .recover { case e: Exception =>
          logger.error(s"Error inserting $docType documents: ${e.getMessage}")
          throw e

        }
    }
}
