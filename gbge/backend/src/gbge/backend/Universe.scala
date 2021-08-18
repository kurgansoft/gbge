package gbge.backend

import gbge.shared.{FrontendGame, FrontendUniverse}
import gbge.shared.actions._
import zio.{UIO, ZIO}

object UniverseEffects {
  val generateToken: Universe => UIO[List[Action]] = universe => {
    val token = universe.generateToken()
    ZIO.succeed(List(ProvideToken(token)))
  }
}

case class Universe(selectedGame: Option[Int] = None,
                    game: Option[BackendGame[_ <: FrontendGame[_ <: GameAction]]] = None,
                    players: List[Player] = List.empty,
                    candidate: Option[Player] = None,
                    nextPlayerId: Int = 1) {

  val joinInProgress: Boolean = candidate.isDefined

  def generateToken(): String = {
    val randomTokens: Boolean = System.getProperty("randomTokens", "true").toBooleanOption.getOrElse(true)

    val randomSuffix = if (randomTokens)
      scala.util.Random.between(1,10000).toString
    else ""

    (nextPlayerId + 100).toString + randomSuffix
  }

  def reduce(action: Action, playerToken: Option[String] = None): (Universe, UniverseResult) = {
    val player = getPlayerFromToken(playerToken)
    action match {
      case ProvideToken(token) => reduceProvideToken(token)
      case UnassignRole(role) => reduceUnassignRole(role)
      case Join(name) => reduceJoin(name)
      case KickPlayer(playerId) => reduceKickPlayer(playerId, player)
      case adminAction: AdminAction => reduceAdminAction(adminAction, player)
      case linkRoleToPlayer: LinkRoleToPlayer => reduceLinkRoleToPlayer(linkRoleToPlayer, player)
      case unlinkPlayerFromRole: UnlinkPlayerFromRole => reduceUnlinkPlayerFromRole(unlinkPlayerFromRole, player)
      case NaturalLink(roles) => reduceNaturalLink(roles)
      case ga: GameAction => reduceGameAction(ga, player)
      case _ => (this, OK)
    }
  }

  private def getPlayerFromToken(token: Option[String]): Option[Player] = token.flatMap(t => players.find(_.token == t))

  private def reduceKickPlayer(playerId: Int, player: Option[Player]): (Universe, UniverseResult) = {
    if (player.isEmpty) {
      (this, UnauthorizedFailure())
    } else if (player.isDefined && (player.get.isAdmin || player.get.id == playerId)) {
      val updatedPlayers = players.filterNot(_.id == playerId)
      if (updatedPlayers.exists(_.isAdmin) || updatedPlayers.isEmpty) {
        (this.copy(players = updatedPlayers), OK)
      } else {
        val x = updatedPlayers.updated(0, updatedPlayers.head.copy(isAdmin = true))
        (this.copy(players = x), OK)
      }
    } else {
      (this, UnauthorizedFailure("You have no permission to do that."))
    }
  }

  private def reduceAdminAction(adminAction: AdminAction, player: Option[Player]): (Universe, UniverseResult) = {
    if (player.isEmpty || !player.get.isAdmin) {
      (this, UnauthorizedFailure("Only an admin can execute admin actions - which you are not."))
    } else {
      adminAction match {
        case SelectGame(gameNumber) => reduceSelect(gameNumber)
        case UnselectGame => reduceUnselectGame()
        case CancelGame => reduceCancel()
        case Start => reduceStart()
        case DelegateAdminRole(idOfTheNewAdmin) => {
          if (players.find(_.isAdmin).map(_.id).contains(idOfTheNewAdmin)) {
            (this, GeneralFailure("You cannot delegate the admin role to yourself, since you are the admin already."))
          } else if (!players.exists(_.id == idOfTheNewAdmin)) {
            (this, GeneralFailure(s"There is no player with id #$idOfTheNewAdmin."))
          } else {
            (this.copy(players = players.map(player => {
              if (player.isAdmin) {
                player.copy(isAdmin = false)
              } else if (player.id == idOfTheNewAdmin) {
                player.copy(isAdmin = true)
              } else {
                player
              }
            })), OK)
          }
        }
        case IncreaseProposedNumberOfPlayers | DecreaseProposedNumberOfPlayers => {
          if (game.isEmpty)
            (this, GeneralFailure("There is no game."))
          else {
            val gameResult = adminAction match {
              case IncreaseProposedNumberOfPlayers => game.get.increaseRoomSize()
              case DecreaseProposedNumberOfPlayers => game.get.decreaseRoomSize()
              case _ => (game.get, OK) // This can never happen
            }
            (this.copy(game = Some(gameResult._1)), gameResult._2)
          }
        }
      }
    }
  }

  private def reduceGameAction(ga: GameAction, invokingPlayer: Option[Player]): (Universe, UniverseResult) = {
    if (game.isDefined) {
      val reduceResult = game.get.reduce(ga, invokingPlayer)
      (this.copy(game = Some(reduceResult._1)), reduceResult._2)
    } else {
      (this, OK)
    }
  }

  private def reduceProvideToken(token: String): (Universe, UniverseResult) = {
    if (!joinInProgress) {
      (this, GeneralFailure("No join is in progress"))
    } else if (players.exists(_.token == token)) {
      (this.copy(candidate = None), GeneralFailure("Token is already in use"))
    } else {
      val player = candidate.get.copy(token = token)
      val frontendPlayer = player.toFrontendPlayer().copy(token=Some(token))
      (this.copy(nextPlayerId = nextPlayerId+1, players = players.appended(player), candidate = None), OKWithPlayerPayload(frontendPlayer))
    }
  }

  private def reduceUnassignRole(role: Int): (Universe, UniverseResult) = {
    (this.copy(players = players.map(p => {
      if (p.role.contains(role)) {
        p.copy(role = None)
      } else {
        p
      }
    })), OK)

  }

  private def reduceJoin(name: String): (Universe, UniverseResult) = {
    val player: Either[Player, String] = {
      if (players.isEmpty)
        Left(Player(nextPlayerId, name, "", isAdmin = true))
      else if (players.size < Universe.MAX_PLAYER_NUMBER) {
        if (players.exists(_.name.equalsIgnoreCase(name)))
          Right("A player with a similar name has already joined.")
        else
          Left(Player(nextPlayerId, name, ""))
      }
      else
        Right("Already full.")
    }
    if (player.isLeft) {
      val theCandidate = player.swap.getOrElse(null)
      (this.copy(candidate = Some(theCandidate)), ExecuteEffect(UniverseEffects.generateToken))
    } else
      (this, GeneralFailure(player.getOrElse("")))
  }

  private def reduceSelect(gameNumber: Int): (Universe, UniverseResult) = {
    if (game.isEmpty)
      if (gameNumber >=0 && gameNumber < RG.registeredGames.size) {
        (this.copy(selectedGame = Some(gameNumber)), OK)
      } else {
        (this, FAIL)
      }
    else
      (this, FAIL)
  }

  private def reduceStart(): (Universe, UniverseResult) = {
    if (game.isEmpty && selectedGame.isDefined && selectedGame.get < RG.registeredGames.size) {
      val result = RG.registeredGames(selectedGame.get).start(this.players.size)
      val temp = this.copy(game = Some(result._1))
      val temp2 = if (result._2.isDefined){
        temp.reduce(result._2.get)._1
      } else {
        temp
      }
      (temp2, OK)
    } else
      (this, FAIL)
  }

  private def reduceCancel(): (Universe, UniverseResult) = {
    if (game.isEmpty)
      (this, FAIL)
    else
      (this.copy(selectedGame = None, game = None, players = players.map(_.copy(role = None))), OK)
  }

  private def reduceUnselectGame(): (Universe, UniverseResult) = {
    if (game.isDefined || selectedGame.isEmpty)
      (this, FAIL)
    else
      (this.copy(selectedGame = None), OK)
  }

  private def reduceLinkRoleToPlayer(linkRoleToPlayer: LinkRoleToPlayer, player: Option[Player]): (Universe, UniverseResult) = {
    def executeLinking(): (Universe, UniverseResult) = {
      val playerId = linkRoleToPlayer.playerId
      val role = linkRoleToPlayer.role
      val target = players.find(player => player.id == playerId)

      if (target.isEmpty) {
        (this, GeneralFailure("No player with such id."))
      } else {
        val isRoleAlreadyTaken: Boolean = players.exists(_.role.contains(role))
        if (isRoleAlreadyTaken) {
          (this, GeneralFailure("Role [" + role + "] is already taken."))
        } else {
          val updatedTarget = target.get.copy(role = Some(role))
          val newPlayers = players.updated(players.indexOf(target.get), updatedTarget)
          (this.copy(players = newPlayers), OK)
        }
      }
    }

    if (player.isEmpty) {
      (this, GeneralFailure("Invalid or missing token."))
    } else {
      val isAdmin = player.get.isAdmin
      val selfLinking = player.get.id == linkRoleToPlayer.playerId
      if (isAdmin || selfLinking) {
        if (game.isEmpty) {
          (this, GeneralFailure("Cannot link while there is no game..."))
        } else if (!game.get.roles.exists(_.roleId == linkRoleToPlayer.role)){
          (this, GeneralFailure("This role is unavailable for the current game."))
        } else {
          executeLinking()
        }
      } else {
        (this, GeneralFailure("Not entitled to do that."))
      }
    }
  }

  private def reduceUnlinkPlayerFromRole(unlinkPlayerFromRole: UnlinkPlayerFromRole, player: Option[Player]): (Universe, UniverseResult) = {
    if (player.isEmpty) {
      (this, GeneralFailure("Invalid or missing token."))
    } else {
      val isAdmin = player.get.isAdmin
      val selfLinking = player.get.id == unlinkPlayerFromRole.playerId
      if (isAdmin || selfLinking) {
        if (game.isEmpty) {
          (this, GeneralFailure("Cannot unlink while there is no game..."))
        } else {
          val playerId = unlinkPlayerFromRole.playerId
          val target = players.find(player => player.id == playerId)

          if (target.isEmpty) {
            (this, GeneralFailure("No player with such id."))
          } else {
            val targetHasNoRole: Boolean = target.get.role.isEmpty
            if (targetHasNoRole) {
              (this, GeneralFailure("Player has no role to revoke."))
            } else {
              val target2 = target.get.copy(role = None)
              val newPlayers = players.map(p => {
                if (p.id == playerId)
                  target2
                else
                  p
              })
              (this.copy(players = newPlayers), OK)
            }
          }
        }
      } else {
        (this, UnauthorizedFailure("Not entitled to do that."))
      }
    }
  }

  private def reduceNaturalLink(roles: List[Int]): (Universe, UniverseResult) = {
    val playersUpdatedWithRoles = for (player <- this.players.zipWithIndex) yield
      if (player._2 < roles.size)
        player._1.copy(role = Some(roles(player._2)))
      else
        player._1
    (this.copy(players=playersUpdatedWithRoles), OK)
  }

  def getFrontendUniverseForPlayer(playerId: Option[Int] = None): FrontendUniverse = {
    val role: Option[Int] = playerId.flatMap(pi => players.find(_.id == pi)).flatMap(_.role)
    FrontendUniverse(selectedGame, game.map(_.toFrontendGame(role)), this.players.map(_.toFrontendPlayer()))
  }

  def getPlayerWithToken(token: String): Option[Player] = players.find(_.token == token)

}

object Universe {
  val MAX_PLAYER_NUMBER: Int = 12
}
