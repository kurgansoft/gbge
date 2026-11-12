package chat.ui

import chat.shared.ClientChatGame
import gbge.shared.{FrontendGame, GameRole}
import gbge.shared.actions.GameAction
import gbge.ui.EntryPoint
import gbge.ui.token.{TokenService, URLBasedTokenService}
import zio.ZEnvironment
import zio.blocks.schema.json.JsonCodec

import scala.scalajs.js.annotation.JSExportTopLevel

// You need to uncomment this annotation if you want to use the chat game.
// By default it is commented out, because it causes a linker error if
// you try to register multiple games.
//@JSExportTopLevel("ep")
object ChatUILauncher extends EntryPoint {

  override val tokenService: ZEnvironment[TokenService] = ZEnvironment(
    URLBasedTokenService
  )

  gbge.shared.RG.gameCodecs = List(
    ClientChatGame.jsonCodec
      .asInstanceOf[
        JsonCodec[FrontendGame[? <: GameRole, ? <: GameAction]]
      ]
  )
  gbge.ui.RG.registeredGames = List(ClientChatGameProps)
}
