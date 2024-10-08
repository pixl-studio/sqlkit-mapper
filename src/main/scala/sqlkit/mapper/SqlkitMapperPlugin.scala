package sqlkit.mapper

import sbt._
import sbt.Keys._
import sbt.complete.EditDistance
import scala.language.reflectiveCalls
import scala.util.control.Exception._
import java.io.FileNotFoundException
import java.util.Locale.{ENGLISH => en}
import java.util.Properties

object SqlkitMapperPlugin extends AutoPlugin {

  private[this] final val JDBC = "jdbc."
  private[this] final val JDBC_DRIVER = JDBC + "driver"
  private[this] final val JDBC_URL = JDBC + "url"
  private[this] final val JDBC_USER_NAME = JDBC + "username"
  private[this] final val JDBC_PASSWORD = JDBC + "password"
  private[this] final val JDBC_SCHEMA = JDBC + "schema"

  private[this] final val GENERATOR = "generator."
  private[this] final val SRC_DIR = GENERATOR + "src"
  private[this] final val PACKAGE_NAME = GENERATOR + "packageName"

  object autoImport {
    val sqlkitGenTable =
      inputKey[Unit]("Generates & write/update a model for a specified table")

    val sqlkitGenTableForce =
      inputKey[Unit]("Generates & write a model for a specified table")

    val sqlkitGenTablePrint =
      inputKey[Unit]("Generates & print a model for a specified table")

    val jdbcSettings = taskKey[JDBCSettings]("")
  }

  import autoImport._

  private[this] def getString(props: Properties, key: String): Option[String] =
    Option(props.get(key)).map { value =>
      val str = value.toString
      if (str.startsWith("\"") && str.endsWith("\"") && str.length >= 2) {
        str.substring(1, str.length - 1)
      } else str
    }

  private[this] def commaSeparated(
    props: Properties,
    key: String
  ): collection.Seq[String] =
    getString(props, key)
      .map(_.split(',').map(_.trim).filter(_.nonEmpty).toList)
      .getOrElse(Nil)

  private[this] val jdbcKeys =
    Set(JDBC_DRIVER, JDBC_URL, JDBC_USER_NAME, JDBC_PASSWORD, JDBC_SCHEMA)

  private[this] val generatorKeys = Set(
    SRC_DIR, PACKAGE_NAME
  )
  private[this] val allKeys = jdbcKeys ++ generatorKeys

  private[this] def printWarningIfTypo(props: Properties): Unit = {
    import scala.jdk.CollectionConverters._
    props.keySet().asScala.map(_.toString).filterNot(allKeys).foreach {
      typoKey =>
        val correctKeys = allKeys.toList
          .sortBy(key => EditDistance.levenshtein(typoKey, key))
          .take(3)
          .mkString(" or ")
        println(s"""Not a valid key "$typoKey". did you mean ${correctKeys}?""")
    }
  }

  private[this] def loadJDBCSettings(props: Properties, srcDir:File): JDBCSettings = {

    printWarningIfTypo(props)
    JDBCSettings(
      driver = getString(props, JDBC_DRIVER).getOrElse(
        throw new IllegalStateException(
          s"Add $JDBC_DRIVER to project/scalikejdbc-mapper-generator.properties"
        )
      ),
      url = getString(props, JDBC_URL).getOrElse(
        throw new IllegalStateException(
          s"Add $JDBC_URL to project/scalikejdbc-mapper-generator.properties"
        )
      ),
      username = getString(props, JDBC_USER_NAME).getOrElse(""),
      password = getString(props, JDBC_PASSWORD).getOrElse(""),
      schema = getString(props, JDBC_SCHEMA).orNull[String],
      src = getString(props, SRC_DIR).map(p => new File(p)).getOrElse(srcDir),
      packageName = getString(props, PACKAGE_NAME).getOrElse("")
    )
  }

  private[this] def loadPropertiesFromFile()
  : Either[FileNotFoundException, Properties] = {
    val props = new java.util.Properties
    /*
    try {
      using(
        new java.io.FileInputStream(
          "project/scalikejdbc-mapper-generator.properties"
        )
      ) { inputStream =>
        props.load(inputStream)
      }
    } catch {
      case e: FileNotFoundException =>
    }*/
    if (props.isEmpty) {
      try {
        using(new java.io.FileInputStream("project/sqlkit.properties")) {
          inputStream => props.load(inputStream)
        }
        Right(props)
      } catch {
        case e: FileNotFoundException =>
          Left(e)
      }
    } else {
      Right(props)
    }
  }

  private final case class GenTaskParameter(
    table: String,
    clazz: Option[String]
  )

  import complete.DefaultParsers._

  private def genTaskParser(
    keyName: String
  ): complete.Parser[GenTaskParameter] = (Space ~> token(
    StringBasic,
    "tableName"
  ) ~ (Space ~> token(StringBasic, "(class-name)")).?)
    .map(GenTaskParameter.tupled)
    .!!!("Usage: " + keyName + " [table-name (class-name)]")

  override val projectSettings: collection.Seq[Def.Setting[?]] =
    inConfig(Compile)(
      Seq
      (
        jdbcSettings := {
          val srcDir = (Compile / scalaSource).value
          loadPropertiesFromFile().fold(throw _, loadJDBCSettings(_, srcDir))
        },
        sqlkitGenTable := {

          val args = genTaskParser(sqlkitGenTable.key.label).parsed

          SqlMapper.updateTable(jdbcSettings.value, args.table)

        },
        sqlkitGenTableForce := {

          val args = genTaskParser(sqlkitGenTableForce.key.label).parsed

          SqlMapper.writeTable(jdbcSettings.value, args.table)

        },
        sqlkitGenTablePrint := {

          val args = genTaskParser(sqlkitGenTablePrint.key.label).parsed

          val settings = jdbcSettings.value
          val table = SqlMapper.genTable(settings, args.table)

          table.map { t =>
            println(SqlMapper(t).genFull(settings.packageName))
          }
        }
      )
    )

  def using[R <: {def close(): Unit}, A](resource: R)(f: R => A): A =
    ultimately {
      ignoring(classOf[Throwable]) apply resource.close()
    } apply f(resource)
}
