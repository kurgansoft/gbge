package gbge.ui.token

import zio.{IO, ZIO}

object URLBasedTokenService extends TokenService {
  override def saveToken(token: String): IO[Nothing, Unit] = {
    org.scalajs.dom.window.location.hash = token
    ZIO.unit
  }

  override val getToken: IO[Nothing, Option[String]] = {
    val tokenFormURL: String = org.scalajs.dom.window.location.hash
    val recoveredToken = if (tokenFormURL != null && tokenFormURL.length > 1 && tokenFormURL.startsWith("#"))
      Option(tokenFormURL.substring(1))
    else None
    ZIO.succeed(recoveredToken)
  }
}
