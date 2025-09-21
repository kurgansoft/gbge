package gbge.shared.actions

trait Action {
  val systemOnly: Boolean = false
  val adminOnly: Boolean = false
}

trait GameAction extends Action
