package com.howtographql.scala.sangria
import DBSchema._
import slick.jdbc.H2Profile.api._
// add "Handling Arguments" chapter
import com.howtographql.scala.sangria.models.Link
import com.howtographql.scala.sangria.models.User
import scala.concurrent.Future

class DAO(db: Database) {
  def allLinks = db.run(Links.result)

  // add "Handling Arguments" chapter
  def getLink(id: Int): Future[Option[Link]] = db.run(
     Links.filter(_.id === id).result.headOption
   )

   def getLinks(ids: Seq[Int]) = db.run(
     Links.filter(_.id inSet ids).result
   )

   def getUsers(ids: Seq[Int]): Future[Seq[User]] = {
     db.run(
       Users.filter(_.id inSet ids).result
     )
   }
}
