package gbge.ui.eps

import gbge.ui.eps.ConnectionStatus._

sealed trait ConnectionStatus

object ConnectionStatus {
  case object NOT_YET_ESTABLISHED extends ConnectionStatus
  case object CONNECTED extends ConnectionStatus
  case object BROKEN extends ConnectionStatus
}