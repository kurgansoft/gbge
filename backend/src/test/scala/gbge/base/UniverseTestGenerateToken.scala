package gbge.base

import gbge.backend.{Failure, Player, OKWithPlayerPayload, Universe}
import gbge.shared.actions.ProvideToken
import org.scalatest.funsuite.AnyFunSuite

class UniverseTestGenerateToken extends AnyFunSuite {

  def invariant(universe: Universe): Unit = {
    if (universe.game.isDefined) {
      assert(universe.selectedGame.isDefined)
    }
    if (universe.players.nonEmpty) {
      universe.players.exists(_.isAdmin)
    }
  }

  val player1: Player = Player(1, "A", "token1")
  val player2: Player = Player(2, "B", "token2", isAdmin = true)
  val player3: Player = Player(3, "C", "token3")
  val player4: Player = Player(4, "D", "token4")
  val player5: Player = Player(5, "E", "token5")
  val player6: Player = Player(6, "F", "token6")
  val player7: Player = Player(7, "G", "token7")
  val player8: Player = Player(8, "H", "token8")
  val player9: Player = Player(9, "I", "token9")
  val player10: Player = Player(10, "X", "token10")
  val player11: Player = Player(11, "Y", "token11")
  val player12: Player = Player(12, "Z", "token12")

  test("GenerateToken; join is NOT in progress") {
    val universe = Universe()
    assert(!universe.joinInProgress)
    val result = universe.reduce(ProvideToken("token"))
    assert(result._1 == universe)
    assert(result._2.isInstanceOf[Failure])
  }

  test("GenerateToken; token is already taken") {
    val universe = Universe(players = List(player2), candidate = Some(player1))
    assert(universe.joinInProgress)
    val result = universe.reduce(ProvideToken("token2"))
    assert(!result._1.joinInProgress)
    assert(result._2.isInstanceOf[Failure])
  }

  test("GenerateToken; success") {
    val theToken = "appropriateToken"
    val universe = Universe(players = List(player2), candidate = Some(player1))
    assert(universe.joinInProgress)
    val result = universe.reduce(ProvideToken(theToken))
    assert(!result._1.joinInProgress)
    assert(result._1.players.exists(_.token == theToken))
    assert(result._2.isInstanceOf[OKWithPlayerPayload])
  }
}

