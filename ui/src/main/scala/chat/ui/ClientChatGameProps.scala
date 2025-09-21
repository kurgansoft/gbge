package chat.ui

import chat.shared.{ChatAction, ChatGameProps, ClientChatGame, SendMessage}
import gbge.client.DispatchActionWithToken
import gbge.shared.FrontendPlayer
import gbge.ui.ClientGameProps
import gbge.ui.eps.player.ClientState
import gbge.ui.eps.spectator.SpectatorState
import japgolly.scalajs.react.vdom.all.*
import japgolly.scalajs.react.{Callback, ReactEventFromInput}
import org.scalajs.dom.html.{Div, Input}
import uiglue.EventLoop.EventHandler

object ClientChatGameProps extends ChatGameProps with ClientGameProps[ChatAction, ClientChatGame] {

  override val spectatorDisplayer: (SpectatorState, EventHandler[uiglue.Event]) => VdomTagOf[Div] = (state, _) => {
    val fu = state.frontendUniverse.get
    val chat = fu.game.get.asInstanceOf[ClientChatGame]
    div(
      h1(color := "yellow", textAlign := "center", "CHAT"),
      div(display := "flex", flexDirection := "row",
        div(flex := "3 3", marginRight := "10px", marginLeft := "10px",
          h1(color := "yellow", textAlign := "center", textDecoration := "underline", "MESSAGES:"),
          chat.messages.map(message => {
            val x2 = FrontendPlayer.getNameOfPlayerWithRole(message.roleNumber)(fu.players)
            Directives.messageDisplayer(x2, message.message)
          }).toTagMod
        ),
        Directives.players(fu.players)(flex := "1 1", marginRight := "10px")
      )
    )
  }

  override val playerDisplayer: (ClientState, EventHandler[uiglue.Event]) => VdomTagOf[Div] = (state, eventHandler) => {
    def cb(e: ReactEventFromInput): Callback = Callback {
      val theMessage = e.target.parentElement.childNodes(0).asInstanceOf[Input].value
      val action = SendMessage(theMessage)
      eventHandler(DispatchActionWithToken(action))
    }

    val fu = state.frontendUniverse.get
    val chat = fu.game.get.asInstanceOf[ClientChatGame]

    div(
      h1(color := "yellow", textAlign := "center", textDecoration := "underline", "MESSAGES:"),
      chat.messages.map(message => {
        val x2 = FrontendPlayer.getNameOfPlayerWithRole(message.roleNumber)(fu.players)
        Directives.messageDisplayer(x2, message.message)
      }).toTagMod,
      div(color := "yellow", position := "absolute", bottom := "0px",
        if (state.yourRole.nonEmpty) {
          div(
            input(`type` := "text"),
            button("SEND", `class` := "btn btn-primary", onClick ==> cb)
          )
        } else {
          div("You have no role in this game.")
        }
      )
    )
  }
}
