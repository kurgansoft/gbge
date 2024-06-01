package gbge.shared.actions

abstract sealed class ActionResult

case object ResultTimeout extends ActionResult
case object Error extends ActionResult

case class ResultOK(resultAsString: Option[String] = None) extends ActionResult
