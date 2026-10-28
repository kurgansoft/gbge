package gbge.client.events_and_effects

import gbge.client.SseUtils
import gbge.shared.actions.{GameAction, GeneralAction}
import gbge.shared.{FrontendUniverse, JoinResponse}
import gbge.ui.Urls
import gbge.ui.token.TokenService
import sttp.capabilities.zio.ZioStreams
import sttp.client4.impl.zio.FetchZioBackend
import sttp.client4.{Request, UriContext, WebSocketStreamBackend, basicRequest}
import uiglue.EventLoop
import zio.{Clock, Task, UIO, ZIO}

object ClientEffects {

  private val backend: WebSocketStreamBackend[Task, ZioStreams] =
    FetchZioBackend()

  val recoverTokenEffect: ZIO[TokenService, Nothing, List[PlayerEvent]] = for {
    tokenService <- ZIO.service[TokenService]
    token <- tokenService.getToken
    result =
      if (token.isDefined) {
        List(RecoverTokenEvent(token.get))
      } else {
        List.empty
      }
  } yield result

  val clearToken: ZIO[TokenService, Nothing, List[PlayerEvent]] = for {
    tokenService <- ZIO.service[TokenService]
    _ <- tokenService.clearToken
  } yield List.empty

  def getPlayerWithToken(token0: String): UIO[List[PlayerEvent]] = {

    import sttp.client4.asString

    val request = basicRequest
      .get(uri"${Urls.getPlayerPostFix}")
      .auth
      .bearer(token0)
      .response(asString.mapRight((_: String).toInt))

    for {
      _ <- ZIO.log(s"trying to get player with token: [$token0]")
      x <- backend
        .send(request)
        .map(response =>
          response.body match
            case Left(l) =>
              List(FailedToRecoverPlayer)
            case Right(playerId) =>
              List(
                PlayerRecovered(playerId, token0)
              )
        )
        .orDie
      _ <- ZIO.log("the list: " + x)
    } yield x
  }

  def createSSEConnection(
      eventHandler: EventLoop.EventHandler[GeneralEvent],
      token: Option[String] = None,
      comment: Option[String] = None
  ): ZIO[Clock, Throwable, Unit] = {
    val onMessage = (message: String) => {
      val jsonMessage = message.substring(
        6
      ) // The SSE message starts with 'data: ' that is why the first few characters are discarded
      val ast =
        zio.blocks.schema.json.Json.jsonCodec.decode(jsonMessage).getOrElse(???)
      val fu: FrontendUniverse = FrontendUniverse.decodeWithDecoder(ast)(using
        gbge.ui.RG.gameCodecs
      )
      eventHandler(NewFU(fu))
    }

    val url: String = token match {
      case Some(_) => Urls.nonPublicEvents
      case None    => Urls.publicEvents
    }

    val urlWithComment = comment match {
      case Some(c) => url + s"?comment=$c"
      case None    => url
    }

    for {
      clockService <- ZIO.service[Clock]
      _ <- ZIO.log("Creating SSE connection...")
      response <- ZIO.fromFuture(_ =>
        SseUtils.createFetchPromise(urlWithComment, token).toFuture
      )
      epochMillisOfConnection <- clockService.instant.map(_.toEpochMilli)
      _ = eventHandler(ConnectionEstablished(epochMillisOfConnection))
      _ <- ZIO
        .fromFuture(ec =>
          SseUtils.processStream(response.body, onMessage)(using ec)
        )
        .tapError(error =>
          for {
            _ <- ZIO.log(
              "connection error: " + error.getMessage + "\n\t" + error.getStackTrace
                .mkString("\n\t")
            )
            epochMillisOfDisconnection <- clockService.instant
              .map(_.toEpochMilli)
            _ = eventHandler(ConnectionBrokeDown(epochMillisOfDisconnection))
          } yield ()
        )
    } yield ()
  }

  def joinWithName(
      name: String
  ): ZIO[TokenService, Nothing, List[PlayerEvent]] = {

    val request: Request[Either[String, String]] =
      basicRequest
        .post(uri"${Urls.joinPostFix}")
        .body(name)

    backend
      .send(request)
      .flatMap(response =>
        response.body match
          case Left(_)                     => ZIO.succeed(List.empty)
          case Right(joinResponseAsString) =>
            val ast = zio.blocks.schema.json.Json.jsonCodec
              .decode(joinResponseAsString)
              .getOrElse(???)
            JoinResponse.codec.decode(ast) match {
              case Left(schemaError) =>
                ZIO.dieMessage("failed to decode response")
              case Right(joinResponse: JoinResponse) =>
                for {
                  _ <- zio.Console.printLine("your id: " + joinResponse.id)
                  _ <- zio.Console
                    .printLine("your token: " + joinResponse.token)
                  tokenService <- ZIO.service[TokenService]
                  _ <- tokenService.saveToken(joinResponse.token)
                } yield List(JoinResponseEvent(joinResponse))
            }
      )
      .orDie
  }

  def submitGeneralActionWithToken(
      generalAction: GeneralAction,
      token: String
  ): UIO[List[Nothing]] = {
    val request = basicRequest
      .post(uri"${Urls.performActionPostFix}")
      .auth
      .bearer(token)
      .body(generalAction.asJsonString)
    backend.send(request).orDie.as(List.empty)
  }

  def submitGameSpecificActionWithToken(
      action: GameAction,
      selectedGame: Int,
      token: String
  ): UIO[List[Nothing]] = {
    val req0 = gbge.ui.RG
      .registeredGames(selectedGame)
      .createSttpRequestFromGameAction(action)
    req0 match {
      case None =>
        ZIO
          .log(
            s"Seems like provided action [$action] is not compatible with the current game."
          )
          .as(List.empty)
      case Some(request) =>
        val req1 = request.auth.bearer(token)
        ZIO.log(s"Sending game specific action [$action]") *>
          backend.send(req1).orDie.as(List.empty)
    }
  }
}
