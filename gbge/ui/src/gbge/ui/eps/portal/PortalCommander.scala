package gbge.ui.eps.portal

import gbge.client.{ClientEvent, AbstractCommander, UIState}
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.{Event, WebSocket}

class PortalSubCommander(val commander: PortalCommander, var uiState: UIState[_ <: ClientEvent]) extends AbstractCommander[ClientEvent] {

  override def setState(state: UIState[ClientEvent]): Unit = {
    uiState = state
  }

  override def getState(): UIState[ClientEvent] = uiState.asInstanceOf[UIState[ClientEvent]]

  override def render(): Unit = commander.addAnEventToTheEventQueue(NewStateFromSubCommander(uiState))

  override def handleEventsFromQueue(): Unit = super.handleEventsFromQueueWithoutEffectExecution()
}

case class PortalCommander(val div: Div, var state: PortalState) extends AbstractCommander[PortalClientEvent] {

  var portalSocket: WebSocket = _

  override def setState(newState: UIState[PortalClientEvent]): Unit = {
    state = newState.asInstanceOf[PortalState]
  }

  override def getState(): UIState[PortalClientEvent] = state

  def createPortalSubCommander(): PortalSubCommander = {
    assert(state.clientState.isDefined)
    val uiState = state.clientState.get.asInstanceOf[UIState[ClientEvent]]
    new PortalSubCommander(this, uiState)
  }

  def render(): Unit = {
    Displayer.portalComponent((state, this)).renderIntoDOM(div)
  }

  addAnEventToTheEventQueue(Start)
}
