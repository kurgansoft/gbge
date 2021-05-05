package gbge.ui.eps.spectator

import gbge.client._
import gbge.ui.display.Displayer
import gbge.ui.eps.player.SetupWSConnection
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.WebSocket

class SpectatorCommander(val div: Div, var state: SpectatorState) extends AbstractCommander[ClientEvent] {

  override def getState(): UIState[ClientEvent] = state

  override def setState(newState: UIState[ClientEvent]): Unit = {
    state = newState.asInstanceOf[SpectatorState]
  }

  var stateSocket: WebSocket = _

  def render(): Unit = {
    Displayer.spectatorRootComponent(state, this).renderIntoDOM(div)
  }

  addAnEventToTheEventQueue(SetupWSConnection)
}
