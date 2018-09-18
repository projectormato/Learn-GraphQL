package com.howtographql.scala.sangria
import DBSchema._
import slick.jdbc.H2Profile.api._
// add "Handling Arguments" chapter
import com.howtographql.scala.sangria.models.{Link, User, Vote, AuthProviderSignupData}
import scala.concurrent.Future
import sangria.execution.deferred.{RelationIds, SimpleRelation}

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

   def getVotes(ids: Seq[Int]): Future[Seq[Vote]] = {
     db.run(
       Votes.filter(_.id inSet ids).result
     )
   }

   // add "Relation" chapter
   def getLinksByUserIds(ids: Seq[Int]): Future[Seq[Link]] = {
     db.run {
       Links.filter(_.postedBy inSet ids).result
     }
   }

   def getVotesByUserIds(ids: Seq[Int]): Future[Seq[Vote]] = {
     db.run {
       Votes.filter(_.userId inSet ids).result
     }
   }

   def getVotesByRelationIds(rel: RelationIds[Vote]): Future[Seq[Vote]] =
     db.run(
       Votes.filter { vote =>
         rel.rawIds.collect({
           case (SimpleRelation("byUser"), ids: Seq[Int]) => vote.userId inSet ids
           case (SimpleRelation("byLink"), ids: Seq[Int]) => vote.linkId inSet ids
         }).foldLeft(true: Rep[Boolean])(_ || _)

       } result
     )

   def createUser(name: String, authProvider: AuthProviderSignupData): Future[User] = {
     val newUser = User(0, name, authProvider.email.email, authProvider.email.password )

     val insertAndReturnUserQuery = (Users returning Users.map(_.id)) into {
       (user, id) => user.copy(id = id)
     }

     db.run {
       insertAndReturnUserQuery += newUser
     }
   }

   def createLink(url: String, description: String, postedBy: Int): Future[Link] = {
     val newLink = Link(0, url, description, postedBy)

     val insertAndReturnLinkQuery = (Links returning Links.map(_.id)) into {
       (link, id) => link.copy(id = id)
     }
     db.run {
       insertAndReturnLinkQuery += newLink
     }
   }

   def createVote(linkId: Int, userId: Int): Future[Vote] = {
     val insertAndReturnVoteQuery = (Votes returning Votes.map(_.id)) into {
       (vote, id) => vote.copy(id = id)
     }
     db.run {
       insertAndReturnVoteQuery += Vote(0, userId, linkId)
     }
   }

   def authenticate(email: String, password: String): Future[Option[User]] = db.run {
       Users.filter(u => u.email === email && u.password === password).result.headOption
   }
}
