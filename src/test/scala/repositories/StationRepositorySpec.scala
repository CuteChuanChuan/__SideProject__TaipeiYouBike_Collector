package repositories

import helper.MongoDBConfig
import model.{ DocFields, SCDMetadata, StationDim, StationFct }
import org.bson.Document
import org.mockito.Mockito.mock
import org.mongodb.scala.{ MongoClient, MongoCollection, MongoDatabase }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import services.SCDService

import java.time.format.DateTimeFormatter
import java.time.{ LocalDateTime, ZoneId, ZonedDateTime }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class StationRepositorySpec extends AnyFlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  implicit val ec:                      ExecutionContext = ExecutionContext.global
  implicit override val patienceConfig: PatienceConfig   = PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  private val mongoDBContainer      = new MongoDBContainer(DockerImageName.parse("mongo:8.0"))
  private val testDBName            = "test_db"
  private val testCollectionDimName = "dim"
  private val testCollectionFctName = "fct"

  private var mongoClient:   MongoClient               = _
  private var database:      MongoDatabase             = _
  private var dimCollection: MongoCollection[Document] = _
  private var fctCollection: MongoCollection[Document] = _

  private val mockSCDService:    SCDService        = mock(classOf[SCDService])
  private var stationRepository: StationRepository = _

  override def beforeAll(): Unit = {
    super.beforeAll()

    mongoDBContainer.start()
    val mongoUri = mongoDBContainer.getReplicaSetUrl(testDBName)
    mongoClient = MongoClient(mongoUri)
    database = mongoClient.getDatabase(testDBName)
    dimCollection = database.getCollection(testCollectionDimName)
    fctCollection = database.getCollection(testCollectionFctName)

    val mockConfig: MongoDBConfig = new MongoDBConfig {
      override val uri:           String = mongoUri
      override val database:      String = testDBName
      override val dimCollection: String = testCollectionDimName
      override val fctCollection: String = testCollectionFctName
    }
    stationRepository = new StationRepository(mockSCDService, mockConfig)
  }

  override def afterAll(): Unit = {
    mongoClient.close()
    mongoDBContainer.stop()
    super.afterAll()
  }

  def clearCollections(): Unit = {
    dimCollection.deleteMany(new Document()).toFuture().futureValue
    fctCollection.deleteMany(new Document()).toFuture().futureValue
  }

  "StationRepository" should "insert station facts successfully" in {

    clearCollections()

    val facts        = Seq(
      StationFct(
        stationId = 1,
        timestampFetched = ZonedDateTime.now(),
        stationDistrict = "Test District",
        isActive = true,
        availableBikes = 10,
        availableDocks = 20,
        timestampSrcUpdated = LocalDateTime
          .parse("2025-03-01 21:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
          .atZone(ZoneId.of("Asia/Taipei"))
          .withZoneSameInstant(ZoneId.of("UTC"))
      ),
      StationFct(
        stationId = 2,
        timestampFetched = ZonedDateTime.now(),
        stationDistrict = "Test District 2",
        isActive = true,
        availableBikes = 20,
        availableDocks = 30,
        timestampSrcUpdated = LocalDateTime
          .parse("2025-03-01 21:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
          .atZone(ZoneId.of("Asia/Taipei"))
          .withZoneSameInstant(ZoneId.of("UTC"))
      )
    )
    stationRepository.insertStationFacts(facts).futureValue
    val insertedDocs = fctCollection.find().toFuture().futureValue
    insertedDocs should have size 2
    val stationsIds  = insertedDocs.map(_.getInteger(DocFields.Fct.STATION_ID)).toSet
    stationsIds should contain allOf (1, 2)
  }

  "StationRepository" should "insert station dimensions successfully" in {

    clearCollections()

    val dims = Seq(
      StationDim(
        timestampUpdated = ZonedDateTime.now(),
        stationId = 1,
        name = "Dim 1",
        nameZh = "第一筆測試",
        address = "Address",
        addressZh = "地址",
        latitude = 25,
        longitude = 20,
        totalDocks = 50,
        districtZh = "測試區",
        isActive = true
      ),
      StationDim(
        timestampUpdated = ZonedDateTime.now(),
        stationId = 2,
        name = "Dim 2",
        nameZh = "第二筆測試",
        address = "Address 2",
        addressZh = "地址 2",
        latitude = 20,
        longitude = 25,
        totalDocks = 100,
        districtZh = "測試區 2",
        isActive = false
      )
    )

    val metadata = SCDMetadata(effectiveFrom = ZonedDateTime.now(), effectiveTo = None, isCurrent = true)

    stationRepository.insertStationDims(dims = dims, metadata = metadata).futureValue
    val insertedDocs = dimCollection.find().toFuture().futureValue
    insertedDocs should have size 2
    val stationIds   = insertedDocs.map(_.getInteger(DocFields.Dim.STATION_ID)).toSet
    stationIds should contain allOf (1, 2)

    for (doc <- insertedDocs)
      doc.getBoolean(DocFields.Metadata.IS_CURRENT) shouldBe true
  }

}
