package chat.backend.launchers

import chat.backend.BackendChatGameProps
import gbge.backend.{BackendGameProps, GameConfig, GenericLauncher}
import zio.{Runtime, ConfigProvider, Scope, ZIO, ZIOAppDefault, ZLayer}

object Launcher extends ZIOAppDefault {
  
  private val games: Seq[BackendGameProps[_,_]] = Seq(BackendChatGameProps)

  private val gl = GenericLauncher(games)

  override def run: ZIO[Scope, Any, Unit] = for {
    config <- GameConfig.resolveConfig
    _ <- ZIO.log(s"The resolved config is: [$config]")
    _ <- gl.launch.provideSomeEnvironment[Scope](scope =>
      scope.add(config)
    )
  } yield ()

  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    Runtime.setConfigProvider(ConfigProvider.propsProvider)

}

