package com.howtographql.scala.sangria

import sangria.schema.{ListType, ObjectType}
import models._
import sangria.schema._
import sangria.macros.derive._

// add "Deferred Resolvers" chapter
// import sangria.execution.deferred.Fetcher
// import sangria.execution.deferred.DeferredResolver
// import sangria.execution.deferred.HasId
// 上のimportをまとめてimport
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}

// add "Custom Scalars" chapter
import sangria.ast.StringValue
import akka.http.scaladsl.model.DateTime


object GraphQLSchema {


  implicit val GraphQLDateTime = ScalarType[DateTime](//1
    "DateTime",//2
    coerceOutput = (dt, _) => dt.toString, //3
    coerceInput = { //4
      case StringValue(dt, _, _ ) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    },
    coerceUserInput = { //5
      case s: String => DateTime.fromIsoDateTimeString(s).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    }
  )

  // implicit val LinkType = deriveObjectType[Unit, Link]()
  val LinkType = deriveObjectType[Unit, Link](
      ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
    )

  // add "Deferred Resolvers" chapter
  implicit val linkHasId = HasId[Link, Int](_.id)
  val linksFetcher = Fetcher(
      (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids)
    )

  val UserType = deriveObjectType[Unit, User]() //ObjectType for user
  implicit val userHasId = HasId[User, Int](_.id) //HasId type class
  val usersFetcher = Fetcher(
      (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
    )// resolver

  implicit val VoteType = deriveObjectType[Unit, Vote]()
  implicit val voteHasId = HasId[Vote, Int](_.id)

  val votesFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getVotes(ids)
  )

  // val Resolver = DeferredResolver.fetchers(linksFetcher)
  val Resolver = DeferredResolver.fetchers(linksFetcher, usersFetcher, votesFetcher)

  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))

  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks),

      // modify "Deferred Resolvers" chapter.(need review)
      Field("link",
        OptionType(LinkType),
        arguments = Id :: Nil,
        resolve = c => linksFetcher.deferOpt(c.arg(Id))
      ),
      Field("links",
        ListType(LinkType),
        arguments = Ids :: Nil,
        resolve = c => linksFetcher.deferSeq(c.arg(Ids))
      ),
      Field("users",
              ListType(UserType),
              arguments = List(Ids),
              resolve = c => usersFetcher.deferSeq(c.arg(Ids))
      ),
      Field("votes",
              ListType(VoteType),
              arguments = List(Ids),
              resolve = c => votesFetcher.deferSeq(c.arg(Ids))
      )
    )
  )

  val SchemaDefinition = Schema(QueryType)
}
