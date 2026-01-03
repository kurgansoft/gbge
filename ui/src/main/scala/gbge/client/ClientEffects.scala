package gbge.client

import gbge.shared.actions.{GameAction, GeneralAction}
import gbge.shared.{FrontendUniverse, JoinResponse}
import gbge.ui.eps.player.{JoinResponseEvent, PlayerEvent, PlayerRecovered, RecoverTokenEvent}
import gbge.ui.token.TokenService
import gbge.ui.Urls
import org.scalajs.dom.EventSource
import sttp.capabilities
import sttp.capabilities.zio.ZioStreams
import sttp.client4.impl.zio.{FetchZioBackend, ZioServerSentEvents}
import sttp.client4.ziojson.asJson
import sttp.client4.{Request, Response, ResponseException, UriContext, WebSocketStreamBackend, asStream, basicRequest}
import sttp.model.sse.ServerSentEvent
import uiglue.EventLoop
import zio.json.*
import zio.stream.ZStream
import zio.{Task, UIO, ZIO}

object ClientEffects {

  private val backend: WebSocketStreamBackend[Task, ZioStreams] = FetchZioBackend()

  val recoverTokenEffect: ZIO[TokenService, Nothing, List[PlayerEvent]] = for {
    tokenService <- ZIO.service[TokenService]
    token <- tokenService.getToken
    result  = if (token.isDefined) {
      List(RecoverTokenEvent(token.get))
    } else {
      List.empty
    }
  } yield result

  def getPlayerWithToken(token0: String): UIO[List[PlayerEvent]] = {

    import sttp.client4.asString

    val request = basicRequest.get(uri"${Urls.getPlayerPostFix}")
      .auth.bearer(token0)
      .response(asString.mapRight((_: String).toInt))

    for {
      _ <- ZIO.log(s"trying to get player with token: [$token0]")
      x <- backend.send(request).map(response => response.body match
        case Left(l) =>
          println("the l: " + l.getClass + " - " + l)
          List.empty
        case Right(playerId) =>
          println("the decoded player is: " + playerId)
          List(
            PlayerRecovered(playerId, token0)
          )
      ).orDie
      _ <- ZIO.log("the list: " + x)
    } yield x
  }


  def createSSEConnection(eventHandler: EventLoop.EventHandler[GeneralEvent]): Unit = {
    val eventSource = new EventSource(Urls.publicEvents)
    eventSource.onmessage = message => {
      println("message.data: " + message.data)
      val ast = zio.json.ast.Json.decoder.decodeJson(message.data.toString).getOrElse(???)
      val newUniverse: FrontendUniverse = FrontendUniverse.decodeWithDecoder(ast)(gbge.ui.RG.gameCodecs.map(_.decoder))
      println("the decoded fu: " + newUniverse)
      eventHandler(NewFU(newUniverse))
    }
    eventSource.onerror = error => {
      println("something went haywire: " + error)
      eventHandler(WebsocketConnectionBrokeDown)
    }
  }

  // NOT USED YET
  def connectToPublicSseEndpointAndConsumeIt(): Task[Response[Either[String, Unit]]] = {
    val sseRequest =
      basicRequest
        .get(uri"http://localhost:8080/publicEvents")
        .response(asStream(ZioStreams)(stream => {
      val x: ZStream[Any, Throwable, ServerSentEvent] = stream.viaFunction(ZioServerSentEvents.parse)
      val t: ZIO[Any, Throwable, Unit] = for {
        _ <- zio.Console.printLine("about to consume stream...")
        a <- x.foreach(a => for {
            _ <- zio.Console.printLine("the raw data: " + a.data)
            ast = zio.json.ast.Json.decoder.decodeJson(a.data.get).getOrElse(???)
            _ <- zio.Console.printLine("the ast => " + ast)
          } yield ())
          .tapError(e => zio.Console.printLine("something went wrong... ==> " + e).orDie)
        _ <- zio.Console.printLine("is it reached?")
      } yield ()
      t
    }))
    val q: Task[Response[Either[String, Unit]]] = backend.send(sseRequest)
    q
  }

  def joinWithName(name: String): ZIO[TokenService, Nothing, List[PlayerEvent]] = {

    val request: Request[Either[ResponseException[String], JoinResponse]] =
      basicRequest.post(uri"${Urls.joinPostFix}").body(name)
        .response(asJson[JoinResponse])

    println(s"about to send join request; [$name]")

    backend.send(request).flatMap(response => response.body match
      case Left(_) => ZIO.succeed(List.empty)
      case Right(joinResponse) =>
        for {
          _ <- zio.Console.printLine("your id: " + joinResponse.id)
          _ <- zio.Console.printLine("your token: " + joinResponse.token)
          tokenService <- ZIO.service[TokenService]
          _ <- tokenService.saveToken(joinResponse.token)
        } yield List(JoinResponseEvent(joinResponse))
    ).orDie
  }

  def submitGeneralActionWithToken(generalAction: GeneralAction, token: String): UIO[List[Nothing]] = {
    val request = basicRequest.post(uri"${Urls.performActionPostFix}")
      .auth.bearer(token)
      .body(asJson[GeneralAction](generalAction))
    backend.send(request).orDie.as(List.empty)
  }

  def submitGameSpecificActionWithToken(action: GameAction, selectedGame: Int, token: String): UIO[List[Nothing]] = {
    val req0 = gbge.ui.RG.registeredGames(selectedGame).createSttpRequestFromGameAction(action)
    req0 match {
      case None => ZIO.log(s"Seems like provided action [$action] is not compatible with the current game.").as(List.empty)
      case Some(request) =>
        val req1 = request.auth.bearer(token)
        ZIO.log(s"Sending game specific action [$action]") *>
          backend.send(req1).orDie.as(List.empty)
    }
  }
}