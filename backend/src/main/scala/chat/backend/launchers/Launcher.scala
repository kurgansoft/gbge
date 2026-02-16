package chat.backend.launchers

import chat.backend.BackendChatGameProps
import gbge.backend.gameroutes.StaticRoutes
import gbge.backend.gameroutes.StaticRoutes.DevStaticRouteOptions
import gbge.backend.{BackendGameProps, GameConfig, GenericLauncher}
import zio.{
  ConfigProvider,
  Runtime,
  Scope,
  ZIO,
  ZIOAppArgs,
  ZIOAppDefault,
  ZLayer
}

object Launcher extends ZIOAppDefault {
  
  private val games: Seq[BackendGameProps[_,_]] = Seq(BackendChatGameProps)

  private val gl = GenericLauncher(games)

//  val config: GameConfig = GameConfig(
//    devStaticRouteOptions = Some(DevStaticRouteOptions(
//      "absolute_path_to_ui-fastopt.js",
//    ))
//  )
  
  override def run: ZIO[Scope & ZIOAppArgs, Any, Unit] = for {
    args <- ZIO.serviceWith[ZIOAppArgs](_.getArgs)
    config <- GameConfig.resolveConfig
    _ <- ZIO.log(s"The resolved config is: [$config]")
    _ <- gl.launch.provideSomeEnvironment[Scope](scope =>
      scope.add(config).add(args.headOption)
    )
  } yield ()

  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    Runtime.setConfigProvider(ConfigProvider.propsProvider)

}

