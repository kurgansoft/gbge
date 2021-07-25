package chat.ui

import chat.shared.ClientChatGame
import gbge.ui.EntryPoint

import scala.scalajs.js.annotation.JSExportTopLevel

//@JSExportTopLevel("ep")
object ChatUILauncher extends EntryPoint {
  gbge.shared.RG.registeredGames = List(ClientChatGame)
  gbge.ui.RG.registeredGames = List(ChatUIExport)
}
