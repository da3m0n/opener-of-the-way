package code.model

import org.squeryl.Query
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.annotations.Column
import code.gate.T
import code.model.Mythos._
import org.squeryl.PrimitiveTypeMode._
import java.io.File
import java.sql.Timestamp
import xml.{Unparsed, Node}

class Gateway extends MythosObject {
  var cultistId: Long = 0
  var location: String = "" // hostname/sharename
  var path: String = "" // folder/subfolder/tcfilename
  var localPath: String = "" // /folder/subfolder
  var password: String = "" // storing in cleartext as none should have access to db
  var mode: GateMode.Value = GateMode.source
  var state: GateState.Value = GateState.lost
  var stateDesc: String = ""
  var scoured: Timestamp = T.yesterday
  var seen: Timestamp = T.yesterday

  lazy val cultist: ManyToOne[Cultist] = Mythos.cultistToGateways.right(this)
  lazy val artifacts: OneToMany[Artifact] = Mythos.gatewayToArtifacts.left(this)

  def clonesPath: String = new File(localPath, "clones").getPath

  def description: String = new File(location, path).getPath

  override def toString = "Gateway[" + location + "," + path + "," + mode + "]"
}

object Gateway {
  lazy val viableDestinations: Query[Gateway] = {
    gateways.where(g =>
      g.mode === GateMode.sink and
      g.state === GateState.open)
  }

  lazy val symbolQuestion = <img src="/static/g_help.png" title="Did this slip your mind?"/>
  lazy val symbolWarning = <img src="/static/g_exclamation_lesser.png" title="Are you sure?!"/>
  lazy val symbolExclamation = <img src="/static/g_exclamation.png" title="Are you crazy?!!"/>
}

object GateMode extends Enumeration {
  type GateMode = Value

  val source = Value("source (read only)")
  val sink = Value("sink (write only)")

  def parse(text: String): GateMode = text match {
    case "sink (write only)" => sink
    case _ => source
  }

  def symbol(s: GateMode.Value): Node = s match {
    case GateMode.source => <img src="/static/g_source.png" title="Source"/>
    case GateMode.sink => <img src="/static/g_sink.png" title="Sink"/>
    case _ => Unparsed("&nbsp;")
  }
}

object GateState extends Enumeration {
  type GateState = Value

  val open = Value("open")
  val inactive = Value("closed")
  val lost = Value("lost")

  def symbol(s: GateState.Value): Node = s match {
    case GateState.open => <img src="/static/g_open.png" title="Open"/>
    case GateState.inactive => <img src="/static/g_inactive.png" title="Closed"/>
    case GateState.lost => <img src="/static/g_lost.png" title="Lost"/>
    case _ => Unparsed("&nbsp;")
  }
}
