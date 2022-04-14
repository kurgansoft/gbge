package gbge.ui.display

import gbge.ui.eps.player.ClientState
import gbge.ui.eps.spectator.{Screens0, SpectatorState}
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom.html.Div
import uiglue.{Event, EventLoop}

object Displayer {

  val rootComponent = ScalaComponent.builder[(ClientState, uiglue.EventLoop.EventHandler[Event])]("RootComponent")
    .render_P(t => {
      div(position:= "fixed", top:="0px", bottom:="0px", left:="0px", right:="0px",
        displayer(t._1, t._2)
      )
    })
    .build

  val spectatorRootComponent = ScalaComponent.builder[(SpectatorState, EventLoop.EventHandler[Event])]("SpectatorRootComponent")
    .render_P(t => {
      div(position:= "fixed", top:="0px", bottom:="0px", left:="0px", right:="0px",
        Screens0.root(t._1, t._2)
      )
    })
    .build

  def displayer(state: ClientState, eventHandler: EventLoop.EventHandler[Event]): TagOf[Div] = {
    if (state.you.isEmpty)
      Screens.joinScreen(state, eventHandler)
    else {
      val stateDiv = state.tab match {
        case 1 =>
          if (state.frontendUniverse.isEmpty) {
            div(color:="yellow", "LOADING...")
          } else if (state.frontendUniverse.get.game.isDefined) {
            state.getCurrentGame.get.playerDisplayer(state, eventHandler)
          } else
            Screens.welcomeScreen(state, eventHandler)
        case 2 => Screens.metaScreen(state, eventHandler)
        case 3 => Screens.adminScreen(state, eventHandler)
        case 4 => Screens.kickScreen(state, eventHandler)
        case 5 => Screens.playerRoleScreen(state, eventHandler)
        case 6 => Screens.delegateAdminRoleScreen(state, eventHandler)
      }
      div(position:= "absolute", top:= "0px", bottom:= "0px", left:= "0px", right:= "0px", display:="flex", flexDirection:="column",
        Directives.tabMenu(state, eventHandler)(flex:="0 1 auto"),
        stateDiv(flex:="1 1 auto")
      )
    }
  }
}
