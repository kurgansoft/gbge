package gbge.shared

import gbge.shared.FrontendUniverse.RawRepresentation
import gbge.shared.actions.GameAction
import zio.blocks.schema.Schema
import zio.blocks.schema.json.{Json, JsonCodec}

case class FrontendUniverse(
    selectedGame: Option[Int] = None,
    game: Option[FrontendGame[? <: GameAction]] = None,
    players: List[FrontendPlayer] = List.empty,
    playerIdToRoleLink: Map[Int, Int] = Map.empty
) {

  lazy val encode: Json = {
    val s = game.map(_.encode)
    val s2 = RawRepresentation(selectedGame, s, players, playerIdToRoleLink)
    RawRepresentation.codec.encodeToJson(s2)
  }

}

object FrontendUniverse {

  def decodeWithDecoder(
      json: Json
  )(implicit
      decoders: List[JsonCodec[FrontendGame[? <: GameAction]]]
  ): FrontendUniverse = {
    val raw = RawRepresentation.codec.decode(json) match {
      case Right(value) => value
      case Left(_)      =>
        throw new RuntimeException("could not decode FrontendUniverse")
    }
    val temp = FrontendUniverse(
      selectedGame = raw.selectedGame,
      players = raw.players,
      playerIdToRoleLink = raw.playerIdToRoleLink
    )

    val gameOption: Option[FrontendGame[? <: GameAction]] = raw.game.map {
      jsonAst =>
        val selectedGame: Int = raw.selectedGame.get
        decoders(selectedGame).decode(jsonAst) match {
          case Right(game) => game
          case Left(_)     =>
            throw new RuntimeException("could not decode game payload")
        }
    }
    temp.copy(game = gameOption)
  }

  def decode(json: Json): FrontendUniverse = {
    val raw = RawRepresentation.codec.decode(json) match {
      case Right(value) => value
      case Left(_)      =>
        throw new RuntimeException("could not decode FrontendUniverse")
    }
    val temp = FrontendUniverse(
      selectedGame = raw.selectedGame,
      players = raw.players,
      playerIdToRoleLink = raw.playerIdToRoleLink
    )

    val gameOption: Option[FrontendGame[? <: GameAction]] = raw.game.map {
      jsonAst =>
        val selectedGame: Int = raw.selectedGame.get
        gbge.shared.RG.gameCodecs(selectedGame).decode(jsonAst) match {
          case Right(game) => game
          case Left(_)     =>
            throw new RuntimeException("could not decode game payload")
        }
    }
    temp.copy(game = gameOption)
  }

  case class RawRepresentation(
      selectedGame: Option[Int],
      game: Option[Json],
      players: List[FrontendPlayer],
      playerIdToRoleLink: Map[Int, Int]
  )

  private object RawRepresentation {
    val codec: JsonCodec[RawRepresentation] = Schema.derived.jsonCodec
  }
}
