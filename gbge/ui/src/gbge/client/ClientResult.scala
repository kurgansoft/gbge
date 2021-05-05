package gbge.client

import gbge.shared.actions.Action
import zio.UIO

abstract sealed class ClientResult

case object OK extends ClientResult
case class PrepareRestActionWithToken(action: Action) extends ClientResult
case class ExecuteEffect[event <: ClientEvent](effect: AbstractCommander[event] => UIO[List[event]]) extends ClientResult {
  def addAnExtraEffect(extraEffect: AbstractCommander[event] => UIO[List[event]]): ExecuteEffects[event] = ExecuteEffects(List(effect, extraEffect))
}
case class ExecuteEffects[event <: ClientEvent](effects: List[AbstractCommander[event] => UIO[List[event]]]) extends ClientResult {
  def addAnExtraEffect(extraEffect: AbstractCommander[event] => UIO[List[event]]): ExecuteEffects[event] = ExecuteEffects(effects.appended(extraEffect))
}
