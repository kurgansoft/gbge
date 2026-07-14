package gbge.ui.eps.player

import gbge.client.*
import gbge.client.events_and_effects.*
import gbge.shared.actions.{GameAction, GeneralAction, KickPlayer}
import gbge.shared.{FrontendPlayer, FrontendUniverse, GameRole}
import gbge.ui.ClientGameProps
import gbge.ui.eps.ConnectionStatus.*
import gbge.ui.eps.{ConnectionState, ConnectionStatus}
import gbge.ui.state.OfflineState
import gbge.ui.state.screenstates.{JoinScreenState, WelcomeScreenState}
import gbge.ui.token.TokenService
import uiglue.{Event, EventLoop, UIState}
import zio.{Clock, ZIO}

import scala.language.{implicitConversions, postfixOps}

case class ClientState(
                        frontendUniverse: Option[FrontendUniverse] = None,
                        connectionState: ConnectionState = ConnectionState(),
                        you: Option[(Int, String)] = None,
                        offlineState: OfflineState[TokenService] = JoinScreenState(),
                        tab: Int = 1, // 1 -> general, 2 -> meta, 3-> admin
                      ) extends UIState[Event, TokenService & Clock] {

  lazy val playerMaybe: Option[FrontendPlayer] = for {
    playerId <- you.map(_._1)
    fu <- frontendUniverse
    player <- fu.players.find(_.id == playerId)
  } yield player

  lazy val allRoles: List[GameRole] = (for {
    fu <- frontendUniverse
    game <- fu.game
  } yield game.roles).getOrElse(List.empty)

  lazy val isAdmin: Boolean = playerMaybe.exists(_.isAdmin)
  
  lazy val yourRole: Option[GameRole] = for {
    p <- playerMaybe
    yourRoleId <- p.role
    matchedRole <- allRoles.find(_.roleId == yourRoleId)
  } yield matchedRole

  def getPropsOfTheCurrentGame: Option[ClientGameProps[_,_]] =  {
    val selectedGameNumber = frontendUniverse.flatMap(_.selectedGame)
    selectedGameNumber.map(gbge.ui.RG.registeredGames(_))
  }
  
  def getCurrentGame: Option[ClientGameProps[_,_]] = {
    val selectedGameNumber = frontendUniverse.flatMap(_.selectedGame)
    selectedGameNumber.map(gbge.ui.RG.registeredGames(_))
  }

  implicit def convert(clientState: ClientState): (ClientState, EventLoop.EventHandler[Event] => ZIO[Any, Nothing, List[Event]]) =
    (clientState, _ => ZIO.succeed(List.empty))

  override def processEvent(event: Event): (ClientState, EventLoop.EventHandler[Event] => ZIO[TokenService & Clock, Nothing, List[Event]]) = {
    event match {
      case VisibilityChanged => (this,  eh => {
        val visibilityState = org.scalajs.dom.document.visibilityState
        val eventList = if (connectionState.status == BROKEN && visibilityState == "visible" && connectionState.isItWorthToTryReconnecting) {
          List(Connect(Some("RECONNECTING AFTER BROKEN CONNECTION AS SCREEN IS VISIBLE AGAIN")))
        } else
          List.empty
        ZIO.succeed(eventList)
      })
      case SetupEventListeners => (this,  eh => for {
        _ <- ZIO.log("Setting up event listener for visibility change DOM event.")
        _ = org.scalajs.dom.window.addEventListener("visibilitychange", (event: org.scalajs.dom.Event) =>
          eh(VisibilityChanged)
        )
      } yield List.empty)
      case CheckForTokenEvent => (this, _ => for {
        events <- ClientEffects.recoverTokenEffect
      } yield events)
      case DispatchActionWithToken(action) =>
        if (you.isDefined) {
          action match {
            case ga: GeneralAction => (this, ClientEffects.submitGeneralActionWithToken(ga, you.get._2))
            case gameAction: GameAction =>
              (this,
              ClientEffects.submitGameSpecificActionWithToken(gameAction, this.frontendUniverse.get.selectedGame.get, you.map(_._2).get)
            )
          }
        } else {
          this
        }
      case NewFU(fu) =>
        handleNewFU(fu)
      case RecoverTokenEvent(token) =>
        (this, ClientEffects.getPlayerWithToken(token))
      case sa: ScreenEvent =>
        val x = offlineState.handleScreenEvent(sa, frontendUniverse, you.map(_._1))
        (this.copy(offlineState = x._1), _ => x._2)
      case PlayerRecovered(id, token) =>
        (this.copy(you = Some(id, token)), eh => ZIO.succeed(List(Connect(Some("CONNECTING WITH RECOVERED TOKEN")))))
      case FailedToRecoverPlayer =>
        (this.copy(you = None), eh => ClientEffects.clearToken.as(List(Reload)))
      case LogOut =>
        (this, eh => for {
          _ <- ClientEffects.submitGeneralActionWithToken(KickPlayer(you.get._1), you.get._2)
          tokenService <- ZIO.service[TokenService]
          _ <- tokenService.clearToken
          _ = org.scalajs.dom.window.location.reload()
        } yield List.empty)
      case Reload =>
        (this, eh => for {
          _ <- ZIO.log("...about to reload...")
          _ = org.scalajs.dom.window.location.reload()
        } yield List.empty)
      case JoinResponseEvent(joinResponse) =>
        val temp = this.copy(you = Some((joinResponse.id, joinResponse.token)))
        if (temp.offlineState.isInstanceOf[JoinScreenState]) {
          (temp.copy(offlineState = WelcomeScreenState()), eh => ZIO.succeed(List(Connect())))
        } else {
          temp
        }
      case Connect(optionalMessage) =>
        if (you.isDefined) {
          val token = you.map(_._2)
          (this, eh => for {
            _ <- ClientEffects.createSSEConnection(eh, token, optionalMessage).orDie.forkDaemon
          } yield List())
        } else {
          this
        }
      case ConnectionBrokeDown(timeStamp) => {
        val newConnectionState = this.connectionState.addDisconnectionTimeStamp(timeStamp)
        val eventList = if (newConnectionState.isItWorthToTryReconnecting) List(Connect(Some("STANDARD RECONNECTION AFTER DISCONNECT"))) else List.empty
        (this.copy(connectionState = newConnectionState), _ => for {
          _ <- ZIO.when(eventList.nonEmpty)(ZIO.log("Connection just broke down, attempting to reconnect..."))
          _ <- ZIO.when(eventList.isEmpty)(ZIO.log("Connection just broke down, but _NOT_ worth reconnecting."))
        } yield eventList)
      }
      case ConnectionEstablished(timeStamp) =>
        this.copy(connectionState = this.connectionState.transitionToConnectedStateWithTimeStamp(timeStamp))
      case CHANGE_TO_TAB(tab) =>
        this.copy(tab = tab)
      case _ =>
        this
    }
  }

  private def handleNewFU(fu: FrontendUniverse): (ClientState, EventLoop.EventHandler[Event] => ZIO[TokenService, Nothing, List[Event]]) = {
    println("...new FU arrived...")
    val updatedYou = fu.players.find(_.id == this.you.get._1)
    val adminStatusLost: Boolean = false // you.exists(_.isAdmin) && updatedYou.exists(!_.isAdmin)
    val newTab: Int = tab match {
      case 3|4|5|6 =>
        if (adminStatusLost)
          1
        else
          tab
      case _ => tab
    }
    if (updatedYou.isEmpty) {
      this.copy(frontendUniverse = Some(fu), offlineState = JoinScreenState(), tab = newTab)
    } else if (fu.game.isDefined) {
      val temp = this.copy(tab = newTab)
      gbge.ui.RG.registeredGames(fu.selectedGame.get).handleNewFU(temp, fu)
    }
    else {
      this.offlineState match {
        case _ : WelcomeScreenState => this.copy(frontendUniverse = Some(fu), tab = newTab)
        case _ => this.copy(frontendUniverse = Some(fu), offlineState = WelcomeScreenState(), tab = newTab)
      }

    }
  }
}

sealed trait ACTIVE_MENU

case object GAME_MENU extends ACTIVE_MENU
case object ADMIN_MENU extends ACTIVE_MENU
case object LOG_MENU extends ACTIVE_MENU
case object LOG_OUT_MENU extends ACTIVE_MENU
case object LOGIN_MENU extends ACTIVE_MENU