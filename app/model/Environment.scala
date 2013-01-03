package model

import gate._
import comet._
import model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import play.api.Logger
import concurrent.Await
import concurrent.duration._
import state.ArtifactServer

// TODO is this right in a play app?
import scala.concurrent.ExecutionContext.Implicits.global

object Environment {
  // replace these with name lookups?
  var actorSystem: ActorSystem = _

  def start {
    actorSystem = ActorSystem.create("YogSothoth")
    val gatewayServer = actorSystem.actorOf(Props[GatewayServer], "GatewayServer")
    val artifactServer = actorSystem.actorOf(Props[ArtifactServer], "ArtifactServer")
    val lurker = actorSystem.actorOf(Props(new Lurker(actorSystem.deadLetters, artifactServer, gatewayServer)), "Lurker")
    val threshold = actorSystem.actorOf(Props(new Threshold(ProcesssImpl)), "Threshold")
    val watcher = actorSystem.actorOf(Props(new Watcher(threshold, lurker, gatewayServer)), "Watcher")
    actorSystem.scheduler.schedule(1 minute, 5 minutes, watcher, 'Wake)
    actorSystem.scheduler.schedule(2 minutes, 2 minutes, watcher, 'Close)
    actorSystem.scheduler.schedule(10 minutes, 10 minutes, watcher, 'Unlockable)
    val summoner = actorSystem.actorOf(Props(new Summoner(lurker, watcher)), "Summoner")
    implicit val timeout = Timeout(60 seconds)
    val future = ask(summoner, 'Check)
    Await.result(future, timeout.duration)
    actorSystem.scheduler.schedule(3 minutes, 5 minutes, summoner, 'Wake)
    lurker ! 'Flush
//    val manipulator: ActorRef = actorSystem.actorOf(Props(new Manipulator(watcher, artifactServer)), "Manipulator")
  }

  def dispose {
    actorSystem.shutdown()
  }
}
