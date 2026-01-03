package gbge.ui.token

import zio.{IO, ZIO}

object LocalStorageBasedTokenService extends TokenService {
  override def saveToken(token: String): IO[Nothing, Unit] = {
    org.scalajs.dom.window.localStorage.setItem("token", token)
    ZIO.unit
  }

  override val getToken: IO[Nothing, Option[String]] = {
    org.scalajs.dom.window.localStorage.getItem("token") match {
      case value if value.forall(_.isWhitespace) => ZIO.none
      case value => ZIO.some(value)
    }
  }
}
