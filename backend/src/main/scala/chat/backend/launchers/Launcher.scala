package chat.backend.launchers

import chat.backend.BackendChatGameProps
import gbge.backend.{BackendGameProps, GenericLauncher}
import zio.{Scope, ZIO, ZIOAppDefault}

object Launcher extends ZIOAppDefault {
  
  private val games: Seq[BackendGameProps[_,_]] = Seq(BackendChatGameProps)

  private val gl = GenericLauncher(
    games
  )

  override def run: ZIO[Scope, Any, Unit] = gl.launch
}

