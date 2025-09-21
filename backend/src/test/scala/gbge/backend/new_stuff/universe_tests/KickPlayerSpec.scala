package gbge.backend.new_stuff.universe_tests

import gbge.backend.{GeneralFailure, UnauthorizedFailure}
import gbge.backend.models.{Player, Universe}
import gbge.shared.actions.KickPlayer
import zio.Scope
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object KickPlayerSpec extends ZIOSpecDefault {
  override def spec: Spec[Scope, Any] =
    suite("top")(
      suite("successes")(
        test("admin can kick itself out")({
          val u = Universe(Seq.empty, players = Map("abc" -> Player(1, "Steve", true)))
          val result = u.reduce(KickPlayer(1), Some(1))
          assert(result.isRight)
          val u2 = result.getOrElse(???)._1
          assertTrue(u2.players.isEmpty)
        }),
        test("non-admin can kick itself out")({
          val u = Universe(Seq.empty, players = Map(
            "abc" -> Player(1, "Steve", true),
            "def" -> Player(2, "Joe")
          ))
          val result = u.reduce(KickPlayer(2), Some(2))
          assert(result.isRight)
          val u2 = result.getOrElse(???)._1
          assertTrue(u2.players.size == 1 && u2.players.values.head.isAdmin)
        }),
        test("admin can kick out any other player")({
          val u = Universe(Seq.empty, players = Map(
            "abc" -> Player(1, "Steve", true),
            "def" -> Player(2, "Joe")
          ))
          val result = u.reduce(KickPlayer(2), Some(1))
          assert(result.isRight)
          val u2 = result.getOrElse(???)._1
          assertTrue(u2.players.size == 1 && u2.players.values.head.isAdmin)
        })
      ),
      suite("failures")(
        test("non-admin cannot kick any other player out")({
          val u = Universe(Seq.empty, players = Map(
            "abc" -> Player(1, "Steve", true),
            "def" -> Player(2, "Joe"),
            "ghi" -> Player(3, "Diane")
          ))
          val result = u.reduce(KickPlayer(3), Some(2))
          assert(result.isLeft)
          val failure = result.swap.getOrElse(???)
          assertTrue(failure.isInstanceOf[UnauthorizedFailure])
        }),
        test("admin tries to kick-out non-existing player")({
          val u = Universe(Seq.empty, players = Map(
            "abc" -> Player(1, "Steve", true),
            "def" -> Player(2, "Joe"),
            "ghi" -> Player(3, "Diane")
          ))
          val result = u.reduce(KickPlayer(33), Some(1))
          assert(result.isLeft)
          val failure = result.swap.getOrElse(???)
          assertTrue(failure.isInstanceOf[GeneralFailure])
        }),
      )
    )
}
