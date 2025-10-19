package gbge.backend

import gbge.backend.gameroutes.StaticRoutes.DevStaticRouteOptions
import zio.{Config, IO, ZIO}

case class GameConfig(
    host: Option[String] = None,
    port: Int = 8080,
    devStaticRouteOptions: Option[DevStaticRouteOptions] = None
)

object GameConfig {
  val resolveConfig: IO[Unit, GameConfig] = {
    val portConfig = Config.int("port").optional
    val hostConfig = Config.string("host").optional
    for {
      resolvedHost <- ZIO.config(hostConfig).orElseFail(())
      resolvedPort <- ZIO.config(portConfig).tapError(_ => ZIO.logError(s"Provided port value is invalid.")).orElseFail(())
      gameConfig <- (resolvedHost, resolvedPort) match {
        case (_, Some(port)) if port <= 0 || port > 65535 => ZIO.logError(s"Provided port value [$port] is invalid.") *> ZIO.fail(())
        case (None, None) => ZIO.succeed(GameConfig())
        case (None, Some(port)) => ZIO.succeed(GameConfig(port = port))
        case (Some(host), None) => ZIO.succeed(GameConfig(Some(host)))
        case (Some(host), Some(port)) => ZIO.succeed(GameConfig(Some(host), port))
      }
    } yield gameConfig
  }
}
