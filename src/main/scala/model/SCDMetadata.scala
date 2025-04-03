package model

import java.time.ZonedDateTime

case class SCDMetadata(
  effectiveFrom: ZonedDateTime,
  effectiveTo:   Option[ZonedDateTime],
  isCurrent:     Boolean
)
