import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import datomic.Connection
import datomic.Database
import datomic.Peer
import datomic.Util

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

import java.io.Reader
import java.io.FileReader

import scala.concurrent._
import scala.concurrent.util._
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit._

import reactivedatomic._

import Datomic._
import DatomicData._
import EntityImplicits._

@RunWith(classOf[JUnitRunner])
class DatomicEntitySpec extends Specification {
  sequential

  val uri = "datomic:mem://datomicschemaspec"

  import scala.concurrent.ExecutionContext.Implicits.global

  case class Person(name: String, age: Long, characters: Set[DRef])

  val person = new Namespace("person") {
    val character = Namespace("person.character")
  }      

  val violent = AddIdent(Keyword(person.character, "violent"))
  val weak = AddIdent(Keyword(person.character, "weak"))
  val clever = AddIdent(Keyword(person.character, "clever"))
  val dumb = AddIdent(Keyword(person.character, "dumb"))
  val stupid = AddIdent(Keyword(person.character, "stupid"))

  object PersonSchema {
    val name = Attribute( person / "name", SchemaType.string, Cardinality.one).withDoc("Person's name")
    val age = Attribute( person / "age", SchemaType.long, Cardinality.one).withDoc("Person's name")
    val characters =  Attribute( person / "characters", SchemaType.ref, Cardinality.many).withDoc("Person's characterS")

    val schema = Seq(name, age, characters)
  }

  val personReader = (
    PersonSchema.name.read[String] and 
    PersonSchema.age.read[Long] and
    PersonSchema.characters.read[Set[DRef]]
  )(Person)

  "Datomic" should {
    "create entity" in {
      
      //DatomicBootstrap(uri)
      println("created DB with uri %s: %s".format(uri, createDatabase(uri)))
      implicit val conn = Datomic.connect(uri)

      Await.result(
        transact(PersonSchema.schema ++ Seq(violent, weak, dumb, clever, stupid)).flatMap{ tx =>
          println("TX:"+tx)
          transact(
            addToEntity(DId(Partition.USER))(
              person / "name" -> "toto",
              person / "age" -> 30,
              person / "characters" -> Set(violent, weak)
            ),
            addToEntity(DId(Partition.USER))(
              person / "name" -> "tutu",
              person / "age" -> 54
            ),
            addToEntity(DId(Partition.USER))(
              person / "name" -> "tata",
              person / "age" -> 23
            )
          ).map{ tx => 
            println("Provisioned data... TX:%s".format(tx))

            query(typedQuery[Args0, Args1]("""
              [ :find ?e 
                :where [?e :person/name "toto"]
              ]
            """)).head match {
              case e: DLong =>
                database.entity(e).map { entity =>
                  println(
                    "dentity age:" + entity.getAs[DLong](person / "age") + 
                    " name:" + entity(person / "name") +
                    " map:" + entity.toMap
                  )
                  fromEntity(entity)(personReader).map {
                    case Person(name, age, characters) => 
                      println(s"Found person with name $name and age $age and characters $characters")
                      success
                  }.get
                }.getOrElse(failure("could't find entity"))
              case _ => failure("error")
            }
          }
        },
        Duration("2 seconds")
      )

    }

    "get entity fields from attribute" in {
      import scala.util.{Try, Success, Failure}
      import DatomicDataImplicits._
      import EntityImplicits._

      implicit val conn = Datomic.connect(uri)

      Datomic.query(typedQuery[Args0, Args1]("""
        [ :find ?e 
          :where [?e :person/name "toto"]
        ]
      """)).head match {
        case e: DLong =>
          database.entity(e).map { entity =>
            val nameValue = entity.as(PersonSchema.name)
            nameValue must beEqualTo("toto")

            
            val nameValue2 = entity.as[String](person / "name")
            nameValue2 must beEqualTo("toto")
            
            val nameValue3 = entity.as[DString](person / "name")
            nameValue3.underlying must beEqualTo("toto")

            val ageValue = entity.as(PersonSchema.age)
            ageValue must beEqualTo(30)

            val ageValue2 = entity.getAs(PersonSchema.age)
            ageValue2 must beEqualTo(Some(30))

            val ageValue3 = entity.tryGetAs(PersonSchema.age)
            ageValue3 must beEqualTo(Success(30))

            val ageValue4 = entity.as[Long](person / "age")
            ageValue4 must beEqualTo(30)

            val characters = entity.as(PersonSchema.characters)
            println("characters:"+characters)
          }.getOrElse(failure("could't find entity"))
        case _ => failure("error")
      }
    }
  }
}