package services

import model.{ SCDMetadata, StationDim }

import java.time.ZonedDateTime

case class ClosedMetadataValues(isCurrent: Boolean, effectiveTo: ZonedDateTime)

class SCDService {

  def createNewSCDMetadata(timestamp: ZonedDateTime): SCDMetadata =
    SCDMetadata(effectiveFrom = timestamp, effectiveTo = None, isCurrent = true)

  def getClosedMetadataValues(closingTime: ZonedDateTime): ClosedMetadataValues =
    ClosedMetadataValues(
      effectiveTo = closingTime,
      isCurrent = false
    )

  def hasAttributesChanged(currentDim: StationDim, newDim: StationDim): Boolean =
    currentDim.name != newDim.name ||
      currentDim.nameZh != newDim.nameZh ||
      currentDim.address != newDim.address ||
      currentDim.addressZh != newDim.addressZh ||
      currentDim.latitude != newDim.latitude ||
      currentDim.longitude != newDim.longitude ||
      currentDim.totalDocks != newDim.totalDocks ||
      currentDim.districtZh != newDim.districtZh ||
      currentDim.isActive != newDim.isActive
}
