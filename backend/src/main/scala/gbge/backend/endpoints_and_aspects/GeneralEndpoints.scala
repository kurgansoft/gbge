package gbge.backend.endpoints_and_aspects

import gbge.backend.BackendGameProps
import gbge.shared.JoinResponse
import gbge.shared.actions.GeneralAction
import zio.ZNothing
import zio.http.*
import zio.http.codec.HttpCodec.content
import zio.http.codec.{HttpCodec, HttpContentCodec, PathCodec, StatusCodec}
import zio.http.endpoint.AuthType.None
import zio.http.endpoint.{AuthType, Endpoint}
import zio.schema.Schema

object GeneralEndpoints {

  private val eventStreamHeader = HttpCodec
    .header(Header.Accept)
    .const(Header.Accept(MediaType.text.`event-stream`))

  val sse =
    Endpoint(RoutePattern.GET / "publicEvents")
      .inCodec(
        eventStreamHeader ++ HttpCodec.query[String]("comment").optional
      )
      .outStream[ServerSentEvent[String]](MediaType.text.`event-stream`)

  val sseWithAuthentication =
    Endpoint(RoutePattern.GET / "events")
      .auth(AuthType.Bearer)
      .inCodec(
        eventStreamHeader ++ HttpCodec.query[String]("comment").optional
      )
      .outStream[ServerSentEvent[String]](MediaType.text.`event-stream`)

  val joinEndpoint = Endpoint(RoutePattern.POST / "join")
    .inCodec(HttpCodec.content[String](MediaType.text.plain))
    .outCodec(
      StatusCodec.Ok ++ HttpCodec
        .content[String](MediaType.text.plain)
    )
    .outError[String](Status.BadRequest)

  val getFUEndpoint = Endpoint(RoutePattern.GET / "fu")
    .outCodec(HttpCodec.content[String](MediaType.text.plain))

  val getPlayerEndpoint = Endpoint(RoutePattern.GET / "player")
    .outCodec(HttpCodec.content[Int](MediaType.text.plain))

  val performGeneralActionEndpoint
      : Endpoint[Unit, String, ZNothing, Unit, AuthType.Bearer] =
    Endpoint(RoutePattern.POST / "performAction")
      .auth(AuthType.Bearer)
      .inCodec(content[String])
      .outCodec(StatusCodec.Ok)

  def generateGameSpecificActionEndpoint(game: BackendGameProps[?, ?]) = {
    Endpoint(RoutePattern.POST / "performAction" / game.urlFragment)
      .auth(AuthType.Bearer)
      .inCodec(content[String])
      .outCodec(StatusCodec.Ok)
  }
}
