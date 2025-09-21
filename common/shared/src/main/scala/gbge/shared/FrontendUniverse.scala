package gbge.shared

import gbge.shared.FrontendUniverse.RawRepresentation
import gbge.shared.actions.GameAction
import zio.json.*
import zio.schema.{DeriveSchema, Schema}

case class FrontendUniverse(selectedGame: Option[Int] = None,
                            game: Option[FrontendGame[_ <: GameAction]] = None,
                            players: List[FrontendPlayer] = List.empty,
                            playerIdToRoleLink: Map[Int,Int] = Map.empty) {
  
  import zio.json.ast.Json as Json
  
  lazy val encode: zio.json.ast.Json = {
    val s = game.map(_.encode)
    val s2 = RawRepresentation(selectedGame, s, players, playerIdToRoleLink)
    s2.toJsonAST.getOrElse(???)
  }
  
}

object FrontendUniverse {

  def decodeWithDecoder(json: zio.json.ast.Json)(implicit decoders: List[JsonDecoder[FrontendGame[_]]]): FrontendUniverse = {
    val raw = RawRepresentation.codec.decoder.fromJsonAST(json).getOrElse(???)
    val temp = FrontendUniverse(selectedGame = raw.selectedGame, players = raw.players, playerIdToRoleLink = raw.playerIdToRoleLink)

    val decoder = raw.selectedGame.map(index => decoders(index))

    val gameOption: Option[FrontendGame[_]] = (decoder , raw.game) match {
      case (Some(d), Some(g)) => Some(d.fromJsonAST(g).getOrElse(???))
      case _ => None
    }
    temp.copy(game = gameOption)
  }


  def decode(json: zio.json.ast.Json): FrontendUniverse = {
    val raw = RawRepresentation.codec.decoder.fromJsonAST(json).getOrElse(???)
    val temp = FrontendUniverse(selectedGame = raw.selectedGame, players = raw.players, playerIdToRoleLink = raw.playerIdToRoleLink)

    val gameOption: Option[FrontendGame[_]] = raw.game.map(jsonAst => {
      val selectedGame: Int = raw.selectedGame.get
      gbge.shared.RG.gameCodecs(selectedGame).decoder.fromJsonAST(jsonAst).getOrElse(???)
    })
    temp.copy(game = gameOption)
  }

  case class RawRepresentation(selectedGame: Option[Int],
                               game: Option[zio.json.ast.Json],
                               players: List[FrontendPlayer],
                               playerIdToRoleLink: Map[Int,Int])

  private object RawRepresentation {
    implicit val schema: Schema[RawRepresentation] = DeriveSchema.gen[RawRepresentation]
    implicit val codec: JsonCodec[RawRepresentation] = DeriveJsonCodec.gen[RawRepresentation]
  }
}