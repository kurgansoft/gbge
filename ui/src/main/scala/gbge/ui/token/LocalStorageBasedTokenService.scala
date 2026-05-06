package gbge.ui.token

import zio.{IO, ZIO}

object LocalStorageBasedTokenService extends TokenService {
  
  private val tokenKeyName = "token"
  
  override def saveToken(token: String): IO[Nothing, Unit] = for {
    _ <- ZIO.log("Saving token to local storage.")
    _ = org.scalajs.dom.window.localStorage.setItem(tokenKeyName, token)
  } yield ()

  override val getToken: IO[Nothing, Option[String]] = for {
    _ <- ZIO.log("Attempting to get token from local storage.")
    token: Option[String] = org.scalajs.dom.window.localStorage.getItem(tokenKeyName) match {
      case null | "" => None
      case value => Some(value)
    }
  } yield token

  override val clearToken: IO[Nothing, Unit] = for {
    _ <- ZIO.log("Clearing token from local storage.")
    _ = org.scalajs.dom.window.localStorage.removeItem(tokenKeyName)
  } yield ()
}
