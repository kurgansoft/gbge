package gbge.backend.endpoints_and_aspects

import zio.http.{MediaType, RoutePattern, Status}
import zio.http.codec.{HttpCodec, StatusCodec}
import zio.http.endpoint.Endpoint

object TimeMachineEndpoints {
  val tmResetEndpoint = Endpoint(RoutePattern.POST / "reset")
    .inCodec(HttpCodec.content[Int](MediaType.text.plain))
    .outCodec(StatusCodec.Ok)
    .outError[Unit](Status.BadRequest)
}
