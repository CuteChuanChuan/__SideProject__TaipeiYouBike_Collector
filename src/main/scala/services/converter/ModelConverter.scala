package services.converter

import model.{ Station, StationDim, StationFct }

import java.time.format.DateTimeFormatter
import java.time.{ LocalDateTime, ZoneId, ZonedDateTime }

/** Object responsible for converting data from API to models
  */
object ModelConverter {

  def stationToDim(station: Station, retrievedTime: ZonedDateTime): StationDim =
    StationDim(
      timestampUpdated = retrievedTime,
      stationId = station.id.toInt,
      name = station.name,
      nameZh = station.nameZh,
      address = station.addr,
      addressZh = station.addrZh,
      latitude = station.latitude,
      longitude = station.longitude,
      totalDocks = station.total,
      districtZh = station.districtZh,
      isActive = station.act == "1"
    )

  def stationToFct(station: Station, retrievedTime: ZonedDateTime): StationFct = {
    val localDateTime =
      LocalDateTime.parse(station.srcUpdateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    val timestampSourceUpdated =
      localDateTime.atZone(ZoneId.of("Asia/Taipei")).withZoneSameInstant(ZoneId.of("UTC"))

    StationFct(
      timestampFetched = retrievedTime,
      stationId = station.id.toInt,
      stationDistrict = station.district,
      isActive = station.act == "1",
      availableBikes = station.availableRentBikes,
      availableDocks = station.availableReturnBikes,
      timestampSrcUpdated = timestampSourceUpdated
    )
  }
}
