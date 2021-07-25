package chat.ui

import chat.shared.ClientChatGame
import gbge.ui.EntryPoint

import scala.scalajs.js.annotation.JSExportTopLevel

// You need to uncomment this annotation if you want to use the chat game.
// By default it is commented out, because it causes a linker error if
// you try to register an another game.
//@JSExportTopLevel("ep")
object ChatUILauncher extends EntryPoint {
  gbge.shared.RG.registeredGames = List(ClientChatGame)
  gbge.ui.RG.registeredGames = List(ChatUIExport)
}
