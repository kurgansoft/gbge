package gbge.backend

import gbge.backend.endpoints_and_aspects.Aspects
import gbge.backend.gameroutes.{MyRoutes, TMRoutes}
import gbge.backend.models.{Player, Universe}
import gbge.backend.services.state_manager.TimeMachineStateManager
import gbge.backend.services.{MainService, MainServiceLive, SequentialTokenGenerator}
import zio.http.{Route, Routes, Server}
import zio.stream.SubscriptionRef
import zio.{Ref, Scope, ZEnvironment, ZIO, ZLayer}

case class GenericLauncher(games: Seq[BackendGameProps[_,_]], uiAssetsFolder: String, mainFilePath: String) {
  private val myRoutes = MyRoutes(uiAssetsFolder, mainFilePath)

  private val gameSpecificActionRoutes = Routes.fromIterable(games.map(myRoutes.generateGameSpecificActionRoute))
  
  private val routesWithAuthentication: Routes[MainService & SubscriptionRef[Universe], Nothing] =
    (Routes(
      myRoutes.playerRoute,
      myRoutes.performGeneralActionRoute,
      myRoutes.sseRouteWithAuthentication,
    ) ++ gameSpecificActionRoutes) @@ Aspects.tokenExtractorAspect

  private val routes = Routes(TMRoutes.resetRoute, myRoutes.joinRoute, myRoutes.sseRoute, myRoutes.mainJSRoute, myRoutes.fuRoute)
    ++ routesWithAuthentication ++ myRoutes.staticAssetsRoute ++ Routes(myRoutes.redirect1, myRoutes.redirect2)

  val launch: ZIO[Scope, Any, Unit] = for {
    universeRef: SubscriptionRef[Universe] <- SubscriptionRef.make(Universe(Seq.empty)) // first value does not matter
    tmStateManager <- TimeMachineStateManager.layer.build.provideSomeEnvironment[Scope](scope => scope ++ ZEnvironment(games))
    _ <- tmStateManager.get.universeStream.foreach(u => universeRef.set(u)).fork
    tokenGenerator <- SequentialTokenGenerator.layer.build.provideSomeLayer(ZLayer.succeed(100))
    mainService <- MainServiceLive.layer.build.provideSomeEnvironment[Scope](scope => scope ++ tmStateManager ++ tokenGenerator.add(games))

    routesWithDepsProvided = routes.provideEnvironment(ZEnvironment(universeRef) ++ mainService ++ tmStateManager)

    _ <- Server.serve(routesWithDepsProvided).provide(Server.default)
    _ <- ZIO.never
  } yield ()

}
