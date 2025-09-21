package gbge.backend.new_stuff.mocks

import gbge.backend.services.TokenGenerator
import zio.*
import zio.mock.*

object MockTokenGenerator extends Mock[TokenGenerator] {
  
  object GenerateToken extends Effect[Unit, Nothing, String]

  val compose: URLayer[Proxy, TokenGenerator] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new TokenGenerator {
        override val generateToken: IO[Nothing, String] =
          proxy(GenerateToken)
      }
    }
}
