package gbge.ui.eps.tm

import gbge.client._
import gbge.shared.tm._
import gbge.shared.{ClientTimeMachine, FrontendPlayer}
import gbge.ui.eps.player.{ClientState, NewPlayerEvent}
import gbge.ui.eps.spectator.{CONNECTED, SpectatorState}
import org.scalajs.dom.raw.WebSocket
import zio.UIO

import scala.util.Try

case class TimeMachineState(
                             status: DataFetchStatus = LOADING,
                             timeMachine: ClientTimeMachine = ClientTimeMachine(),
                             selectedAction: Option[Int] = None,
                             selectedPerspective: Option[Perspective] = None,
                             componentDisplayMode: ComponentDisplayMode = PPRINT,
                             selectedClientState: Either[CSState, UIState[_]] = Left(CS_NOT_SELECTED),
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
  private lazy val calculateClientState: Either[CSState, UIState[_]] = {
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
          val clientState = ClientState().processClientEvent(NewPlayerEvent(player.get))._1
            .handleNewFU(fu)._1
          Right(clientState)
        } else {
          val spectatorState = SpectatorState(frontendUniverse = Some(fu), CONNECTED).processClientEvent(NewFU(fu))._1
          Right(spectatorState)
        }
      } else {
        Left(CS_LOADING)
      }
    } else {
      Left(CS_NOT_SELECTED)
    }
  }

  val neededEffects: List[AbstractCommander[TMClientEvent] => UIO[List[TMClientEvent]]] = {
    val first: Option[AbstractCommander[TMClientEvent] => UIO[List[TMClientEvent]]] = if (selectedAction.isDefined && selectedAction.get >= 0 && selectedAction.get <= timeMachine.actions.size &&
      timeMachine.stateCache.get(selectedAction.get, SpectatorPerspective).isEmpty) {
      Some(TMEffects.retrieveTMState(selectedAction.get, SpectatorPerspective))
    } else
      None

    val second: Option[AbstractCommander[TMClientEvent] => UIO[List[TMClientEvent]]] = {
      if (selectedAction.isDefined && selectedAction.get >= 0 && selectedAction.get <= timeMachine.actions.size &&
        selectedPerspective.isDefined && timeMachine.stateCache.get(selectedAction.get, selectedPerspective.get).isEmpty) {
        Some(TMEffects.retrieveTMState(selectedAction.get, selectedPerspective.get))

      } else None
    }

    first.toList.appendedAll(second.toList).distinct
  }

  implicit def implicitConversion(state: TimeMachineState): (TimeMachineState, ClientResult) = (state, OK)

  override def processClientEvent(event: TMClientEvent): (TimeMachineState, ClientResult)  = {
    val x: (TimeMachineState, ClientResult) = event match {
        case Start => {
          (this, ExecuteEffect(TMEffects.recoverFromHash))
        }
        case TimeMachineHaveArrived(timeMachine) => {
          this.copy(timeMachine = timeMachine, status = LOADED)
        }
        case TMStateArrived(number, perspective, fu) => {
          val temp = this.copy(timeMachine = timeMachine.copy(
            stateCache = timeMachine.stateCache.updated((number, perspective), fu)
          )).transformTMState
          (temp, ExecuteEffects(temp.neededEffects))
        }
        // Effects to potentially execute: PersistSelection, retrieveTMState(Spectator), retrieveTMState(selectedPerspective)
        case ActionSelected(selected) => {
          if (status == LOADED && selected >= 0 && selected <= timeMachine.actions.size) {
            val newState = this.copy(selectedAction = Some(selected)).transformTMState
            (newState, ExecuteEffects(newState.neededEffects))
          } else {
            this
          }
        }
        case PerspectiveSelected(selection) => {
          if (selectedAction.isDefined) {
            val newState = this.copy(selectedPerspective = Some(selection)).transformTMState
            (newState, ExecuteEffects(newState.neededEffects))
          } else {
            this
          }
        }
        case SAVE => {
          (this, ExecuteEffect(TMEffects.save))
        }
        case SetComponentDisplayMode(mode) => {
          val newState = this.copy(componentDisplayMode = mode)
          newState
        }
        case TMMessageContainer(tmMessage) => {
          import gbge.shared.tm._
          tmMessage match {
            case PortalId(id) => {
              this.copy(portalId = Some(id))
            }
            case Update => {
              this
            }
            case _ => {
              this
            }
          }
        }
        case ResetTmToNumber(number) => {
          (this, ExecuteEffect(TMEffects.resetTmToNumber(number)))
        }
        case TmGotShrunk(number) => {
          this.copy(timeMachine = this.timeMachine.take(number))
        }
        case RegisterWebSocket(webSocket) => {
          this.copy(portalSocket = Some(webSocket))
        }
        case RecoveredHash(hash) => {
          val (portalId, actionNumber, perspective, mode) = TimeMachineState.calculateParamsFromHash(hash)
          val newState = this.copy(portalId = portalId, selectedAction = actionNumber,
            selectedPerspective = perspective,
            componentDisplayMode = if (mode) COMPONENT else PPRINT).transformTMState

          (newState, ExecuteEffects(List(
            TMEffects.retrieveTimeMachine,
            TMEffects.createOrReusePortal(newState.portalId),
            TMEffects.justReturnAnEvent(RetrieveMissingStates)
          )))
        }
        case RetrieveMissingStates => {
          val transformedState = this.transformTMState
          (transformedState, ExecuteEffects(transformedState.neededEffects))
        }
        case NewStateFromSubCommander(uiState) => {
          if (selectedClientState.isRight) {
            this.copy(selectedClientState = Right(uiState))
          } else {
            this
          }
        }
        case _ => this
      }
    if (x._1.stringToPersist == this.stringToPersist) {
      x
    } else {
      x._2 match {
        case OK => (x._1, ExecuteEffect(TMEffects.persistToHashAndSubmitPortalCoordinates(x._1)))
        case ee: ExecuteEffect[TMClientEvent] => (x._1, ee.addAnExtraEffect(TMEffects.persistToHashAndSubmitPortalCoordinates(x._1)))
        case ee: ExecuteEffects[TMClientEvent] => (x._1, ee.addAnExtraEffect(TMEffects.persistToHashAndSubmitPortalCoordinates(x._1)))
        case _ => (x._1, x._2)
      }
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
      } else if( pattern1.matches(t)) {
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