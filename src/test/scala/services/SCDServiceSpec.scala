package services

import model.StationDim
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.ZonedDateTime

class SCDServiceSpec extends AnyFlatSpec with Matchers {
  val scdService = new SCDService()
  val timestamp: ZonedDateTime = ZonedDateTime.now()

  def createBaseDim(): StationDim =
    StationDim(
      timestampUpdated = timestamp,
      stationId = 1,
      name = "Test Station",
      nameZh = "測試站",
      address = "123 Test St",
      addressZh = "測試街123號",
      latitude = 1.0,
      longitude = 1.0,
      totalDocks = 1,
      districtZh = "測試區",
      isActive = true
    )

  "hasAttributesChanged" should "return false if no attributes have changed" in {
    val currentDim = createBaseDim()
    val newDim     = createBaseDim()

    scdService.hasAttributesChanged(currentDim, newDim) shouldBe false
  }

  it should "detect change in name" in {
    val currentDim = createBaseDim()
    val newDim     = createBaseDim().copy(name = "Changed Test Station")

    scdService.hasAttributesChanged(currentDim, newDim) shouldBe true
  }

  it should "detect change in name_zh" in {
    val currentDim = createBaseDim()
    val newDim     = createBaseDim().copy(nameZh = "變更測試站")

    scdService.hasAttributesChanged(currentDim, newDim) shouldBe true
  }

  it should "detect change in address" in {
    val currentDim = createBaseDim()
    val newDim     = createBaseDim().copy(address = "456 Changed St")

    scdService.hasAttributesChanged(currentDim, newDim) shouldBe true
  }

  it should "detect change in address_zh" in {
    val currentDim = createBaseDim()
    val newDim     = createBaseDim().copy(addressZh = "變更街456號")

    scdService.hasAttributesChanged(currentDim, newDim) shouldBe true
  }

  it should "detect change in latitude" in {
    val currentDim = createBaseDim()
    val newDim     = createBaseDim().copy(latitude = 25.1)

    scdService.hasAttributesChanged(currentDim, newDim) shouldBe true
  }

  it should "detect change in longitude" in {
    val currentDim = createBaseDim()
    val newDim     = createBaseDim().copy(longitude = 121.1)

    scdService.hasAttributesChanged(currentDim, newDim) shouldBe true
  }

  it should "detect change in total_docks" in {
    val currentDim = createBaseDim()
    val newDim     = createBaseDim().copy(totalDocks = 25)

    scdService.hasAttributesChanged(currentDim, newDim) shouldBe true
  }

  it should "detect change in district_zh" in {
    val currentDim = createBaseDim()
    val newDim     = createBaseDim().copy(districtZh = "新測試區")

    scdService.hasAttributesChanged(currentDim, newDim) shouldBe true
  }

  it should "detect change in isActive status" in {
    val currentDim = createBaseDim()
    val newDim     = createBaseDim().copy(isActive = false)

    scdService.hasAttributesChanged(currentDim, newDim) shouldBe true
  }

  it should "detect multiple attribute changes" in {
    val currentDim = createBaseDim()
    val newDim     = createBaseDim().copy(
      name = "Changed Station",
      totalDocks = 30,
      isActive = false
    )

    scdService.hasAttributesChanged(currentDim, newDim) shouldBe true
  }
}
