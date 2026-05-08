package gbge.ui.display

import gbge.ui.eps.SSEStatus
import gbge.ui.eps.SSEStatus.{BROKEN, NOT_YET_ESTABLISHED}
import gbge.ui.eps.player.{ClientState, Reconnect}
import gbge.ui.eps.spectator.{Screens0, SpectatorState}
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.all.*
import japgolly.scalajs.react.{Callback, ScalaComponent}
import org.scalajs.dom.html.Div
import uiglue.{Event, EventLoop}

object Displayer {

  val rootComponent = ScalaComponent.builder[(ClientState, uiglue.EventLoop.EventHandler[Event])]("RootComponent")
    .render_P({case (clientState, eventHandler) =>
      div(position:= "fixed", top:="0px", bottom:="0px", left:="0px", right:="0px",
        displayer(clientState, eventHandler)
      )
    })
    .build

  val spectatorRootComponent = ScalaComponent.builder[(SpectatorState, EventLoop.EventHandler[Event])]("SpectatorRootComponent")
    .render_P({case (clientState, eventHandler) =>
      div(position:= "fixed", top:="0px", bottom:="0px", left:="0px", right:="0px",
        Screens0.root(clientState, eventHandler)
      )
    })
    .build

  def displayer(state: ClientState, eventHandler: EventLoop.EventHandler[Event]): TagOf[Div] = {
    if (state.you.isEmpty)
      Screens.joinScreen(state, eventHandler)
    else {
      state.sseStreamStatus match {
        case NOT_YET_ESTABLISHED => div(h1("Connection is NOT YET ESTABLISHED", color:="yellow"))
        case BROKEN => div(
          h1("Connection is BROKEN", color:="yellow"),
          button(`class`:="btn btn-primary", "RECONNECT", onClick --> Callback {
            eventHandler(Reconnect)
          })
        )
        case SSEStatus.CONNECTED => {
          val stateDiv = state.tab match {
            case 1 =>
              if (state.frontendUniverse.isEmpty) {
                div(color := "yellow", "LOADING...")
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
          div(position := "absolute", top := "0px", bottom := "0px", left := "0px", right := "0px", display := "flex", flexDirection := "column",
            Directives.tabMenu(state, eventHandler)(flex := "0 1 auto"),
            stateDiv(flex := "1 1 auto")
          )
        }
      }

    }
  }
}
