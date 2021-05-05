package gbge.ui.eps.tm

import gbge.client.{ClientEvent, AbstractCommander, UIState}
import gbge.shared.tm.TMMessage
import gbge.ui.Urls
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.WebSocket

import scala.util.Try

class TMSubCommander(val commander: TMCommander, var uiState: UIState[_ <: ClientEvent]) extends AbstractCommander[ClientEvent] {

  override def setState(state: UIState[ClientEvent]): Unit = {
    uiState = state
  }

  override def getState(): UIState[ClientEvent] = uiState.asInstanceOf[UIState[ClientEvent]]

  override def render(): Unit = commander.addAnEventToTheEventQueue(NewStateFromSubCommander(uiState))

  override def handleEventsFromQueue(): Unit = super.handleEventsFromQueueWithoutEffectExecution()

}

case class TMCommander(val div: Div, var state: TimeMachineState) extends AbstractCommander[TMClientEvent] {

  var portalSocket: WebSocket = _

  override def setState(newState: UIState[TMClientEvent]): Unit = {
    state = newState.asInstanceOf[TimeMachineState]
  }

  override def getState(): UIState[TMClientEvent] = state

  def createTMSubCommander(): TMSubCommander = {
    assert(state.selectedClientState.isRight)
    val uiState = state.selectedClientState.getOrElse(null).asInstanceOf[UIState[ClientEvent]]
    new TMSubCommander(this, uiState)
  }

  def render(): Unit = {
    Displayer.tmComponent((state, this)).renderIntoDOM(div)
  }

  def createWebSocketConnection(portalId: Option[Int] = None): Unit = {
    val url = if (portalId.isDefined) {
      Urls.portalSocketURL + "/" + portalId.get
    } else {
      Urls.portalSocketURL + "/0"
    }
    portalSocket = new WebSocket(url)
    portalSocket.onmessage = message => {
      val id = Try(upickle.default.read[TMMessage](message.data.toString))
      if (id.isSuccess) {
        import gbge.shared.tm.PortalId
        id.get match {
          case PortalId(id) => {
            this.addAnEventToTheEventQueue(TMMessageContainer(PortalId(id)))
          }
          case _ =>
        }
      }
    }
    portalSocket.onclose = event => {
      println("websocket connection broke down...")
    }
    portalSocket.onopen = event => {
      println("this is the event: " + event)
    }
  }

  addAnEventToTheEventQueue(Start)
}