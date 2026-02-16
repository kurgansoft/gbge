package gbge.shared.actions

trait Action {
  val systemOnly: Boolean = false
  val adminOnly: Boolean = false
  def convertToJson(): String
}

trait GameAction extends Action
