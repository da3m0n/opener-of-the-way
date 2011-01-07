package code.model

import org.squeryl.Schema
import net.liftweb.squerylrecord.RecordTypeMode._

object Mythos extends Schema {
  val cultists = table[Cultist]
  val gateways = table[Gateway]
  val artifacts = table[Artifact]

  val cultistToGateways = oneToManyRelation(cultists, gateways).via((c,g) => c.id === g.cultistId)
  val gatewayToArtifacts = oneToManyRelation(gateways, artifacts).via((g,a) => g.id === a.gatewayId)
}