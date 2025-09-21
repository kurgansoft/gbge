package gbge.backend.services

import zio.{IO, Ref, ZIO, ZLayer}

trait TokenGenerator {
  val generateToken: IO[Nothing, String]
}

object TokenGenerator {
  val generateToken: ZIO[TokenGenerator, Nothing, String] =
    ZIO.serviceWithZIO(_.generateToken)
}
