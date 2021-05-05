package gbge.client

trait UIState[Event <: ClientEvent] {
  def processClientEvent(clientEvent: Event): (UIState[Event], ClientResult)
}
