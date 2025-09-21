package gbge.ui.eps.player

import gbge.client.*
import gbge.shared.FrontendUniverse
import gbge.shared.actions.{GameAction, GeneralAction}
import gbge.ui.ClientGameProps
import gbge.ui.state.OfflineState
import gbge.ui.state.screenstates.{JoinScreenState, WelcomeScreenState}
import org.scalajs.dom.WebSocket
import uiglue.{Event, EventLoop, UIState}
import zio.{UIO, ZIO}

import scala.language.implicitConversions

case class ClientState(
                        frontendUniverse: Option[FrontendUniverse] = None,
                        you: Option[(Int, String)] = None,
                        offlineState: OfflineState = JoinScreenState(),
                        tab: Int = 1, // 1 -> general, 2 -> meta, 3-> admin
                        ws: Option[WebSocket] = None
                      ) extends UIState[Event] {
  lazy val playerId = you.getOrElse(???)._1
  lazy val player = frontendUniverse.get.players.find(_.id == playerId)
  lazy val isAdmin: Boolean = player.exists(_.isAdmin)
  
  lazy val allRoles = frontendUniverse.get.game.get.roles
  
  lazy val yourRole = for {
    p <- player
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

  implicit def convert(clientState: ClientState): (ClientState, EventLoop.EventHandler[Event] => UIO[List[Event]]) =
    (clientState, _ => ZIO.succeed(List.empty))

  override def processEvent(event: Event): (ClientState, EventLoop.EventHandler[Event] => UIO[List[Event]]) = {
    event match {
      case CheckForTokenEvent => (this, ZIO.log("checking for token?") *> ClientEffects.recoverTokenEffect)
      case DispatchActionWithToken(action) =>
        println(s"dispatching action ??? $action")
        if (you.isDefined) {
          action match {
            case ga: GeneralAction => (this, ClientEffects.submitGeneralActionWithToken(ga, you.get._2))
            case gameAction: GameAction =>
              println(s"sending gameAction? ({println}) $gameAction")
              (this,
              ClientEffects.submitGameSpecificActionWithToken(gameAction, this.frontendUniverse.get.selectedGame.get, you.map(_._2).get)
            )
          }
        } else {
          this
        }
      case NewFU(fu) => handleNewFU(fu)
      case BootstrapPlayerEvent =>
//        (this, ClientEffects.recoverTokenEffect)
        this
      case RecoverTokenEvent(token) =>
        (this, ClientEffects.getPlayerWithToken(token))
      case sa: ScreenEvent =>
        val x = offlineState.handleScreenEvent(sa, frontendUniverse, None)
        (this.copy(offlineState = x._1), x._2)
      case PlayerRecovered(id, token) =>
        println("Player id successfully recovered; subscribing to SSE stream")
        (this.copy(you = Some(id, token)), eh => ZIO.succeed(List(CreateSSEStream)))
      case JoinResponseEvent(joinResponse) =>
        val temp = this.copy(you = Some((joinResponse.id, joinResponse.token)))
        if (temp.offlineState.isInstanceOf[JoinScreenState]) {
          (temp.copy(offlineState = WelcomeScreenState()), eh => ZIO.succeed(List(CreateSSEStream)))
        } else {
          temp
        }
      case CreateSSEStream =>
        if (you.isDefined)
          (this, eh => {
            ClientEffects.createSSEConnection(eh)
            ZIO.succeed(List.empty)
          })
        else {
          this
        }
      case CHANGE_TO_TAB(tab) =>
        this.copy(tab = tab)
      case _ =>
        this
    }
  }

  def handleNewFU(fu: FrontendUniverse): (ClientState, EventLoop.EventHandler[Event] => UIO[List[Event]]) = {
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