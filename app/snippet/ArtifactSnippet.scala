//package snippet
//
//import xml.NodeSeq
//import _
//import _root_.net.liftweb.util._
//import _root_.net.liftweb.common._
//import _root_.net.liftweb.http._
//import _root_.net.liftweb.http.js._
//import Helpers._
//import comet.{ArtifactSearch, SearchInput, ArtifactBinding}
//
//object searchText extends RequestVar[Option[String]](None)
//
//class ArtifactSnippet extends ArtifactBinding {
//  def searcher = {
//    if (model.Cultist.attending_?) {
//      ClearClearable &
//      ".search:text" #> JsCmds.FocusOnLoad(SHtml.text(searchText.is.getOrElse(""), t => searchText(Some(t)), "placeholder" -> "Search")) &
//      "#search:submit" #> SHtml.submit("Search", () => processSearch)
//    } else {
//      "#searcher" #> NodeSeq.Empty
//    }
//  }
//
//  def processSearch {
//    val tmp = searchText.is.getOrElse("")
//    S.redirectTo("search", () => {
//      S.session.foreach(_.sendCometActorMessage("ArtifactSearch", Empty, SearchInput(tmp)))
//      searchText(Some(tmp))
//    })
//  }
//}