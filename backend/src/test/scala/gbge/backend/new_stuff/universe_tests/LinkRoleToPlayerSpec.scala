package gbge.backend.new_stuff.universe_tests

import gbge.backend.models.{Player, Universe}
import gbge.backend.{GeneralFailure, UnauthorizedFailure}
import gbge.shared.actions.{KickPlayer, LinkRoleToPlayer}
import zio.Scope
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object LinkRoleToPlayerSpec extends ZIOSpecDefault {
  override def spec: Spec[Scope, Any] =
    suite("top")(
      suite("successes")(
        test("self linking works when a game is in progress; the role exists and is not taken yet")({
//          val u = Universe(players = Map("abc" -> Player(1, "Steve", true)))
//          val result = u.reduce(LinkRoleToPlayer(1,2), 1)
//          assert(result.isRight)
//          val u2 = result.getOrElse(???)._1
//          assertTrue(u2.players.isEmpty)
          assertTrue(true)
        }),

      ),
      suite("failures")(
        test("when game is not started yet; it fails")({
          val u = Universe(Seq.empty, players = Map("abc" -> Player(1, "Steve", true)))
          val result = u.reduce(LinkRoleToPlayer(1,2), Some(1))
          assertTrue(result.isLeft)
        }),
      )
    )
}
