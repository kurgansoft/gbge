package gbge.client

trait ClientEventHandler[-Event <: ClientEvent] {

  def addAnEventToTheEventQueue(event: Event): Unit

  def addEventsToTheEventQueue(events: List[Event]): Unit
}