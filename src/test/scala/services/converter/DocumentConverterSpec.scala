package services.converter

import model.{ DocFields, SCDMetadata, StationDim, StationFct }
import org.bson.Document
import org.mongodb.scala.bson.{ BsonBoolean, BsonDateTime, BsonDouble, BsonInt32, BsonString }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.format.DateTimeFormatter
import java.time.{ temporal, LocalDateTime, ZoneId, ZonedDateTime }
import java.util.Date

class DocumentConverterSpec extends AnyFlatSpec with Matchers {

  val timestamp:        ZonedDateTime = ZonedDateTime.now()
  val timestampFetched: ZonedDateTime = LocalDateTime
    .parse("2025-02-28 10:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    .atZone(ZoneId.of("Asia/Taipei"))
    .withZoneSameInstant(ZoneId.of("UTC"))
  val stationFct:       StationFct    = StationFct(
    stationId = 456,
    timestampFetched = timestamp,
    stationDistrict = "Test District",
    isActive = true,
    availableBikes = 10,
    availableDocks = 20,
    timestampSrcUpdated = timestampFetched
  )
  val stationDim:       StationDim    = StationDim(
    timestampUpdated = timestamp,
    stationId = 123,
    name = "Test Station",
    nameZh = "測試站",
    address = "123 Test St",
    addressZh = "測試街123號",
    latitude = 25.123,
    longitude = 121.456,
    totalDocks = 20,
    districtZh = "測試區",
    isActive = true
  )
  val metadata:         SCDMetadata   = SCDMetadata(
    effectiveFrom = timestamp,
    effectiveTo = None,
    isCurrent = true
  )

  def isSameToMillis(dt1: ZonedDateTime, dt2: ZonedDateTime): Boolean =
    dt1.truncatedTo(temporal.ChronoUnit.MILLIS).isEqual(dt2.truncatedTo(temporal.ChronoUnit.MILLIS))

  "fctToDocument" should "convert StationFct to Document correctly" in {
    val document = DocumentConverter.fctToDocument(stationFct)
    document.get(DocFields.Fct.STATION_ID) shouldBe BsonInt32(456)
    document.get(DocFields.Fct.TIMESTAMP_FETCHED) shouldBe BsonDateTime(timestamp.toInstant.toEpochMilli)
    document.get(DocFields.Fct.STATION_DISTRICT) shouldBe BsonString("Test District")
    document.get(DocFields.Fct.IS_ACTIVE) shouldBe BsonBoolean(true)
    document.get(DocFields.Fct.AVAILABLE_BIKES) shouldBe BsonInt32(10)
    document.get(DocFields.Fct.AVAILABLE_DOCKS) shouldBe BsonInt32(20)
    document.get(DocFields.Fct.TIMESTAMP_SRC_UPDATED) shouldBe BsonDateTime(timestampFetched.toInstant.toEpochMilli)
  }

  "dimToDocument" should "convert StationDim and SCDMetadata to Document correctly" in {
    val document = DocumentConverter.dimToDocument(stationDim, metadata)
    document.get(DocFields.Dim.TIMESTAMP_UPDATED) shouldBe BsonDateTime(timestamp.toInstant.toEpochMilli)
    document.get(DocFields.Dim.STATION_ID) shouldBe BsonInt32(123)
    document.get(DocFields.Dim.NAME) shouldBe BsonString("Test Station")
    document.get(DocFields.Dim.NAME_ZH) shouldBe BsonString("測試站")
    document.get(DocFields.Dim.ADDRESS) shouldBe BsonString("123 Test St")
    document.get(DocFields.Dim.ADDRESS_ZH) shouldBe BsonString("測試街123號")
    document.get(DocFields.Dim.LATITUDE) shouldBe BsonDouble(25.123)
    document.get(DocFields.Dim.LONGITUDE) shouldBe BsonDouble(121.456)
    document.get(DocFields.Dim.TOTAL_DOCKS) shouldBe BsonInt32(20)
    document.get(DocFields.Dim.DISTRICT_ZH) shouldBe BsonString("測試區")
    document.get(DocFields.Dim.IS_ACTIVE) shouldBe BsonBoolean(true)
    document.get(DocFields.Metadata.EFFECTIVE_FROM) shouldBe BsonDateTime(timestamp.toInstant.toEpochMilli)
    document.get(DocFields.Metadata.EFFECTIVE_TO) shouldBe null
  }

  "dimToSCDMetadata" should "convert Document to SCDMetadata correctly" in {
    val doc               = new Document()
      .append(DocFields.Metadata.EFFECTIVE_FROM, Date.from(timestamp.toInstant))
      .append(DocFields.Metadata.EFFECTIVE_TO, null)
      .append(DocFields.Metadata.IS_CURRENT, true)
    val convertedMetadata = DocumentConverter.documentToSCDMetadata(doc)
    isSameToMillis(convertedMetadata.effectiveFrom, metadata.effectiveFrom) shouldBe true
    convertedMetadata.effectiveTo shouldBe metadata.effectiveTo
    convertedMetadata.isCurrent shouldBe metadata.isCurrent
  }

  it should "handle effectiveTo correctly when present" in {
    val closingTime       = timestamp.plusDays(1)
    val doc               = new Document()
      .append(DocFields.Metadata.EFFECTIVE_FROM, Date.from(timestamp.toInstant))
      .append(DocFields.Metadata.EFFECTIVE_TO, Date.from(closingTime.toInstant))
      .append(DocFields.Metadata.IS_CURRENT, false)
    val convertedMetadata = DocumentConverter.documentToSCDMetadata(doc)

    isSameToMillis(convertedMetadata.effectiveFrom, metadata.effectiveFrom) shouldBe true
    isSameToMillis(convertedMetadata.effectiveTo.get, closingTime) shouldBe true
    convertedMetadata.isCurrent shouldBe false
  }

  "documentToDim" should "convert Document to StationDim correctly" in {
    val now = ZonedDateTime.now()
    val doc = new Document()
      .append(DocFields.Dim.TIMESTAMP_UPDATED, Date.from(now.toInstant))
      .append(DocFields.Dim.STATION_ID, 123)
      .append(DocFields.Dim.NAME, "Test Station")
      .append(DocFields.Dim.NAME_ZH, "測試站")
      .append(DocFields.Dim.ADDRESS, "123 Test St")
      .append(DocFields.Dim.ADDRESS_ZH, "測試街123號")
      .append(DocFields.Dim.LATITUDE, 25.123)
      .append(DocFields.Dim.LONGITUDE, 121.456)
      .append(DocFields.Dim.TOTAL_DOCKS, 20)
      .append(DocFields.Dim.DISTRICT_ZH, "測試區")
      .append(DocFields.Dim.IS_ACTIVE, true)
      .append(DocFields.Metadata.EFFECTIVE_FROM, Date.from(now.toInstant))
      .append(DocFields.Metadata.EFFECTIVE_TO, null)
      .append(DocFields.Metadata.IS_CURRENT, true)

    val dim = DocumentConverter.documentToDim(doc)
    isSameToMillis(dim.timestampUpdated, now) shouldBe true
    dim.stationId shouldBe 123
    dim.name shouldBe "Test Station"
    dim.nameZh shouldBe "測試站"
    dim.address shouldBe "123 Test St"
    dim.addressZh shouldBe "測試街123號"
    dim.latitude shouldBe 25.123
    dim.longitude shouldBe 121.456
    dim.totalDocks shouldBe 20
    dim.districtZh shouldBe "測試區"
    dim.isActive shouldBe true

    val metadata = DocumentConverter.documentToSCDMetadata(doc)
    isSameToMillis(metadata.effectiveFrom, now) shouldBe true
    metadata.effectiveTo shouldBe None
    metadata.isCurrent shouldBe true
  }
}
