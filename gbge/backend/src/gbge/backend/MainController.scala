package gbge.backend

import gbge.shared.FrontendUniverse
import gbge.shared.actions.{Action, GeneralAction, InvalidAction}
import gbge.shared.tm.{Perspective, PortalCoordinates}
import os.RelPath
import io.undertow.websockets.core._
import zio.{UIO, ZIO}

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

class MainController(hardCodedActions: List[(Action, Option[String])] = List.empty) {
  val fileToRecoverFrom: Option[String] = Option(System.getProperty("loadPath"))

  var timeMachine: TimeMachine = TimeMachine()

  var universe: Universe = hardCodedActions.foldLeft(Universe())((universe, action) => {
    timeMachine = timeMachine.addAction(action._1, action._2)
    universe.reduce(action._1, action._2)._1
  })
  var universeResult: UniverseResult = OK

  var nextPortalId = 1
  var portals: List[Portal] = List.empty

  private var channels: Set[(WebSocketChannel, Option[Int])] = Set.empty // channel, playerId

  def performAction(action: Action, playerToken: Option[String]): (Universe, UniverseResult) = this.synchronized {
    val result = handleActionOnUniverse(universe, action, playerToken)
    universe = result._1
    notifyClientsViaWebSocket()
    result
  }

  def handleActionOnUniverse(universe: Universe, action: Action, playerToken: Option[String] = None): (Universe, UniverseResult) = {
    timeMachine = timeMachine.addAction(action, playerToken)
    val (u, r) = universe.reduce(action, playerToken)
    r match {
      case ExecuteEffect(effect) => {
        val eEffect: UIO[List[Action]] = effect(u).foldM(error => UIO.effectTotal {
          println("Error during effect execution: " + error)
          List.empty
        }, success => ZIO.succeed(success))
        val additionalActions = zio.Runtime.default.unsafeRun(eEffect)
        val first = (u, OK)
        val result = additionalActions.foldLeft[(Universe, UniverseResult)](first) { (tuple, action) => {
          handleActionOnUniverse(tuple._1, action, None)
        }}
        result
      }
      case ExecuteAsyncEffect(effect) => {
        val eEffect: UIO[List[Action]] = effect(u).foldM(error => UIO.effectTotal {
          println("Error during async effect execution: " + error)
          List.empty
        }, success => ZIO.succeed(success))
        // Currently multiple additional actions are not supported spawning from an async effect
        Future {
          val additionalActions = zio.Runtime.default.unsafeRun(eEffect)
          this.performAction(additionalActions.head, None)
        }.foreach(_ => notifyClientsViaWebSocket())
        (u, r)
      }
      case _ => (u, r)
    }
  }

  def setupStateSocketConnection(channel: WebSocketChannel): Unit = this.synchronized {
    channel.getReceiveSetter.set(new AbstractReceiveListener {
      override def onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage): Unit = {
        super.onFullTextMessage(channel, message)
        val token = message.getData
        val playerId = universe.players.find(_.token == token).map(_.id)
        val a = channels.find(_._1 == channel)
        if (a.isDefined) {
          channels = channels.-(a.get).incl((a.get._1, playerId))
        }
        if (channel.isOpen) {
          if (playerId.isEmpty)
            WebSockets.sendTextBlocking(universe.getFrontendUniverseForPlayer().serialize(), channel)
          else
            WebSockets.sendTextBlocking(universe.getFrontendUniverseForPlayer(playerId).serialize(), channel)
        }
      }
    })
    channel.resumeReceives()
    channels = channels.+((channel, None))
  }

  def notifyClientsViaWebSocket(): Unit = {
    for (channel <- channels) {
      try {
        if (channel._1.isOpen) {
          if (channel._2.isEmpty)
            WebSockets.sendTextBlocking(universe.getFrontendUniverseForPlayer().serialize(), channel._1)
          else {
            WebSockets.sendTextBlocking(universe.getFrontendUniverseForPlayer(channel._2).serialize(), channel._1)
          }
        }
      } catch {
        case exception: Exception => println("some exception: " + exception)
      }
    }

    for (channel <- channels) {
      if (!channel._1.isOpen) {
        channels = channels.filterNot(_ == channel)
      }
    }
  }

  def modifyPortalCoordinates(a: PortalCoordinates): Unit = {
    val portal = portals.find(_.id == a.portalId)
    if (portal.isDefined) {
      portals = portals.map(e => {
        if (e.id == a.portalId) {
          e.coordinates = Some(a)
          e
        } else
          e
      })
      notifyPortal(portal.get)
    }
  }

  def notifyPortal(portal: Portal): Unit = {
    val selectedPerspective = portal.coordinates.flatMap(_.selectedPerspective)
    val actionNumber = portal.coordinates.flatMap(_.actionNumber)
    if (selectedPerspective.isDefined && actionNumber.isDefined) {
      val fu = Try(timeMachine.getFrontendUniverseForPerspective(actionNumber.get, selectedPerspective.get))
      if (fu.isSuccess && fu.get.isRight) {
        val fu2: FrontendUniverse = fu.get.getOrElse(null)._1
        if (fu2 != null) {
          val payload: (Perspective, String) = (selectedPerspective.get, fu2.serialize())
          for (channel <- portal.clients) {
            if (channel.isOpen)
              WebSockets.sendTextBlocking(upickle.default.write(payload), channel)
          }
        }
      }
    }
  }

  def createNewPortal(webSocketChannel: WebSocketChannel, portalId: Option[Int] = None): Int = {
    webSocketChannel.getReceiveSetter.set(new AbstractReceiveListener {
      override def onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage): Unit = {
        super.onFullTextMessage(channel, message)
      }
    })
    val idOfTheNewPortal = portalId.getOrElse(nextPortalId)
    val portal = new Portal(idOfTheNewPortal, List(webSocketChannel))
    portals = portal :: portals
    while (portals.exists(_.id == nextPortalId))
      nextPortalId = nextPortalId+1
    idOfTheNewPortal
  }

  def addControllerToAnExistingPortal(webSocketChannel: WebSocketChannel, portalId: Int): Int = {
    val portal = portals.find(_.id == portalId)
    if (portal.isDefined) {
      webSocketChannel.getReceiveSetter.set(new AbstractReceiveListener {
        override def onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage): Unit = {
          super.onFullTextMessage(channel, message)
        }
      })
      portal.get.controllers = webSocketChannel :: portal.get.controllers
      portal.get.id
    } else 0
  }

  def joinToAnExistingPortal(webSocketChannel: WebSocketChannel, portalId: Int): Unit = {
    val portal = portals.find(_.id == portalId)
    if (portal.isDefined) {
      webSocketChannel.getReceiveSetter.set(new AbstractReceiveListener {
        override def onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage): Unit = {
          super.onFullTextMessage(channel, message)
        }
      })
      portal.get.clients = webSocketChannel :: portal.get.clients
    }
  }

  recover(fileToRecoverFrom)

  def reset(): Unit = {
    universe = Universe()
    timeMachine = TimeMachine()
  }

  def save(): Boolean = {
    if (MainController.timeMachineEnabled()) {
      import java.text.SimpleDateFormat
      import java.util.Calendar
      val dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
      val now = Calendar.getInstance().getTime
      val dateAsString = dateFormat.format(now)
      val fileName = dateAsString + ".dat"
      os.write.over(os.pwd / fileName, timeMachine.serialize())
      true
    } else {
      false
    }
  }

  private def recover(path: Option[String]): Unit = {
    if (path.isDefined) {
      val x = os.pwd / RelPath(path.get)
      if (os.exists(x)) {
        val x2 = os.read(x)
        parse(x2).foreach(tm => {
          timeMachine = tm
          universe = timeMachine.latestUniverse._1
        })
      }
    }
  }

  def parseActionWithUniverse(payload: String, universe: Universe): Action = {
    val generalAction = Try(upickle.default.read[GeneralAction](payload))
    if (generalAction.isSuccess) {
      generalAction.get
    } else {
      val gameAction = Try(universe.game.get.decodeAction(payload))
      gameAction.getOrElse(InvalidAction(payload))
    }
  }

  private def parse(content: String): Option[TimeMachine] = {
    try {
      val tm = TimeMachine.decode(content)
      Some(tm)
    } catch {
      case _: Throwable => None
    }
  }
}

object MainController {
  def timeMachineEnabled(): Boolean = {
    System.getProperty("tmEnabled", "false").toBooleanOption.getOrElse(false)
  }
}