package chat.ui

import chat.shared.ClientChatGame
import gbge.shared.FrontendGame
import gbge.shared.actions.GameAction
import gbge.ui.EntryPoint
import zio.json.JsonCodec

import scala.scalajs.js.annotation.JSExportTopLevel

// You need to uncomment this annotation if you want to use the chat game.
// By default it is commented out, because it causes a linker error if
// you try to register multiple games.
//@JSExportTopLevel("ep")
object ChatUILauncher extends EntryPoint {
  gbge.shared.RG.gameCodecs = List(ClientChatGame.codec.asInstanceOf[JsonCodec[FrontendGame[_ <: GameAction]]])
  gbge.ui.RG.registeredGames = List(ClientChatGameProps)
}
