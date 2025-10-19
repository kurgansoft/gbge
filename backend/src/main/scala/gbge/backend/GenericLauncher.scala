package gbge.backend

import gbge.backend.endpoints_and_aspects.Aspects
import gbge.backend.gameroutes.{GameRoutes, StaticRoutes, TMRoutes}
import gbge.backend.models.{Player, Universe}
import gbge.backend.services.state_manager.TimeMachineStateManager
import gbge.backend.services.{MainService, MainServiceLive, SequentialTokenGenerator}
import zio.http.{Route, Routes, Server}
import zio.stream.SubscriptionRef
import zio.{IO, Ref, Scope, ZEnvironment, ZIO, ZLayer}

import java.net.InetSocketAddress

case class GenericLauncher(games: Seq[BackendGameProps[_,_]]) {

  val launch: ZIO[Scope & GameConfig, Any, Unit] = for {
    gameConfig <- ZIO.service[GameConfig]
    universeRef: SubscriptionRef[Universe] <- SubscriptionRef.make(Universe(Seq.empty)) // first value does not matter
    tmStateManager <- TimeMachineStateManager.layer.build.provideSomeEnvironment[Scope](scope => scope ++ ZEnvironment(games))
    _ <- tmStateManager.get.universeStream.foreach(u => universeRef.set(u)).fork
    tokenGenerator <- SequentialTokenGenerator.layer.build.provideSomeLayer(ZLayer.succeed(100))
    mainService <- MainServiceLive.layer.build.provideSomeEnvironment[Scope](scope => scope ++ tmStateManager ++ tokenGenerator.add(games))

    routesWithDepsProvided = createRoutes(gameConfig.devStaticRouteOptions).provideEnvironment(ZEnvironment(universeRef) ++ mainService ++ tmStateManager)

    socketAddress = gameConfig.host.fold(new InetSocketAddress(gameConfig.port))(host =>
      new InetSocketAddress(host, gameConfig.port))

    _ <- printStartUpMessage(gameConfig)
    serverLayer = Server.defaultWith(config => config.copy(address = socketAddress))

    _ <- Server.serve(routesWithDepsProvided).provide(serverLayer)
    _ <- ZIO.never
  } yield ()

  private def printStartUpMessage(config: GameConfig): IO[Nothing, Unit] = (for {
    _ <- zio.Console.printLine("**************************************************************")
    port = config.port
    _ <- zio.Console.printLine(s"\t Starting server with port [$port].")
    address = config.host match {
      case None => s"http://localhost:$port"
      case Some(hostName) => s"http://$hostName:$port"
    }
    _ <- zio.Console.printLine(s"\t Visit [$address] to join the game.")
    _ <- zio.Console.printLine(s"\t Visit [$address/s] to display the game board.")
    _ <- zio.Console.printLine("**************************************************************")
  } yield ()).either.unit


  private def createRoutes(optionalDevStaticRouteOptions: Option[StaticRoutes.DevStaticRouteOptions] = None) = {
    val staticRoutes = StaticRoutes(optionalDevStaticRouteOptions)

    val gameSpecificActionRoutes = Routes.fromIterable(games.map(GameRoutes.generateGameSpecificActionRoute))

    val routesWithAuthentication: Routes[MainService & SubscriptionRef[Universe], Nothing] =
      (Routes(
        GameRoutes.playerRoute,
        GameRoutes.performGeneralActionRoute,
        GameRoutes.sseRouteWithAuthentication,
      ) ++ gameSpecificActionRoutes) @@ Aspects.tokenExtractorAspect

    Routes(TMRoutes.resetRoute, GameRoutes.joinRoute, GameRoutes.sseRoute, GameRoutes.fuRoute)
      ++ routesWithAuthentication ++ staticRoutes.allStaticRoutes
  }

}
