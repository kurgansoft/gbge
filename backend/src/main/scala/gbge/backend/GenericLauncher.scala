package gbge.backend

import gbge.backend.endpoints_and_aspects.Aspects
import gbge.backend.gameroutes.{GameRoutes, StaticRoutes, TMRoutes}
import gbge.backend.models.{Player, Universe}
import gbge.backend.services.state_manager.{TimeMachineContent, TimeMachineStateManager}
import gbge.backend.services.{MainService, MainServiceLive, SequentialTokenGenerator}
import gbge.shared.tm.ActionStackInTransit
import zio.http.{Route, Routes, Server}
import zio.stream.SubscriptionRef
import zio.{IO, Ref, Scope, ZEnvironment, ZIO, ZLayer}

import java.net.InetSocketAddress

case class GenericLauncher(games: Seq[BackendGameProps[_,_]]) {

  val launch: ZIO[Scope & GameConfig & Option[String], Any, Unit] = for {
    gameConfig <- ZIO.service[GameConfig]
    optionalFileNameForRecovery <- ZIO.service[Option[String]]
    universeRef: SubscriptionRef[Universe] <- SubscriptionRef.make(Universe(Seq.empty)) // first value does not matter

    optionalTmContent <- ZIO.when(optionalFileNameForRecovery.isDefined)(reconstructTmContentFromFile(optionalFileNameForRecovery.get))

    tmStateManager <- optionalTmContent match {
      case Some(timeMachineContent: TimeMachineContent) =>
        for {
          _ <- ZIO.log(s"The recovered tm content: [$timeMachineContent]")
          _ <- universeRef.set(timeMachineContent.latestUniverse)
          result <- TimeMachineStateManager.layerFromRecoveredTmContent.build.provideSomeEnvironment[Scope](scope =>
            scope ++ ZEnvironment(games).add(timeMachineContent))
        } yield result.get
      case None =>
        ZIO.log("nothing was recovered...") *>
        TimeMachineStateManager.layer.build.provideSomeEnvironment[Scope](scope => scope ++ ZEnvironment(games)).map(_.get)
    }

    _ <- tmStateManager.universeStream.foreach(u => universeRef.set(u)).fork

    u <- universeRef.get
    tokenValueZero: Int = {
      if (u.players.isEmpty)
        100
      else
        u.players.keys.map(_.toInt).max
    } // Will require some redesign when support for non-sequential TokenGenerator will be added
    _ <- ZIO.log("Token value 0: " + tokenValueZero)
    tokenGenerator <- SequentialTokenGenerator.layer.build.provideSomeLayer(ZLayer.succeed(tokenValueZero))
    mainService <- MainServiceLive.layer.build.provideSomeEnvironment[Scope](scope => scope.add(tmStateManager) ++ tokenGenerator.add(games))

    routesWithDepsProvided = createRoutes(gameConfig.devStaticRouteOptions, gameConfig.timeMachineEnabled).provideEnvironment(ZEnvironment(universeRef).add(tmStateManager) ++ mainService)

    socketAddress = gameConfig.host.fold(new InetSocketAddress(gameConfig.port))(host =>
      new InetSocketAddress(host, gameConfig.port))

    _ <- printStartUpMessage(gameConfig)
    serverLayer = Server.defaultWith(config => config.copy(address = socketAddress))

    _ <- Server.serve(routesWithDepsProvided).provide(serverLayer)
    _ <- ZIO.never
  } yield ()

  private def reconstructTmContentFromFile(fileName: String): IO[Unit, TimeMachineContent] = for {
    _ <- ZIO.log(s"Attempting to rebuildActionStack from file [$fileName]")
    path = os.pwd / fileName
    raw = os.read(path)
    actionStackInTransit = ActionStackInTransit.jsonCodec.decoder.decodeJson(raw).getOrElse(???)
    _ <- ZIO.log(s"decoded actionStackInTransit:\n$actionStackInTransit")
  } yield TimeMachineContent.rebuildFromActionStack(
    games,
    actionStackInTransit.toActionEntries()(games.map(_.actionCodec).toList).map(aip => (aip.action, aip.invoker))
  )

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
  } yield ()).ignore


  private def createRoutes(optionalDevStaticRouteOptions: Option[StaticRoutes.DevStaticRouteOptions] = None, timeMachineEnabled: Boolean) = {
    val staticRoutes = StaticRoutes(optionalDevStaticRouteOptions)

    val gameSpecificActionRoutes = Routes.fromIterable(games.map(GameRoutes.generateGameSpecificActionRoute))
    
    val developmentRoutes = Routes(
      TMRoutes.resetRoute,
      TMRoutes.actionHistoryRoute,
      TMRoutes.getTmSpectatorStateAtTimeRoute,
      TMRoutes.getTmStateAtTimeForPlayerRoute,
      TMRoutes.saveRoute
    )

    val routesWithAuthentication: Routes[MainService & SubscriptionRef[Universe], Nothing] =
      (Routes(
        GameRoutes.playerRoute,
        GameRoutes.performGeneralActionRoute,
        GameRoutes.sseRouteWithAuthentication,
      ) ++ gameSpecificActionRoutes) @@ Aspects.tokenExtractorAspect

    val routes = Routes(GameRoutes.joinRoute, GameRoutes.sseRoute, GameRoutes.fuRoute)
      ++ routesWithAuthentication ++ staticRoutes.allStaticRoutes
    
    if (timeMachineEnabled)
      routes ++ developmentRoutes
    else 
      routes
  }

}
