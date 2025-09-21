package gbge.backend.endpoints_and_aspects

import gbge.backend.models.{Player, Universe}
import zio.{Ref, ZIO}
import zio.http.{HandlerAspect, Header}

object Aspects {
  val tokenExtractorAspect: HandlerAspect[Ref[Universe], Player] = HandlerAspect.customAuthProvidingZIO(request => {
    request.headers.get(Header.Authorization) match
      case Some(Header.Authorization.Bearer(tokenAsSecret)) =>
        val tokenAsString = tokenAsSecret.value.mkString
        for {
          u <- ZIO.serviceWithZIO[Ref[Universe]](_.get)
        } yield u.players.get(tokenAsString)
      case _ => ZIO.none
  })
}
