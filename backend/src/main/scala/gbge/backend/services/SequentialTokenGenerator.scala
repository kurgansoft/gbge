package gbge.backend.services

import zio.{IO, Ref, ZIO, ZLayer}

case class SequentialTokenGenerator(valueZero: Ref[Int]) extends TokenGenerator {
  override val generateToken: IO[Nothing, String] = for {
    nextToken <- valueZero.updateAndGet(_ + 1)
  } yield nextToken.toString
}

object SequentialTokenGenerator {
  val layer: ZLayer[Int, Nothing, TokenGenerator] = ZLayer {
    for {
      valueZero <- ZIO.service[Int]
      ref <- Ref.make(valueZero)
    } yield new SequentialTokenGenerator(ref)
  }

  val defaultLayer: ZLayer[Any, Nothing, TokenGenerator] = ZLayer {
    for {
      ref <- Ref.make(0)
    } yield new SequentialTokenGenerator(ref)
  }
}
