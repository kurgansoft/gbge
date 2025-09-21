package gbge.backend.gameroutes

import gbge.backend.BackendGameProps
import gbge.backend.endpoints_and_aspects.GeneralEndpoints
import gbge.backend.models.{Player, Universe}
import gbge.backend.services.MainService
import gbge.shared.actions.Action
import zio.http.codec.*
import zio.http.*
import zio.json.EncoderOps
import zio.json.JsonEncoder.*
import zio.json.ast.Json
import zio.stream.{SubscriptionRef, ZPipeline, ZStream}
import zio.{Ref, ZEnvironment, ZIO}

import java.io.File

case class MyRoutes(uiAssetsFolder: String, mainFilePath: String) {

  val joinRoute: Route[MainService, Nothing] =
    GeneralEndpoints.joinEndpoint.implement(
      input => MainService.joinWithName(input).mapError(_.toString)
    )

  val fuRoute: Route[Ref[Universe], Nothing] = GeneralEndpoints.getFUEndpoint.implementHandler(
    Handler.fromZIO(
      for {
        uRef <- ZIO.service[Ref[Universe]]
        u <- uRef.get
      } yield u.getFrontendUniverseForPlayer(None).encode.toJsonPretty)
  )

  val sseRoute: Route[SubscriptionRef[Universe], Nothing] = GeneralEndpoints.sse.implement(_ =>
    for {
      ref <- ZIO.service[SubscriptionRef[Universe]]
      aa = createStreamWithPlayerId(None).provideEnvironment(ZEnvironment(ref))
    } yield aa
  )

  val sseRouteWithAuthentication: Route[SubscriptionRef[Universe] & Player, Nothing] = GeneralEndpoints.sseWithAuthentication.implement(_ =>
    withContext((player: Player) => for {
      _ <- ZIO.log("The player trying to subscribe to SSE stream is: " + player)
      ref <- ZIO.service[SubscriptionRef[Universe]]
      aa = createStreamWithPlayerId(Some(player.id)).provideEnvironment(ZEnvironment(ref))
    } yield aa)
  )

  val playerRoute: Route[Player, Nothing] = GeneralEndpoints.getPlayerEndpoint.implement(_ =>
    ZIO.log("playerRoute in action...") *>
    withContext((player: Player) => for {
      _ <- ZIO.log("The player is: " + player)
    } yield player.id)
  )

  val performGeneralActionRoute: Route[Player & MainService, Nothing] =
    GeneralEndpoints.performGeneralActionEndpoint.implement(generalAction =>
      withContext((player: Player) => for {
        _ <- ZIO.log(s"\tProvided action : $generalAction\n")
        s <- MainService.handleAction(generalAction, player.id).either
        _ <- ZIO.log(s"Action handling succeded? $s")
      } yield ()
    ))

  def generateGameSpecificActionRoute(game: BackendGameProps[_,_]): Route[Player & MainService, Nothing] =
    GeneralEndpoints.generateGameSpecificActionEndpoint(game).implement(action =>
      val actionCasted = action.asInstanceOf[Action]
      withContext((player: Player) => for {
        _ <- ZIO.log("decoded action is: " + action)
        s <- MainService.handleAction(actionCasted, player.id).either
      } yield ()
      ))
    
  val mw = Middleware.serveDirectory(Path.empty,
    new File(uiAssetsFolder)
  )

  val redirect1: Route[Any, Nothing] = Method.GET / "" -> Handler.succeed(Response.seeOther(url"index.html"))
  val redirect2: Route[Any, Nothing] = Method.GET / "s" -> Handler.succeed(Response.seeOther(url"spectator.html"))
  
  val mainJSHandler: Handler[Any, Response, Any, Response] = Handler.fromFile(mainFile).mapError(_ => Response.notFound)

  val mainJSRoute: Route[Any, Nothing] = Method.GET / "main.js" -> mainJSHandler

  val staticRoute = Method.GET / "static"
  
  val staticAssetsRoute: Routes[Any, Nothing] = Routes.empty @@ mw
  
  val staticRoutes = Routes(mainJSRoute) ++ staticAssetsRoute
  
  val routes = staticRoutes ++ Routes(joinRoute)

  private val mainFile = new File(mainFilePath)

  private def createStreamWithPlayerId(playerId: Option[Int]): ZStream[SubscriptionRef[Universe], Nothing, ServerSentEvent[String]] =
    ZStream.serviceWithStream[SubscriptionRef[Universe]](uRef => uRef.changes.map(universe =>
      val fu = universe.getFrontendUniverseForPlayer(playerId)
      ServerSentEvent(fu.encode.toJson)
    )) >>> ZPipeline.changes

}
