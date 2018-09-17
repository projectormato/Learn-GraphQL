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

// add "Relations" chapter
import sangria.execution.deferred.{Relation, RelationIds}

import sangria.marshalling.sprayJson._
import spray.json.DefaultJsonProtocol._

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

  val IdentifiableType = InterfaceType(
    "Identifiable",
    fields[Unit, Identifiable](
      Field("id", IntType, resolve = _.value.id)
    )
  )

  // implicit val LinkType = deriveObjectType[Unit, Link]()
  lazy val LinkType: ObjectType[Unit, Link] = deriveObjectType[Unit, Link](
      Interfaces(IdentifiableType),
      ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
      ReplaceField("postedBy",
        Field("postedBy", UserType, resolve = c => usersFetcher.defer(c.value.postedBy))
      ),
      AddFields(
        Field("votes", ListType(VoteType), resolve = c => votesFetcher.deferRelSeq(voteByLinkRel, c.value.id))
      )
    )

  // add "Relations" chapter
  val linkByUserRel = Relation[Link, Int]("byUser", l => Seq(l.postedBy))
  val voteByUserRel = Relation[Vote, Int]("byUser", v => Seq(v.userId))
  val voteByLinkRel = Relation[Vote, Int]("byLink", v => Seq(v.linkId))

  // add "Deferred Resolvers" chapter
  // implicit val linkHasId = HasId[Link, Int](_.id)
  val linksFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids),
    (ctx: MyContext, ids: RelationIds[Link]) => ctx.dao.getLinksByUserIds(ids(linkByUserRel))
  )

  lazy val UserType: ObjectType[Unit, User] = deriveObjectType[Unit, User](
    Interfaces(IdentifiableType),
    AddFields(
      Field("links", ListType(LinkType),
        resolve = c =>  linksFetcher.deferRelSeq(linkByUserRel, c.value.id)),
      Field("votes", ListType(VoteType),
        resolve = c =>  votesFetcher.deferRelSeq(voteByUserRel, c.value.id))
    )
  ) //ObjectType for user
  // implicit val userHasId = HasId[User, Int](_.id) //HasId type class
  val usersFetcher = Fetcher(
      (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
    )// resolver

  lazy val VoteType: ObjectType[Unit, Vote] = deriveObjectType[Unit, Vote](
      Interfaces(IdentifiableType),
      ExcludeFields("userId"),
      AddFields(Field("user",  UserType, resolve = c => usersFetcher.defer(c.value.userId))),
      AddFields(Field("link",  LinkType, resolve = c => linksFetcher.defer(c.value.linkId)))
    )
  // implicit val voteHasId = HasId[Vote, Int](_.id)

  val votesFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getVotes(ids),
    (ctx: MyContext, ids: RelationIds[Vote]) => ctx.dao.getVotesByRelationIds(ids)
    // (ctx: MyContext, ids: RelationIds[Vote]) => ctx.dao.getVotesByUserIds(ids(voteByUserRel))
  )

  implicit val authProviderEmailFormat = jsonFormat2(AuthProviderEmail)
  implicit val authProviderSignupDataFormat = jsonFormat1(AuthProviderSignupData)

  implicit val AuthProviderEmailInputType: InputObjectType[AuthProviderEmail] = deriveInputObjectType[AuthProviderEmail](
     InputObjectTypeName("AUTH_PROVIDER_EMAIL")
  )

  lazy val AuthProviderSignupDataInputType: InputObjectType[AuthProviderSignupData] = deriveInputObjectType[AuthProviderSignupData]()

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

  val NameArg = Argument("name", StringType)
  val AuthProviderArg = Argument("authProvider", AuthProviderSignupDataInputType)
  val UrlArg = Argument("url", StringType)
  val DescArg = Argument("description", StringType)
  val PostedByArg = Argument("postedById", IntType)
  val LinkIdArg = Argument("linkId", IntType)
  val UserIdArg = Argument("userId", IntType)

  val Mutation = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      Field("createUser",
        UserType,
        arguments = NameArg :: AuthProviderArg :: Nil,
        resolve = c => c.ctx.dao.createUser(c.arg(NameArg), c.arg(AuthProviderArg))
      ),
      Field("createLink",
        LinkType,
        arguments = UrlArg :: DescArg :: PostedByArg :: Nil,
        resolve = c => c.ctx.dao.createLink(c.arg(UrlArg), c.arg(DescArg), c.arg(PostedByArg))
      ),
    Field("createVote",
      VoteType,
      arguments = LinkIdArg :: UserIdArg :: Nil,
      resolve = c => c.ctx.dao.createVote(c.arg(LinkIdArg), c.arg(UserIdArg))
      )
    )
  )

  // val SchemaDefinition = Schema(QueryType)
  val SchemaDefinition = Schema(QueryType, Some(Mutation))
}
