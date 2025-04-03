package services.converter

import model.Station
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.time.format.DateTimeFormatter
import java.time.{ LocalDateTime, ZoneId, ZonedDateTime }

class ModelConverterSpec extends AnyFlatSpec with Matchers {
  val retrievedTime: ZonedDateTime = ZonedDateTime.now()
  val station:       Station       = Station(
    id = "123",
    nameZh = "測試站",
    districtZh = "測試區",
    mday = "2025-02-28",
    addrZh = "測試街123號",
    district = "Test District",
    name = "Test Station",
    addr = "123 Test St",
    act = "1",
    srcUpdateTime = "2025-02-28 10:00:00",
    updateTime = "2025-02-28 10:00:00",
    infoTime = "2025-02-28 10:00:00",
    infoDate = "2025-02-28",
    total = 20,
    availableRentBikes = 15,
    latitude = 25.123,
    longitude = 121.456,
    availableReturnBikes = 5
  )

  "stationToDim" should "convert Station to StationDim" in {
    val stationDim = ModelConverter.stationToDim(station, retrievedTime)
    stationDim.timestampUpdated shouldBe retrievedTime
    stationDim.stationId shouldBe 123
    stationDim.name shouldBe "Test Station"
    stationDim.nameZh shouldBe "測試站"
    stationDim.address shouldBe "123 Test St"
    stationDim.addressZh shouldBe "測試街123號"
    stationDim.latitude shouldBe 25.123
    stationDim.longitude shouldBe 121.456
    stationDim.totalDocks shouldBe 20
    stationDim.districtZh shouldBe "測試區"
    stationDim.isActive shouldBe true
  }

  "stationToFct" should "convert Station to StationFct" in {
    val stationFct        = ModelConverter.stationToFct(station, retrievedTime)
    val expectedTimestamp = LocalDateTime
      .parse("2025-02-28 10:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
      .atZone(ZoneId.of("Asia/Taipei"))
      .withZoneSameInstant(ZoneId.of("UTC"))
    stationFct.timestampFetched shouldBe retrievedTime
    stationFct.stationId shouldBe 123
    stationFct.stationDistrict shouldBe "Test District"
    stationFct.isActive shouldBe true
    stationFct.availableBikes shouldBe 15
    stationFct.availableDocks shouldBe 5
    stationFct.timestampSrcUpdated.isEqual(expectedTimestamp) shouldBe true
  }

  "stationToDim and stationToFact" should "handle inactive stations correctly" in {
    val inactiveStation = station.copy(act = "0")
    val stationDim      = ModelConverter.stationToDim(inactiveStation, retrievedTime)
    val stationFct      = ModelConverter.stationToFct(inactiveStation, retrievedTime)

    stationDim.isActive shouldBe false
    stationFct.isActive shouldBe false
  }
}
