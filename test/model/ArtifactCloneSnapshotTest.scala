/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package model

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import model.Mythos._
import gate.{Millis, T}
import java.sql.Timestamp
import db.TestDb
import test.WithTestApplication


class ArtifactCloneSnapshotTest extends Specification with Mockito {
  val notNewsAfterDefault = Millis.days(365) * 1000;

  def clearTransactions() {
    import org.squeryl.PrimitiveTypeMode._
    inTransaction{
      val time1 = T.at(2011, 3, 20, 1, 2, 3)
      val time2 = T.at(2011, 3, 22, 1, 2, 3)
      artifacts.delete(from(artifacts)(a => select(a)))
      artifacts.insert(Artifact.create(1L, "a/b/c", time1, T.now))
      artifacts.insert(Artifact.create(2L, "fudge", time1, T.now))
      artifacts.insert(Artifact.create(1L, "d/e/f", time2, T.now))
      artifacts.insert(Artifact.create(2L, "sugar", time2, T.now))
      artifacts.insert(Artifact.create(2L, "chocolate", time2, T.ago(Artifact.lostAfter + 1)))
      artifacts.insert(Artifact.create(2L, "marshmallow", time2, T.ago(Artifact.goneAfter + 1)))
    }
    inTransaction(presences.delete(from(presences)(p => select(p))))
    inTransaction(clones.delete(from(clones)(c => select(c))))
  }

  "ArtifactCloneSnapshot" should {

    "load all artifacts" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      clearTransactions()
      val x = new ArtifactCloneSnapshot(notNewsAfterDefault)
      x.reload(1)
      x.states.size mustEqual(5)

      x.reload(2)
      x.states.size mustEqual(5)      
    }

    "group artifacts by discovery, order by path" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      clearTransactions()
      val x = new ArtifactCloneSnapshot(notNewsAfterDefault)
      x.reload(1)
      x.items.keys.toSeq mustEqual("2011-04-20" :: "2011-04-22" :: Nil)
      x.items.get("2011-04-20").map(as => as.map(a => a.path)) must beSome("a/b/c" :: "fudge" :: Nil)
    }

    "default state to mine or available if no clones exist" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      clearTransactions()
      val x = new ArtifactCloneSnapshot(notNewsAfterDefault)
      x.reload(2)
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.glimpsed, 0))
      x.states.get(i + 1) must beSome(ArtifactCloneInfo(ArtifactState.proffered, 0))
      x.states.get(i + 2) must beSome(ArtifactCloneInfo(ArtifactState.glimpsed, 0))
      x.states.get(i + 3) must beSome(ArtifactCloneInfo(ArtifactState.proffered, 0))
      x.states.get(i + 4) must beSome(ArtifactCloneInfo(ArtifactState.profferedLost, 0))
      x.states.get(i + 5) must beNone
    }

    "reflect clone state" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      clearTransactions()
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      val myClone = inTransaction(clones.insert(Clone.create(i, 2L, CloneState.awaiting)))
      val theirClone = inTransaction(clones.insert(Clone.create(i, 1L, CloneState.cloning)))
      val x = new ArtifactCloneSnapshot(notNewsAfterDefault)
      x.reload(2L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.awaiting, 2))
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.proffered, 2))

      myClone.state = CloneState.cloning
      inTransaction(clones.update(myClone))
      x.reload(2L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.cloning, 2))
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.proffered, 2))

      myClone.state = CloneState.cloned
      inTransaction(clones.update(myClone))
      x.reload(2L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.cloned, 2))
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.proffered, 2))
    }

    "reflect presence state" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      clearTransactions()
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      inTransaction(presences.insert(Presence.create(i, PresenceState.present)))

      val x = new ArtifactCloneSnapshot(notNewsAfterDefault)
      x.reload(2L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.present, 0))
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.profferedPresent, 0))

      val myClone = inTransaction(clones.insert(Clone.create(i, 2L, CloneState.awaiting)))

      x.reload(2L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.awaitingPresent, 1))
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.profferedPresent, 1))

      myClone.state = CloneState.cloning
      inTransaction(clones.update(myClone))
      x.reload(2L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.cloning, 1))
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.profferedPresent, 1))

      myClone.state = CloneState.cloned
      inTransaction(clones.update(myClone))
      x.reload(2L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.cloned, 1))
      x.reload(1L)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.profferedPresent, 1))
    }

    "allow artifacts to be updated (including bug with present)" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      clearTransactions()
      val x = new ArtifactCloneSnapshot(notNewsAfterDefault)
      x.reload(2)
      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.glimpsed, 0))

      val myClone = inTransaction(clones.insert(Clone.create(i, 2L, CloneState.awaiting)))
      val theirClone = inTransaction(clones.insert(Clone.create(i, 1L, CloneState.cloned)))
      x.update(i)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.awaiting, 2))

      val presence = inTransaction(presences.insert(Presence.create(i, PresenceState.present)))
      x.update(i)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.awaitingPresent, 2))
      
      inTransaction( clones.deleteWhere(c => c.id === myClone.id) )
      x.update(i)
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.present, 1))
    }

    "not exclude items cloned by others (bug)" in new WithTestApplication {
      import org.squeryl.PrimitiveTypeMode._
      clearTransactions()

      val i = inTransaction(from(artifacts)(a => select(a.id) orderBy(a.id asc)).headOption) getOrElse -1L

      inTransaction(clones.insert(Clone.create(i + 2, 3L, CloneState.cloning)))
      inTransaction(clones.insert(Clone.create(i + 3, 3L, CloneState.awaiting)))

      val x = new ArtifactCloneSnapshot(notNewsAfterDefault)
      x.reload(2)
      
      x.states.get(i) must beSome(ArtifactCloneInfo(ArtifactState.glimpsed, 0))
      x.states.get(i + 1) must beSome(ArtifactCloneInfo(ArtifactState.proffered, 0))
      x.states.get(i + 2) must beSome(ArtifactCloneInfo(ArtifactState.glimpsed, 1))
      x.states.get(i + 3) must beSome(ArtifactCloneInfo(ArtifactState.proffered, 1))
      x.states.get(i + 4) must beSome(ArtifactCloneInfo(ArtifactState.profferedLost, 0))
      x.states.get(i + 5) must beNone
    }
  }
}
