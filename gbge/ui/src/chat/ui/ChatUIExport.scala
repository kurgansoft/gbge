package chat.ui

import chat.shared.ClientChatGame
import gbge.client.{ClientEvent, ClientEventHandler, ClientResult, OK}
import gbge.shared.{FrontendPlayer, FrontendUniverse}
import gbge.ui.UIExport
import gbge.ui.eps.player.ClientState
import gbge.ui.eps.spectator.SpectatorState
import japgolly.scalajs.react.{Callback, ReactEventFromInput}
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom.html.{Div, Input}

object ChatUIExport extends UIExport {

  override val handleNewFU: (ClientState, FrontendUniverse) => (ClientState, ClientResult) = (clientState, newFU) => {
    if (clientState.offlineState != ChatOfflineState)
      (clientState.copy(offlineState = ChatOfflineState, frontendUniverse = Some(newFU)), OK)
    else
      (clientState.copy(frontendUniverse = Some(newFU)), OK)
  }
  override val spectatorDisplayer: (SpectatorState, ClientEventHandler[ClientEvent]) => VdomTagOf[Div] = (state, _) => {
//    assert(state.frontendUniverse.isDefined)
    val fu = state.frontendUniverse.get
//    assert(fu.game.isDefined)
    val chat = fu.game.get.asInstanceOf[ClientChatGame]
    div(
      h1(color:="yellow", textAlign:="center", "CHAT"),
      div(display:="flex", flexDirection:="row",
        div(flex:="3 3", marginRight:="10px", marginLeft:="10px",
          h1(color:="yellow", textAlign:="center", textDecoration:="underline", "MESSAGES:"),
          chat.messages.map(message => {
            val x2 = FrontendPlayer.getNameOfPlayerWithRole(message.roleNumber)(fu.players)
            Directives.messageDisplayer(x2, message.message)
          }).toTagMod
        ),
        Directives.players(fu.players)(flex:="1 1", marginRight:="10px")
      )
    )
  }

  override val playerDisplayer: (ClientState, ClientEventHandler[ClientEvent]) => VdomTagOf[Div] = (state, commander) => {
    def cb(e: ReactEventFromInput): Callback = Callback {
      val theMessage = e.target.parentElement.childNodes(0).asInstanceOf[Input].value
      println("content of the input field: " + theMessage + "[inserting into the event queue]")
      commander.addAnEventToTheEventQueue(SendMessageEvent(theMessage))
    }

    val yourRole: Option[Int] = state.you.flatMap(_.role)
    //    assert(state.frontendUniverse.isDefined)
    val fu = state.frontendUniverse.get
    //    assert(fu.game.isDefined)
    val chat = fu.game.get.asInstanceOf[ClientChatGame]
    div(
      h1(color:="yellow", textAlign:="center", textDecoration:="underline", "MESSAGES:"),
      chat.messages.map(message => {
        val x2 = FrontendPlayer.getNameOfPlayerWithRole(message.roleNumber)(fu.players)
        Directives.messageDisplayer(x2, message.message)
      }).toTagMod,
      div(color:="yellow", position:="absolute", bottom:="0px",
        if (yourRole.nonEmpty) {
          div(
            input(`type`:="text"),
            button("SEND", `class` := "btn btn-primary", onClick ==> cb)
          )
        } else {
          div("You have no role in this game.")
        }
      )
    )
  }
}
