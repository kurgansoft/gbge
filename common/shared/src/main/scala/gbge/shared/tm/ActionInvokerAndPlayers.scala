package gbge.shared.tm

import gbge.shared.FrontendPlayer
import gbge.shared.actions.Action

case class ActionInvokerAndPlayers(action: Action, invoker: Option[Int], players: List[FrontendPlayer])
