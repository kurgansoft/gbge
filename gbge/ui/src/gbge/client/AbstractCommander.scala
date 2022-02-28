package gbge.client

import zio.{Task, UIO}

import scala.concurrent.Future

trait AbstractCommander[Event <: ClientEvent] extends ClientEventHandler[Event] {

  @volatile var queue : List[Event] = List.empty

  private def bindEffectToCommander(commander: AbstractCommander[Event], effect: UIO[List[Event]]): Task[Unit] = {
    effect.map(calculatedEvents => commander.addEventsToTheEventQueue(calculatedEvents))
  }

  protected def handleEventsFromQueue(): Unit = {
    implicit val ec: scala.concurrent.ExecutionContext = org.scalajs.macrotaskexecutor.MacrotaskExecutor
    if (queue.nonEmpty) {
      val action = queue.head
      queue = queue.tail
      val result = getState().processClientEvent(action)
      setState(result._1)
      result._2 match {
        case OK =>
        case PrepareRestActionWithToken(_) => println("this should never happen")
        case ef0: ExecuteEffects[Event] => {
          val preparedEffects: List[UIO[List[Event]]] = ef0.effects.map(_(this))
          preparedEffects.size match {
            case 1 => {
              Future { //TODO There must be a better way to do this.
                zio.Runtime.default.unsafeRun(bindEffectToCommander(this, preparedEffects.head))
              }
            }
            case noOfEffects if noOfEffects > 1 => {
              val combinedEffects = preparedEffects.tail.foldLeft(preparedEffects.head)((left, right) => left.zipWith(right)((effectList, effectToAppend) => effectList.appendedAll(effectToAppend)))
              Future { //TODO There must be a better way to do this.
                zio.Runtime.default.unsafeRun(bindEffectToCommander(this, combinedEffects))
              }
            }
            case _ =>
          }
        }
        case ef: ExecuteEffect[Event] => {
          Future { //TODO There must be a better way to do this.
            zio.Runtime.default.unsafeRun(bindEffectToCommander(this, ef.effect(this)))
          }
        }
      }
      handleEventsFromQueue()
    } else {
      render()
    }
  }

  protected def handleEventsFromQueueWithoutEffectExecution(): Unit = {
    if (queue.nonEmpty) {
      val action = queue.head
      queue = queue.tail
      val result = getState().processClientEvent(action)
      setState(result._1)
      result._2 match {
        case OK  =>
        case PrepareRestActionWithToken(_) => println("this should never happen")
        case _: ExecuteEffect[Event] => println("Effect execution is not supported in this mode.")
      }
      handleEventsFromQueueWithoutEffectExecution()
    } else {
      render()
    }
  }

  def setState(state: UIState[Event])

  def getState(): UIState[Event]

  def render(): Unit

  override def addAnEventToTheEventQueue(event: Event): Unit = {
    queue = queue.appended(event)
    handleEventsFromQueue()
  }

  override def addEventsToTheEventQueue(events: List[Event]): Unit = {
    queue = queue.appendedAll(events)
    handleEventsFromQueue()
  }
}