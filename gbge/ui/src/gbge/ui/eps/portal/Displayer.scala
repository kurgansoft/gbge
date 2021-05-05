package gbge.ui.eps.portal

import gbge.ui.eps.player.ClientState
import gbge.ui.eps.spectator.{Screens0, SpectatorState}
import japgolly.scalajs.react.vdom.all.{pre, _}
import japgolly.scalajs.react.{Callback, ScalaComponent}

object Displayer {
  val portalComponent = ScalaComponent.builder[(PortalState, PortalCommander)]("PortalComponent").
    render_P(t => {
      val portalState = t._1
      val commander = t._2
      portalState.clientState match {
        case Some(SpectatorState(frontendUniverse, wsConnectionStatus, offlineState, _)) => {
          Screens0.root(SpectatorState(frontendUniverse, wsConnectionStatus, offlineState), commander.createPortalSubCommander())
        }
        case Some(ClientState(frontendUniverse, you, offlineState, tab, _)) => {
          gbge.ui.display.Displayer.rootComponent(
            (ClientState(frontendUniverse, you, offlineState, tab), commander.createPortalSubCommander())
          ).vdomElement
        }
        case _ => {
          div(color:="yellow",
            h1("Portal component"),
            div("portal id is: " + portalState.portalId),
          )
        }
      }
    } ).build
}
