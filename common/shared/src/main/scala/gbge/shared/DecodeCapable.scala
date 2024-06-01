package gbge.shared

import gbge.shared.actions.GameAction

trait DecodeCapable {
  def decode(encodedForm: String): FrontendGame[_ <: GameAction]
  val name: String
}
