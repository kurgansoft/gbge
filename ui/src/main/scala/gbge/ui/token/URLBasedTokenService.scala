package gbge.ui.token

import zio.{IO, ZIO}

object URLBasedTokenService extends TokenService {
  override def saveToken(token: String): IO[Nothing, Unit] = for {
    _ <- ZIO.log("Saving token to hash.")
    _ = org.scalajs.dom.window.location.hash = token
  } yield ()

  override val getToken: IO[Nothing, Option[String]] = for {
    _ <- ZIO.log("Attempting to get token from hash")
    tokenFormURL: String = org.scalajs.dom.window.location.hash
    recoveredToken = if (tokenFormURL != null && tokenFormURL.length > 1 && tokenFormURL.startsWith("#"))
      Option(tokenFormURL.substring(1))
    else None
  } yield recoveredToken

  override val clearToken: IO[Nothing, Unit] = for {
    _ <- ZIO.log("Clearing token from hash")
    _ = org.scalajs.dom.window.location.hash = ""
  } yield ()
}
