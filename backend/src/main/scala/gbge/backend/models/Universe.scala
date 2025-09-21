package gbge.backend.models

import gbge.backend.*
import gbge.backend.services.TokenGenerator
import gbge.shared.{FrontendGame, FrontendUniverse}
import gbge.shared.actions.*
import zio.{IO, ZIO}

import scala.language.implicitConversions

object UniverseEffects {
//  val generateToken: Universe => UIO[List[Action]] = universe => {
//    val token = universe.generateToken()
//    ZIO.succeed(List(ProvideToken(token)))
//  }
}

case class Universe(supportedGames: Seq[BackendGameProps[_,_]],
                    selectedGame: Option[Int] = None,
                    game: Option[BackendGame[_ <: GameAction, _ <: FrontendGame[_ <: GameAction]]] = None,
                    //                    players: List[Player] = List.empty,
                    players: Map[String, Player] = Map.empty,
                    candidate: Option[Player] = None,
                    //                    tokenIdMap: Map[String, Int] = Map.empty,
                    nextPlayerId: Int = 1) {

//  def reduceJoin(name: String): Either[String, (Int, Universe)] = {
  private def reduceJoin(name: String): Either[Failure, (Universe, ZIO[TokenGenerator, Failure, Seq[Action]])] = {
    candidate match
      case Some(_) => Left(GeneralFailure("join is already in progress"))
      case None =>
        if (players.isEmpty)
          Right((
            this.copy(candidate = Some(Player(nextPlayerId, name, isAdmin = true))),
            ZIO.serviceWithZIO[TokenGenerator](_.generateToken).map(token => Seq(ProvideToken(token)))
          ))
        else if (players.size < Universe.MAX_PLAYER_NUMBER) {
          if (players.exists(_._2.name.equalsIgnoreCase(name)))
            Left(GeneralFailure("A player with a similar name has already joined."))
          else
            Right((
              this.copy(candidate = Some(Player(nextPlayerId, name))),
              ZIO.serviceWithZIO[TokenGenerator](_.generateToken).map(token => Seq(ProvideToken(token)))
            ))
        }
        else
          Left(GeneralFailure("A player with a similar name has already joined."))
  }

  // unsafe; invariant must be maintained by the caller
  private def addTokenToCandidate(token: String): Universe = {
    this.candidate match
      case None => throw new RuntimeException("should never happen")
      case Some(c) =>
        this.copy(
          candidate = None,
          players = players + (token -> c),
          nextPlayerId = nextPlayerId + 1,
        )
  }

  private def getPlayerFromId(id: Int): Option[Player] = {
    players.values.find(_.id == id)
  }

//  def generateToken(): String = {
//    val randomTokens: Boolean = System.getProperty("randomTokens", "true").toBooleanOption.getOrElse(true)
//
//    val randomSuffix = if (randomTokens)
//      scala.util.Random.between(1,10000).toString
//    else ""
//
//    (nextPlayerId + 100).toString + randomSuffix
//  }

  def reduce(action: Action, playerId: Option[Int] = None): Either[Failure, (Universe, ZIO[TokenGenerator, Failure, Seq[Action]])] = {
    val player: Option[Player] = playerId.flatMap(id => getPlayerFromId(id))
    action match {
      case Join(name) => reduceJoin(name)
      case ProvideToken(token) => {
//        Right(addTokenToCandidate(token), ZIO.succeed(Seq.empty))
        addTokenToCandidate(token)
      }
      case UnassignRole(role) => reduceUnassignRole(role)
      case KickPlayer(playerId) => reduceKickPlayer(playerId, player.get)
      case adminAction: AdminAction => reduceAdminAction(adminAction, player.get)
      case linkRoleToPlayer: LinkRoleToPlayer => reduceLinkRoleToPlayer(linkRoleToPlayer, player.get)
      case unlinkPlayerFromRole: UnlinkPlayerFromRole => reduceUnlinkPlayerFromRole(unlinkPlayerFromRole, player.get)
      case NaturalLink(roles) => reduceNaturalLink(roles)
      case ga: GameAction => reduceGameAction(ga, player.get)
      case _ => ???
    }
  }

  implicit def conversion1(universe: Universe): Either[Failure, (Universe, IO[Failure, Seq[Action]])] = Right(universe, ZIO.succeed(Seq.empty))
  implicit def conversion2(failure: Failure): Either[Failure, (Universe, IO[Failure, Seq[Action]])] = Left(failure)

  private def reduceKickPlayer(playerId: Int, player: Player): Either[Failure, (Universe, IO[Failure, Seq[Action]])] = {
     if (player.isAdmin || player.id == playerId) {
      players.find(pair => pair._2.id == playerId) match {
        case Some((tokenOfPlayerToKick, player)) if !player.isAdmin =>
          this.copy(players = players - tokenOfPlayerToKick)
        case Some((tokenOfPlayerToKick, player)) if player.isAdmin =>
          val temp = this.copy(players = players - tokenOfPlayerToKick)
          if (temp.players.isEmpty)
            temp
          else
            val newAdmin = temp.players.minBy(_._2.id)
            temp.copy(players = players.updatedWith(newAdmin._1)({
              case Some(player) => Some(player.copy(isAdmin = true))
              case None => None
            }))
        case None => GeneralFailure(s"no player with if $playerId")
      }
    } else {
      UnauthorizedFailure("You have no permission to do that.")
    }
  }

  private def reduceAdminAction(adminAction: AdminAction, player: Player): Either[Failure, (Universe, ZIO[TokenGenerator, Failure, Seq[Action]])]  = {
    if (!player.isAdmin) {
      UnauthorizedFailure("Only an admin can execute admin actions - which you are not.")
    } else {
      adminAction match {
        case SelectGame(gameNumber) => reduceSelect(gameNumber)
        case UnselectGame => reduceUnselectGame()
        case CancelGame => reduceCancel()
        case Start => reduceStart(player)
        case DelegateAdminRole(idOfTheNewAdmin) =>
          if (players.values.find(_.isAdmin).map(_.id).contains(idOfTheNewAdmin)) {
            GeneralFailure("You cannot delegate the admin role to yourself, since you are the admin already.")
          } else if (!players.exists(_._2.id == idOfTheNewAdmin)) {
            GeneralFailure(s"There is no player with id #$idOfTheNewAdmin.")
          } else {
            this.copy(players = players.map(player => {
              if (player._2.isAdmin) {
                val p = player._2
                val p2 = p.copy(isAdmin = false)
                player.copy(_2 = p2)
              } else if (player._2.id == idOfTheNewAdmin) {
                val p = player._2
                val p2 = p.copy(isAdmin = true)
                player.copy(_2 = p2)
              } else {
                player
              }
            }))
          }
        case IncreaseProposedNumberOfPlayers | DecreaseProposedNumberOfPlayers =>
          if (game.isEmpty)
            GeneralFailure("There is no game.")
          else {
            val gameResult = adminAction match {
              case IncreaseProposedNumberOfPlayers => game.get.increaseRoomSize()
              case DecreaseProposedNumberOfPlayers => game.get.decreaseRoomSize()
              case _ => ??? // This can never happen
            }
            gameResult match
              case Left(failure) => failure
              case Right(game) => this.copy(game = Some(game))
          }
      }
    }
  }

  private def reduceGameAction(ga: GameAction, invokingPlayer: Player): Either[Failure, (Universe, IO[Failure, Seq[Action]])]= {
    if (game.isDefined) {
      val reduceResult = game.get.reduce(ga, invokingPlayer)
      reduceResult match
        case Left(error) => Left(error)
        case Right((reducedGame, effect)) => Right(this.copy(game = Some(reducedGame)), effect) 
    } else {
      GeneralFailure("no game is defined")
    }
  }

//  private def reduceProvideToken(token: String): (Universe, UniverseResult) = {
//    if (!joinInProgress) {
//      (this, GeneralFailure("No join is in progress"))
//    } else if (players.exists(_.token == token)) {
//      (this.copy(candidate = None), GeneralFailure("Token is already in use"))
//    } else {
//      val player = candidate.get.copy(token = token)
//      val frontendPlayer = player.toFrontendPlayer().copy(token=Some(token))
//      (this.copy(nextPlayerId = nextPlayerId+1, players = players.appended(player), candidate = None), OKWithPlayerPayload(frontendPlayer))
//    }
//  }

  private def reduceUnassignRole(role: Int): Either[Failure, (Universe, IO[Failure, Seq[Action]])] = {
    this.copy(players = players.map(p => {
      if (p._2.role.contains(role)) {
        p._1 -> p._2.copy(role = None)
      } else {
        p
      }
    }))

  }

//  def addTokenToCandidate(token: String): Universe =
//    this.candidate match
//      case Left(_) => throw new RuntimeException("should never happen")
//      case Right(c) => 
//        this.copy(
//          candidate = Left(""),
////          tokenIdMap = tokenIdMap.updated(token, c.id),
////          players = players.appended(c),
//          nextPlayerId = nextPlayerId + 1,
//        )
  
//  private def reduceJoin(name: String): (Universe, UniverseResult) = {
//    val player: Either[Player, String] = {
//      if (players.isEmpty)
//        Left(Player(nextPlayerId, name, isAdmin = true))
//      else if (players.size < Universe.MAX_PLAYER_NUMBER) {
//        if (players.exists(_.name.equalsIgnoreCase(name)))
//          Right("A player with a similar name has already joined.")
//        else
//          Left(Player(nextPlayerId, name))
//      }
//      else
//        Right("Already full.")
//    }
//    if (player.isLeft) {
//      val theCandidate = player.swap.getOrElse(null)
//      (this.copy(candidate = Some(theCandidate)), ExecuteEffect(UniverseEffects.generateToken))
//    } else
//      (this, GeneralFailure(player.getOrElse("")))
//  }

  private def reduceSelect(gameNumber: Int): Either[Failure, (Universe, IO[Failure, Seq[Action]])] = {
    if (game.isEmpty)
      if (gameNumber >=0 && gameNumber < supportedGames.size) {
        this.copy(selectedGame = Some(gameNumber))
      } else {
        FAIL
      }
    else
      FAIL
  }

  private def reduceStart(player: Player): Either[Failure, (Universe, ZIO[TokenGenerator, Failure, Seq[Action]])]  = {
    if (game.isEmpty && selectedGame.isDefined && selectedGame.get < supportedGames.size) {
      val result = supportedGames(selectedGame.get).start(this.players.size)
      val temp = this.copy(game = Some(result._1))
      if (result._2.isDefined){
        temp.reduce(result._2.get, Some(player.id))
      } else {
        temp
      }
    } else
      FAIL
  }

  private def reduceCancel(): Either[Failure, (Universe, IO[Failure, Seq[Action]])]  = {
    if (game.isEmpty)
      FAIL
    else
      this.copy(selectedGame = None, game = None, players = players.map(pair => pair.copy(
        _2 = pair._2.copy(role = None)
      )))
  }

  private def reduceUnselectGame(): Either[Failure, (Universe, IO[Failure, Seq[Action]])] = {
    if (game.isDefined || selectedGame.isEmpty)
      FAIL
    else
      this.copy(selectedGame = None)
  }
//
  private def reduceLinkRoleToPlayer(linkRoleToPlayer: LinkRoleToPlayer, player: Player): Either[Failure, (Universe, IO[Failure, Seq[Action]])] = {
    lazy val executeLinking: Either[Failure, Universe] = {
      val playerId = linkRoleToPlayer.playerId
      val role = linkRoleToPlayer.role
      val tokenAndTarget = players.find(pair => pair._2.id == playerId)

      if (tokenAndTarget.isEmpty) {
        Left(GeneralFailure("No player with such id."))
      } else {
        val isRoleAlreadyTaken: Boolean = players.values.exists(_.role.contains(role))
        if (isRoleAlreadyTaken) {
          Left(GeneralFailure("Role [" + role + "] is already taken."))
        } else {
          val (token, target) = tokenAndTarget.get
          val updatedTarget = target.copy(role = Some(role))
          val newPlayers = players.updated(token, updatedTarget)
          Right(this.copy(players = newPlayers))
        }
      }
    }
    
    val isAdmin = player.isAdmin
    val selfLinking = player.id == linkRoleToPlayer.playerId
    if (isAdmin || selfLinking) {
      if (game.isEmpty) {
        GeneralFailure("Cannot link while there is no game...")
      } else if (!game.get.roles.exists(_.roleId == linkRoleToPlayer.role)){
        GeneralFailure("This role is unavailable for the current game.")
      } else {
        executeLinking match
          case Left(failure) => failure
          case Right(u) => u
      }
    } else {
      Left(UnauthorizedFailure("Not entitled to do that."))
    }
  }
  

  private def reduceUnlinkPlayerFromRole(unlinkPlayerFromRole: UnlinkPlayerFromRole, player: Player): Either[Failure, (Universe, IO[Failure, Seq[Action]])] = {
      val isAdmin = player.isAdmin
      val selfLinking = player.id == unlinkPlayerFromRole.playerId
      if (isAdmin || selfLinking) {
        if (game.isEmpty) {
          GeneralFailure("Cannot unlink while there is no game...")
        } else {
          val playerId = unlinkPlayerFromRole.playerId
          val target = players.values.find(player => player.id == playerId)

          if (target.isEmpty) {
            GeneralFailure("No player with such id.")
          } else {
            val targetHasNoRole: Boolean = target.get.role.isEmpty
            if (targetHasNoRole) {
              GeneralFailure("Player has no role to revoke.")
            } else {
              val target2 = target.get.copy(role = None)
              val newPlayers = players.map(p => {
                if (p._2.id == playerId)
                  p._1 -> target2
                else
                  p
              })
              this.copy(players = newPlayers)
            }
          }
        }
      } else {
        UnauthorizedFailure("Not entitled to do that.")
      }
  }

  private def reduceNaturalLink(roles: List[Int]): Either[Failure, (Universe, IO[Failure, Seq[Action]])] = {
    val playersUpdatedWithRoles = (for (kvp <- this.players.zipWithIndex) yield
      if (kvp._2 < roles.size)
        kvp._1._1 -> kvp._1._2.copy(role = Some(roles(kvp._2)))
      else
        kvp._1).toMap
    this.copy(players= playersUpdatedWithRoles)
  }
//
  def getFrontendUniverseForPlayer(playerId: Option[Int] = None): FrontendUniverse = {
//    val role: Option[Int] = playerId.flatMap(pi => players.find(_.id == pi)).flatMap(_.role)
    FrontendUniverse(selectedGame, game.map(_.toFrontendGame(None)), this.players.values.map(_.toFrontendPlayer()).toList.sortBy(_.id))
  }
//
//  def getPlayerWithToken(token: String): Option[Player] = ??? 
//    //players.find(_.token == token)

}

object Universe {
  val MAX_PLAYER_NUMBER: Int = 12
}
