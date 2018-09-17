package com.howtographql.scala.sangria
import DBSchema._
import slick.jdbc.H2Profile.api._
// add "Handling Arguments" chapter
import com.howtographql.scala.sangria.models.{Link, User, Vote}
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
}
