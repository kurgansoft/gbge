package gbge.ui.eps.tm

import gbge.client.*
import gbge.shared.tm.*
import gbge.shared.{ClientTimeMachine, FrontendPlayer}
import gbge.ui.eps.player.ClientState
import gbge.ui.eps.spectator.{CONNECTED, SpectatorState}
import org.scalajs.dom.WebSocket
import uiglue.EventLoop.EventHandler
import uiglue.{Event, UIState}
import zio.*

import scala.language.implicitConversions
import scala.util.Try

case class TimeMachineState(
                             status: DataFetchStatus = LOADING,
                             timeMachine: ClientTimeMachine = ClientTimeMachine(),
                             selectedAction: Option[Int] = None,
                             selectedPerspective: Option[Perspective] = None,
                             componentDisplayMode: ComponentDisplayMode = PPRINT,
                             selectedClientState: Either[CSState, UIState[Event]] = Left(CS_NOT_SELECTED),
                             portalId: Option[Int] = None,
                             portalSocket: Option[WebSocket] = None
                      ) extends UIState[TMClientEvent] {

  val stringToPersist: String = {
    val portalIdAsString: String = portalId.map(_.toString).getOrElse("")
    val action: String = selectedAction.map(_.toString).getOrElse("")
    val perspectiveString: String = selectedPerspective.map(_.id.toString).getOrElse("")
    val cm = if (componentDisplayMode == COMPONENT) "c" else ""
    if (portalIdAsString != "")
      portalIdAsString + "," + action + "," + perspectiveString + cm
    else
      ""
  }

  lazy val getPortalCoordinates: PortalCoordinates = {
    assert(portalId.isDefined)
    PortalCoordinates(portalId.get, selectedAction, selectedPerspective)
  }

  lazy val currentSelectionAvailableInTheCache: Boolean = {
    if (selectedPerspective.isEmpty || selectedAction.isEmpty) {
      true
    } else {
      timeMachine.stateCache.get(selectedAction.get, selectedPerspective.get).isDefined
    }
  }

  lazy val getPlayersForSelectedAction: Option[List[FrontendPlayer]] = {
    if (selectedAction.isDefined)
      this.timeMachine.stateCache.get(selectedAction.get, SpectatorPerspective).map(_.players)
    else
      None
  }

  lazy val selectedPerspectiveIsValid: Boolean = {
    if (selectedAction.isEmpty || selectedPerspective.isEmpty || selectedPerspective.contains(SpectatorPerspective))
      true
    else {
      val spectatorPerspective = timeMachine.stateCache.get(selectedAction.get, selectedPerspective.get)
      if (spectatorPerspective.isEmpty) {
        true
      } else {
        spectatorPerspective.map(_.players.map(_.id))
          .forall(_.exists(_ == selectedPerspective.get.id))
      }
    }
  }

  lazy val transformTMState: TimeMachineState = {
    val conflictsResolved = selectedPerspective match {
      case Some(PlayerPerspective(perspectiveId)) => {
        if (getPlayersForSelectedAction.isDefined && !getPlayersForSelectedAction.get.exists(_.id == perspectiveId)) {
          this.copy(selectedPerspective = None)
        } else {
          this
        }
      }
      case _ => this
    }
    conflictsResolved.copy(selectedClientState = conflictsResolved.calculateClientState)
  }

  // Populates the selectedClientState field if it is possible from the cache
  private lazy val calculateClientState: Either[CSState, UIState[Event]] = {
    if (selectedAction.isDefined && selectedPerspective.isDefined) {
      val players = getPlayersForSelectedAction
      val player: Option[FrontendPlayer] = {
        selectedPerspective match {
          case Some(PlayerPerspective(id)) => players.getOrElse(List.empty).find(_.id == id)
          case _ => None
        }
      }
      if (timeMachine.stateCache.get(selectedAction.get, selectedPerspective.get).isDefined) {
        val fu = timeMachine.stateCache(selectedAction.get, selectedPerspective.get)
        if (player.isDefined) {
          val clientState = ClientState()
//          val clientState = ClientState().processEvent(NewPlayerEvent(player.get))._1
//            .handleNewFU(fu)._1
          Right(clientState)
        } else {
          val spectatorState = SpectatorState(frontendUniverse = Some(fu), CONNECTED).processEvent(NewFU(fu))._1
          Right(spectatorState)
        }
      } else {
        Left(CS_LOADING)
      }
    } else {
      Left(CS_NOT_SELECTED)
    }
  }

  val neededEffect: UIO[List[TMClientEvent]] = {
    if (selectedAction.isDefined && selectedAction.get >= 0 && selectedAction.get <= timeMachine.actions.size) {
      if (timeMachine.stateCache.get(selectedAction.get, SpectatorPerspective).isEmpty)
        TMEffects.retrieveTMState(selectedAction.get, SpectatorPerspective)
      else if (selectedPerspective.isDefined && timeMachine.stateCache.get(selectedAction.get, selectedPerspective.get).isEmpty)
        TMEffects.retrieveTMState(selectedAction.get, selectedPerspective.get)
      else
        ZIO.succeed(List.empty)
    } else
      ZIO.succeed(List.empty)
  }

  implicit def implicitConversion(state: TimeMachineState): (TimeMachineState, EventHandler[TMClientEvent] => UIO[List[TMClientEvent]]) =
    (state, _ => ZIO.succeed(List.empty))

  def sequentiallyCombineEffects[E <: Event](effects : EventHandler[E] => UIO[List[E]]*): EventHandler[E] => UIO[List[E]] = {
    effects.foldLeft(eh => effects.head(eh))((acc, v) => eh => acc(eh).zipWith(v(eh))(_.appendedAll(_)))
  }

  override def processEvent(event: TMClientEvent): (TimeMachineState, EventHandler[TMClientEvent] => UIO[List[TMClientEvent]]) = {
    val x: (TimeMachineState, EventHandler[TMClientEvent] => UIO[List[TMClientEvent]]) =
      event match {
        case Start => {
          (this, TMEffects.recoverFromHash)
        }
        case TimeMachineHaveArrived(tm) =>
          this.copy(timeMachine = tm, status = LOADED)
        case TMStateArrived(number, perspective, fu) => {
          val temp = this.copy(timeMachine = timeMachine.copy(
            stateCache = timeMachine.stateCache.updated((number, perspective), fu)
          )).transformTMState
          (temp, temp.neededEffect)
        }
        // Effects to potentially execute: PersistSelection, retrieveTMState(Spectator), retrieveTMState(selectedPerspective)
        case ActionSelected(selected) => {
          if (status == LOADED && selected >= 0 && selected <= timeMachine.actions.size) {
            val newState = this.copy(selectedAction = Some(selected)).transformTMState
            (newState, newState.neededEffect)
          } else {
            this
          }
        }
        case PerspectiveSelected(selection) => {
          if (selectedAction.isDefined) {
            val newState = this.copy(selectedPerspective = Some(selection)).transformTMState
            (newState, newState.neededEffect)
          } else {
            this
          }
        }
        case SAVE =>
          (this, TMEffects.save)
        case SetComponentDisplayMode(mode) => {
          val newState = this.copy(componentDisplayMode = mode)
          newState
        }
        case TMMessageContainer(tmMessage) => {
          import gbge.shared.tm._
          tmMessage match {
            case PortalId(id) => this.copy(portalId = Some(id))
            case Update => this
            case _ => this
          }
        }
        case ResetTmToNumber(number) =>
          (this, TMEffects.resetTmToNumber(number))
        case TmGotShrunk(number) =>
          this.copy(timeMachine = this.timeMachine.take(number))
        case RegisterWebSocket(webSocket) =>
          this.copy(portalSocket = Some(webSocket))
        case RecoveredHash(hash) =>
          val (portalId, actionNumber, perspective, mode) = TimeMachineState.calculateParamsFromHash(hash)
          val newState = this.copy(portalId = portalId, selectedAction = actionNumber,
            selectedPerspective = perspective,
            componentDisplayMode = if (mode) COMPONENT else PPRINT).transformTMState
          (newState, sequentiallyCombineEffects(TMEffects.retrieveTimeMachine, TMEffects.createOrReusePortal(newState.portalId), TMEffects.justReturnAnEvent(RetrieveMissingStates)))
        case RetrieveMissingStates =>
          val transformedState = this.transformTMState
          (transformedState, transformedState.neededEffect)
        case EventFromSelectedPerspective(event) =>
          if (selectedClientState.isRight) {
            this.copy(selectedClientState = selectedClientState.map(_.processEvent(event)._1))
          } else
            this
        case _ => this
      }
    if (x._1.stringToPersist == this.stringToPersist) {
      x
    } else { // persist to hash
      (x._1, sequentiallyCombineEffects(x._2, TMEffects.persistToHashAndSubmitPortalCoordinates(x._1)))
    }
  }
}

object TimeMachineState {
  def calculateParamsFromHash(hash: String): (Option[Int], Option[Int], Option[Perspective], Boolean) = {
    if (hash.length > 1) {
      val t: String = hash.substring(1)
      val pattern3 = "([0-9]+),*([0-9]+),*([0-9]*)(c*)".r
      val pattern1 = "([0-9]+).*".r

      if (pattern3.matches(t)) {
        val pattern3(portalId, action, perspective, cMode) = t
        (
          Try(Some(Integer.parseInt(portalId))).getOrElse(None),
          Try(Some(Integer.parseInt(action))).getOrElse(None),
          Try(Integer.parseInt(perspective)).getOrElse(-1) match {
            case 0 => Some(SpectatorPerspective)
            case positiveNumber if positiveNumber > 0 => Some(PlayerPerspective(positiveNumber))
            case _ => None
          },
          cMode.length == 1
        )
      } else if(pattern1.matches(t)) {
        val pattern1(portalId) = t
        (Some(Integer.parseInt(portalId)), None, None, false)
      } else {
        (None, None, None, false)
      }
    } else {
      (None, None, None, false)
    }
  }
}