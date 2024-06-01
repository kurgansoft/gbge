package chat.backend.launchers

import chat.backend.ChatGame
import chat.shared.ClientChatGame
import gbge.backend.server.CustomServer

object Launcher extends CustomServer {
  gbge.shared.RG.registeredGames = List(ClientChatGame)
  gbge.backend.RG.registeredGames = List(ChatGame)

  System.setProperty("tmEnabled", "true")

  assert(gbge.backend.RG.registeredGames.size == gbge.shared.RG.registeredGames.size)
  gbge.backend.RG.registeredGames.zip(gbge.shared.RG.registeredGames).foreach(a => {
    assert(a._1.frontendGame == a._2)
  })

  override val jsLocation: Option[String] = Some("ui/target/scala-3.3.0/")
}
