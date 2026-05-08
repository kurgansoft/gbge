package gbge.ui.eps

sealed trait SSEStatus

object SSEStatus {
  case object NOT_YET_ESTABLISHED extends SSEStatus
  case object CONNECTED extends SSEStatus
  case object BROKEN extends SSEStatus  
}

