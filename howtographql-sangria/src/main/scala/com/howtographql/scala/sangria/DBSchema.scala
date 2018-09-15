package com.howtographql.scala.sangria

import slick.jdbc.H2Profile.api._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps

// add "The first query" chapter
import com.howtographql.scala.sangria.models._

// add "Custom Scalars" chapter
import java.sql.Timestamp
import akka.http.scaladsl.model.DateTime


object DBSchema {

  /**
    * Load schema and populate sample data withing this Sequence od DBActions
    */

  implicit val dateTimeColumnType = MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.clicks),
      ts => DateTime(ts.getTime)
  )

  // add "The first query" chapter
  class LinksTable(tag: Tag) extends Table[Link](tag, "LINKS"){

      def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
      def url = column[String]("URL")
      def description = column[String]("DESCRIPTION")

      // add and modify "Custom Scalars" chapter
      def createdAt = column[DateTime]("CREATED_AT")
      def * = (id, url, description, createdAt).mapTo[Link]
      // def * = (id, url, description).mapTo[Link]

  }
  val Links = TableQuery[LinksTable]

  // add "Interfaces" chapter
  class UsersTable(tag: Tag) extends Table[User](tag, "USERS"){
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def email = column[String]("EMAIL")
    def password = column[String]("PASSWORD")
    def createdAt = column[DateTime]("CREATED_AT")

    def * = (id, name, email, password, createdAt).mapTo[User]
  }

  val Users = TableQuery[UsersTable]

  val databaseSetup = DBIO.seq(
      Links.schema.create,

      // change "Deferred Resolvers" chapter
      Links forceInsertAll Seq(
          Link(1, "http://howtographql.com", "Awesome community driven GraphQL tutorial", DateTime(2017,9,12)),
          Link(2, "http://graphql.org", "Official GraphQL web page",DateTime(2017,10,1)),
          Link(3, "https://facebook.github.io/graphql/", "GraphQL specification",DateTime(2017,10,2))
      ),

      Users.schema.create,
      Users forceInsertAll Seq(
        User(1, "mario", "mario@example.com", "s3cr3t"),
        User(2, "Fred", "fred@flinstones.com", "wilmalove")
      )
  )


  def createDatabase: DAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new DAO(db)

  }

}
