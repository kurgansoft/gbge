package gbge.backend

import gbge.shared.actions.{Action, GameAction}
import gbge.shared.*
import zio.http.codec.ContentCodec

trait BackendGameProps[GA <: GameAction, FG <: FrontendGame[GA]] extends GameProps[GA, FG] {
  def start(noOfPlayers: Int): (BackendGame[_ <: GA, _ <: FG], Option[Action])

  val contentCodec: ContentCodec[GA]
}
