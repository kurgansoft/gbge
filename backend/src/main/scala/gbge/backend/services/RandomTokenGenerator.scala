package gbge.backend.services

import zio.{IO, Random, Ref, ZIO, ZLayer}

case class RandomTokenGenerator(valueZero: Ref[Int]) extends TokenGenerator {
  override val generateToken: IO[Nothing, String] = for {
    nextTokenPrefix <- valueZero.updateAndGet(_ + 1)
    randomSuffix <- Random.nextIntBetween(1, 10000000)
  } yield f"$nextTokenPrefix%03d$randomSuffix%07d"
}

object RandomTokenGenerator {
  val layer: ZLayer[Int, Nothing, TokenGenerator] = ZLayer {
    for {
      valueZero <- ZIO.service[Int]
      ref <- Ref.make(valueZero)
    } yield new RandomTokenGenerator(ref)
  }

  val defaultLayer: ZLayer[Any, Nothing, TokenGenerator] = ZLayer {
    for {
      ref <- Ref.make(0)
    } yield new RandomTokenGenerator(ref)
  }
}
