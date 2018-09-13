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


object GraphQLSchema {

  implicit val LinkType = deriveObjectType[Unit, Link]()


  // add "Deferred Resolvers" chapter
  implicit val linkHasId = HasId[Link, Int](_.id)
  val linksFetcher = Fetcher(
      (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids)
    )
  val Resolver = DeferredResolver.fetchers(linksFetcher)

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
      )
    )
  )

  val SchemaDefinition = Schema(QueryType)
}
