package gbge.shared.actions

trait Action {
  val systemOnly: Boolean = false
  val adminOnly: Boolean = false

  lazy val asJsonString: String
}

trait GameAction extends Action
