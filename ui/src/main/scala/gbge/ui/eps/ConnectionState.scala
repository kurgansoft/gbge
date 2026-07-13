package gbge.ui.eps

import gbge.ui.eps.ConnectionStatus._

case class ConnectionState(
                            status: ConnectionStatus = NOT_YET_ESTABLISHED,
                            connectionTimestamps: List[Long] = List.empty,
                            disconnectionTimestamps: List[Long] = List.empty
                          ) {
  def transitionToConnectedStateWithTimeStamp(timestamp: Long): ConnectionState = {
    assert(status != CONNECTED)
    this.copy(
      status = CONNECTED,
      connectionTimestamps = connectionTimestamps.appended(timestamp)
    )
  }

  def addDisconnectionTimeStamp(timestamp: Long): ConnectionState = {
    assert(status != BROKEN)
    this.copy(
      status = BROKEN,
      disconnectionTimestamps = disconnectionTimestamps.appended(timestamp)
    )
  }

  lazy val isItWorthToTryReconnecting: Boolean = {
    assert(status != CONNECTED && disconnectionTimestamps.nonEmpty)
    val threshold = disconnectionTimestamps.last - 5000
    disconnectionTimestamps.count(_ >= threshold) < 3
  }
}
