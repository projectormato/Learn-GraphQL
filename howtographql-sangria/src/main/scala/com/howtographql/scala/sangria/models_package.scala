package com.howtographql.scala.sangria
// add "Custom Scalars" chapter
import akka.http.scaladsl.model.DateTime
import sangria.validation.Violation
import sangria.execution.deferred.HasId

package object models {
  trait Identifiable {
    val id: Int
  }
  object Identifiable {
      implicit def hasId[T <: Identifiable]: HasId[T, Int] = HasId(_.id)
  }
  // case class Link(id: Int, url: String, description: String)
  // modify "Custom Scalars" chapter
  case class Link(id: Int, url: String, description: String, createdAt: DateTime) extends Identifiable

  // add "Interfaces" chapter
  case class User(id: Int, name: String, email: String, password: String, createdAt: DateTime = DateTime.now) extends Identifiable

  case class Vote(id: Int, userId: Int, linkId: Int, createdAt: DateTime = DateTime.now) extends Identifiable

  case object DateTimeCoerceViolation extends Violation {
    override def errorMessage: String = "Error during parsing DateTime"
  }
}
