package gbge.backend.gameroutes

import gbge.backend.BackendGameProps
import gbge.backend.endpoints_and_aspects.GeneralEndpoints
import gbge.backend.models.{Player, Universe}
import gbge.backend.services.MainService
import gbge.shared.JoinResponse
import gbge.shared.actions.GeneralAction
import zio.http.*
import zio.stream.{SubscriptionRef, ZPipeline, ZStream}
import zio.{Ref, ZEnvironment, ZIO}

object GameRoutes {
  val joinRoute: Route[MainService, Nothing] =
    GeneralEndpoints.joinEndpoint.implement(input =>
      MainService
        .joinWithName(input)
        .mapBoth(
          _.toString,
          value => {
            JoinResponse.codec.encodeToJson(value).print
          }
        )
    )

  val fuRoute: Route[Ref[Universe], Nothing] =
    GeneralEndpoints.getFUEndpoint.implementHandler(
      Handler.fromZIO(for {
        uRef <- ZIO.service[Ref[Universe]]
        u <- uRef.get
      } yield u.getFrontendUniverseForPlayer(None).encode.print)
    )

  val sseRoute: Route[SubscriptionRef[Universe], Nothing] =
    GeneralEndpoints.sse.implement(optionalComment =>
      for {
        _ <- ZIO.when(optionalComment.nonEmpty)(
          ZIO.log(
            "Comment during anonymous SSE subscription:\n\t" + optionalComment.get
          )
        )
        ref <- ZIO.service[SubscriptionRef[Universe]]
        aa = createStreamWithPlayerId(None).provideEnvironment(
          ZEnvironment(ref)
        )
      } yield aa
    )

  val sseRouteWithAuthentication
      : Route[SubscriptionRef[Universe] & Player, Nothing] =
    GeneralEndpoints.sseWithAuthentication.implement(optionalComment =>
      withContext((player: Player) =>
        for {
          _ <- ZIO.log(
            "The player trying to subscribe to SSE stream is: " + player
          )
          _ <- ZIO.when(optionalComment.nonEmpty)(
            ZIO.log(
              "Comment during SSE subscription:\n\t" + optionalComment.get
            )
          )
          ref <- ZIO.service[SubscriptionRef[Universe]]
          aa = createStreamWithPlayerId(Some(player.id))
            .provideEnvironment(ZEnvironment(ref))
        } yield aa
      )
    )

  val playerRoute: Route[Player, Nothing] =
    GeneralEndpoints.getPlayerEndpoint.implement(_ =>
      withContext((player: Player) =>
        for {
          _ <- ZIO.log("The player is: " + player)
        } yield player.id
      )
    )

  val performGeneralActionRoute: Route[Player & MainService, Nothing] =
    GeneralEndpoints.performGeneralActionEndpoint.implement(
      generalActionAsString =>
        withContext((player: Player) =>
          for {
            generalAction <- GeneralAction.jsonCodec.decode(
              generalActionAsString
            ) match {
              case Left(value) => {
                val message = "Does not look like a valid action."
                ZIO.logError(message) *> ZIO.die(new RuntimeException(message))
              }
              case Right(generalAction: GeneralAction) =>
                ZIO.succeed(generalAction)
            }
            s <- MainService.handleAction(generalAction, player.id).either
          } yield ()
        )
    )

  def generateGameSpecificActionRoute(
      game: BackendGameProps[?, ?]
  ): Route[Player & MainService, Nothing] =
    GeneralEndpoints
      .generateGameSpecificActionEndpoint(game)
      .implement(action =>
        val actionCasted = game.actionCodec.decode(action).getOrElse(???)
        withContext((player: Player) =>
          for {
            s <- MainService.handleAction(actionCasted, player.id).either
          } yield ()
        )
      )

  private def createStreamWithPlayerId(
      playerId: Option[Int]
  ): ZStream[SubscriptionRef[Universe], Nothing, ServerSentEvent[String]] =
    (ZStream.serviceWithStream[SubscriptionRef[Universe]](uRef =>
      uRef.changes.map(universe =>
        val fu = universe.getFrontendUniverseForPlayer(playerId)
        ServerSentEvent(fu.encode.print)
      )
    ) >>> ZPipeline.changes)
      .ensuring(playerId match {
        case Some(id) => ZIO.log(s"SSE stream ended for player with id [$id].")
        case None     => ZIO.log("SSE stream ended for anonymous user.")
      })
}
