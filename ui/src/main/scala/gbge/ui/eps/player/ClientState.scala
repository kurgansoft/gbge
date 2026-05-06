package gbge.ui.eps.player

import gbge.client.*
import gbge.shared.{FrontendPlayer, FrontendUniverse, GameRole}
import gbge.shared.actions.{GameAction, GeneralAction}
import gbge.ui.ClientGameProps
import gbge.ui.state.OfflineState
import gbge.ui.state.screenstates.{JoinScreenState, WelcomeScreenState}
import gbge.ui.token.TokenService
import uiglue.{Event, EventLoop, UIState}
import zio.ZIO

import scala.language.implicitConversions

case class ClientState(
                        frontendUniverse: Option[FrontendUniverse] = None,
                        you: Option[(Int, String)] = None,
                        offlineState: OfflineState[TokenService] = JoinScreenState(),
                        tab: Int = 1, // 1 -> general, 2 -> meta, 3-> admin
                      ) extends UIState[Event, TokenService] {

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

  override def processEvent(event: Event): (ClientState, EventLoop.EventHandler[Event] => ZIO[TokenService, Nothing, List[Event]]) = {
    event match {
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
          (this, _ => ZIO.succeed(List.empty))
        }
      case NewFU(fu) => handleNewFU(fu)
      case BootstrapPlayerEvent =>
//        (this, ClientEffects.recoverTokenEffect)
        this
      case RecoverTokenEvent(token) =>
        (this, ClientEffects.getPlayerWithToken(token))
      case sa: ScreenEvent =>
        val x = offlineState.handleScreenEvent(sa, frontendUniverse, you.map(_._1))
        (this.copy(offlineState = x._1), _ => x._2)
      case PlayerRecovered(id, token) =>
        (this.copy(you = Some(id, token)), eh => ZIO.succeed(List(CreateSSEStream)))
      case JoinResponseEvent(joinResponse) =>
        val temp = this.copy(you = Some((joinResponse.id, joinResponse.token)))
        if (temp.offlineState.isInstanceOf[JoinScreenState]) {
          (temp.copy(offlineState = WelcomeScreenState()), eh => ZIO.succeed(List(CreateSSEStream)))
        } else {
          temp
        }
      case CreateSSEStream =>
        if (you.isDefined) {
          val token = you.map(_._2)
          (this, eh => for {
            _ <- ClientEffects.createSSEConnection(eh, token).orDie
          } yield List.empty)
        } else {
          this
        }
      case CHANGE_TO_TAB(tab) =>
        this.copy(tab = tab)
      case _ =>
        this
    }
  }

  private def handleNewFU(fu: FrontendUniverse): (ClientState, EventLoop.EventHandler[Event] => ZIO[TokenService, Nothing, List[Event]]) = {
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