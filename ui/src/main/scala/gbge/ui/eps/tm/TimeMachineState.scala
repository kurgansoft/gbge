package gbge.ui.eps.tm

import gbge.shared.{FrontendPlayer, JoinResponse}
import gbge.shared.tm.*
import gbge.ui.eps.player.{ClientState, JoinResponseEvent}
import gbge.ui.eps.spectator.{CONNECTED, SpectatorState}
import uiglue.EventLoop.EventHandler
import uiglue.{Event, UIState}
import zio.*

import scala.language.implicitConversions
import scala.util.Try

case class TimeMachineState(
                             status: DataFetchStatus = LOADING,
                             actionsAndInvokers: List[ActionInvokerAndPlayers] = List.empty,
                             selectedAction: Option[Int] = None,
                             selectedPerspective: Option[Perspective] = None,
                             componentDisplayMode: ComponentDisplayMode = PPRINT,
                             selectedClientState: CSState = CS_NOT_SELECTED,
                             portalId: Option[Int] = None,
                      ) extends UIState[TMClientEvent, Any] {

  private lazy val playersOfSelection: List[FrontendPlayer] = {
    selectedAction.map({
      case number if number > 0 && number <= actionsAndInvokers.size => actionsAndInvokers(number - 1).players
      case _ => List.empty
    }).getOrElse(List.empty)
  }

  lazy val stringToPersist: String = {
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
    PortalCoordinates(portalId.getOrElse(150), selectedAction, selectedPerspective)
  }

//  lazy val currentSelectionAvailableInTheCache: Boolean = {
//    if (selectedPerspective.isEmpty || selectedAction.isEmpty) {
//      true
//    } else {
//      timeMachine.stateCache.get(selectedAction.get, selectedPerspective.get).isDefined
//    }
//  }
//
//  lazy val getPlayersForSelectedAction: Option[List[FrontendPlayer]] = {
//    if (selectedAction.isDefined)
//      this.timeMachine.stateCache.get(selectedAction.get, SpectatorPerspective).map(_.players)
//    else
//      None
//  }

//  lazy val selectedPerspectiveIsValid: Boolean = {
//    if (selectedAction.isEmpty || selectedPerspective.isEmpty || selectedPerspective.contains(SpectatorPerspective))
//      true
//    else {
//      val spectatorPerspective = timeMachine.stateCache.get(selectedAction.get, selectedPerspective.get)
//      if (spectatorPerspective.isEmpty) {
//        true
//      } else {
//        spectatorPerspective.map(_.players.map(_.id))
//          .forall(_.exists(_ == selectedPerspective.get.id))
//      }
//    }
//  }

//  lazy val transformTMState: TimeMachineState = {
//    val conflictsResolved = selectedPerspective match {
//      case Some(PlayerPerspective(perspectiveId)) => {
//        if (getPlayersForSelectedAction.isDefined && !getPlayersForSelectedAction.get.exists(_.id == perspectiveId)) {
//          this.copy(selectedPerspective = None)
//        } else {
//          this
//        }
//      }
//      case _ => this
//    }
//    conflictsResolved.copy(selectedClientState = conflictsResolved.calculateClientState)
//  }

  // Populates the selectedClientState field if it is possible from the cache
//  private lazy val calculateClientState: Either[CSState, UIState[Event, Any]] = {
//    if (selectedAction.isDefined && selectedPerspective.isDefined) {
//      val players = getPlayersForSelectedAction
//      val player: Option[FrontendPlayer] = {
//        selectedPerspective match {
//          case Some(PlayerPerspective(id)) => players.getOrElse(List.empty).find(_.id == id)
//          case _ => None
//        }
//      }
//      if (timeMachine.stateCache.get(selectedAction.get, selectedPerspective.get).isDefined) {
//        val fu = timeMachine.stateCache(selectedAction.get, selectedPerspective.get)
//        if (player.isDefined) {
//          val clientState = ClientState()
////          val clientState = ClientState().processEvent(NewPlayerEvent(player.get))._1
////            .handleNewFU(fu)._1
//          ???
////          Right(clientState)
//        } else {
//          val spectatorState = SpectatorState(frontendUniverse = Some(fu), CONNECTED).processEvent(NewFU(fu))._1
//          Right(spectatorState)
//        }
//      } else {
//        Left(CS_LOADING)
//      }
//    } else {
//      Left(CS_NOT_SELECTED)
//    }
//  }

  lazy val neededEffect: UIO[List[TMClientEvent]] =
    if (selectedAction.isDefined && selectedAction.get >= 0 && selectedAction.get <= actionsAndInvokers.size)
      TMEffects.retrieveTMState(selectedAction.get, selectedPerspective.getOrElse(SpectatorPerspective))
    else
      ZIO.succeed(List.empty)

  implicit def implicitConversion(state: TimeMachineState): (TimeMachineState, EventHandler[TMClientEvent] => UIO[List[TMClientEvent]]) =
    (state, _ => ZIO.succeed(List.empty))

  private def sequentiallyCombineEffects[E <: Event](effects : EventHandler[E] => UIO[List[E]]*): EventHandler[E] => UIO[List[E]] = {
    effects.reduceLeft((acc, v) => eh => acc(eh).zipWith(v(eh))(_.appendedAll(_)))
  }

  override def processEvent(event: TMClientEvent): (TimeMachineState, EventHandler[TMClientEvent] => UIO[List[TMClientEvent]]) = {
    val x: (TimeMachineState, EventHandler[TMClientEvent] => UIO[List[TMClientEvent]]) =
      event match {
        case Start =>
          (this, TMEffects.recoverFromHash)
        case ActionsHaveArrived(actionsAndInvokers) =>
          this.copy(status = LOADED, actionsAndInvokers = actionsAndInvokers)
//        case TimeMachineHaveArrived(tm) =>
//          this.copy(timeMachine = tm, status = LOADED)
        case TMStateArrived(number, SpectatorPerspective, fu) =>
          this.copy(selectedClientState = CSState_Loaded(SpectatorState(Some(fu), CONNECTED)))
        case TMStateArrived(number, PlayerPerspective(playerId), fu) =>
          val player = fu.players.find(_.id == playerId).get
          val clientState: ClientState = ClientState(Some(fu), Some((playerId, player.name))).processEvent(JoinResponseEvent(JoinResponse(player.id, "???")))._1
          this.copy(selectedClientState = CSState_Loaded(
            clientState
          ))
        // Effects to potentially execute: PersistSelection, retrieveTMState(Spectator), retrieveTMState(selectedPerspective)
        case ActionSelected(selected) =>
          if (status == LOADED && selected >= 0 && selected <= actionsAndInvokers.size && !this.selectedAction.contains(selected)) {
            val step1 = this.copy(selectedAction = Some(selected))
            val newState = selectedPerspective match {
              case None | Some(SpectatorPerspective) =>
                step1
              case Some(PlayerPerspective(playerId)) if step1.playersOfSelection.exists(_.id == playerId) =>
                step1
              case _ =>
                step1.copy(selectedPerspective = None, selectedClientState = CS_NOT_SELECTED)
            }
            if (newState.selectedAction != this.selectedAction && newState.selectedPerspective.nonEmpty)
              (newState, TMEffects.retrieveTMState(newState.selectedAction.get, newState.selectedPerspective.get))
            else
              newState
          } else {
            this
          }
        case PerspectiveSelected(perspective) => {
          if (selectedAction.isDefined) {
            val newState = this.copy(selectedPerspective = Some(perspective))
            (newState, TMEffects.retrieveTMState(newState.selectedAction.get, perspective))
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
          import gbge.shared.tm.*
          tmMessage match {
            case PortalId(id) => this.copy(portalId = Some(id))
            case Update => this
            case _ => this
          }
        }
        case ResetTmToNumber(number) =>
          (this, TMEffects.resetTmToNumber(number))
        case TmGotShrunk(number) =>
          this.copy(actionsAndInvokers = this.actionsAndInvokers.take(number))
        case RecoveredHash(hash) =>
          println(s"recovering from hash [$hash]")
          val (portalId, actionNumber, perspective, mode) = TimeMachineState.calculateParamsFromHash(hash)
          val newState = this.copy(portalId = portalId, selectedAction = actionNumber,
            selectedPerspective = perspective,
            componentDisplayMode = if (mode) COMPONENT else PPRINT)
//          (newState, sequentiallyCombineEffects(TMEffects.retrieveTimeMachine, TMEffects.createOrReusePortal(newState.portalId), TMEffects.justReturnAnEvent(RetrieveMissingStates)))
          (newState, TMEffects.retrieveActionStack)
//        case RetrieveMissingStates =>
//          val transformedState = this.transformTMState
//          (transformedState, transformedState.neededEffect)
        case EventFromSelectedPerspective(event) =>
          selectedClientState match {
            case CSState_Loaded(uiState) =>
              val newUiState = uiState.processEvent(event)._1
              this.copy(selectedClientState = CSState_Loaded(newUiState))
            case _ => this
          }
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