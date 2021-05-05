package gbge.ui.eps.player

import gbge.client._
import gbge.ui.display.Displayer
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.WebSocket

class Commander(val div: Div, var state: StandardStateWrapper) extends AbstractCommander[ClientEvent] {

  override def getState(): UIState[ClientEvent] = state

  override def setState(newState: UIState[ClientEvent]): Unit = {
    state = newState.asInstanceOf[StandardStateWrapper]
  }

  var stateSocket: WebSocket = _

  def render(): Unit = {
    Displayer.rootComponent((state.clientState, this)).renderIntoDOM(div)
  }

  addAnEventToTheEventQueue(BootstrapPlayerEvent)
}
