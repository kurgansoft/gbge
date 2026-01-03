package gbge.ui.token

import zio.IO

trait TokenService {
  def saveToken(token: String): IO[Nothing, Unit]
  val getToken: IO[Nothing, Option[String]]
}
