package sqlkit.mapper


import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import sqlkit.DB

import java.io.{File, FileOutputStream, OutputStreamWriter}
import java.nio.file.Files
import java.sql.DatabaseMetaData
import scala.util.control.Exception.{ignoring, ultimately}

case class SqlMapper(table:Table) {

  def genCaseClass() = {

    s"""
       |case class ${table.modelName} (
       |  ${genFields}
       |) extends ${table.modelExtend}
       |""".stripMargin
  }

  def genFields() = {

    val fields = table.columns.map { c =>
      s"""${c.name}: ${c.scalaType}"""
    }

    s"""${SqlMapper.FIELDS_START}
       |  ${fields.mkString(",\n  ")}
       |  ${SqlMapper.FIELDS_END}""".stripMargin
  }

  def genCompanion() = {
    s"""
       |object ${table.modelName} extends ${if (table.autoIncrement) "SqlTableAutoInc" else "SqlTable"}[${table.modelName}] {
       |
       |  def table(alias: String) = TableDef(alias)
       |
       |  ${genTableDef()}
       |}
       |""".stripMargin
  }

  def genTableDef() = {

    val fields = table.columns.map { c =>
      s"""val ${c.name} = column[${c.scalaType}]("${c.sql_name}"${if(c.autoIncrement) ", primaryKey = true" else ""})"""
    }

    val fieldsFromSql = table.columns.map { c =>
      s"""${c.name} = row.get(${c.name})"""
    }

    val fieldsToSql = table.columns.map { c =>
      s"""${c.name} -> ${table.name}.${c.name}"""
    }

    s"""${SqlMapper.TableDef_START}
       |  case class TableDef(alias: String) extends SqlTableDef[${table.modelName}] {
       |
       |    val table = "${table.sql_name}"
       |
       |    ${fields.mkString("\n    ")}
       |
       |    def fromSql(row: SqlRow) = ${table.modelName}(
       |      ${fieldsFromSql.mkString(", ")}
       |    )
       |
       |    def toSql(${table.name}: ${table.modelName}) = List(
       |      ${fieldsToSql.mkString(", ")}
       |    )
       |  }
       |  ${SqlMapper.TableDef_END}""".stripMargin

  }



  def genFull(packageName:String="") = {

    val imports = table.columns.flatMap { c =>
      TypeName.typeImports.get(c.rawType())
    }.distinct

    s"""package ${packageName}
       |
       |import sqlkit._
       |${imports.map(i => s"import ${i}").mkString("\n")}
       |${genCaseClass()}
       |${genCompanion()}
       |""".stripMargin
  }
}

object SqlMapper {

  val FIELDS_START = "//<SQLKIT_Fields"
  val FIELDS_END = "//>SQLKIT_Fields"

  val TableDef_START = "//<SQLKIT_TableDef"
  val TableDef_END = "//>SQLKIT_TableDef"


  def getTable(m: DatabaseMetaData, table:String): Option[Table] = {
    getTables(m).find(_.sql_name == table)
  }

  def getTables(m: DatabaseMetaData): List[Table] = {
    val rsTables = new DBResultSet(m.getTables(null, null, null, Array("TABLE")))
    rsTables.map { rs =>
      val name = rs.getString("TABLE_NAME")
      Table(
        sql_name = name,
        columns = getTableColumns(m, name)
      )
    }.toList
  }

  def getTableColumns(m: DatabaseMetaData, table: String) = {

    val rsCols = new DBResultSet(m.getColumns(null, null, table, null))
    rsCols.map { rs =>
      Column(
        sql_name = rs.getString("COLUMN_NAME"),
        sql_dataType = rs.getInt("DATA_TYPE"),
        sql_dataTypeName = rs.getString("TYPE_NAME"),
        sql_size = rs.getString("COLUMN_SIZE"),
        sql_nullable = rs.getString("IS_NULLABLE") ,
        sql_autoIncrement = rs.getString("IS_AUTOINCREMENT")
      )
    }.toList

  }

  def genTable(JDBCSettings: JDBCSettings, table:String): Option[Table] = {

    init(JDBCSettings)

    DB.withSession(){ implicit session =>
      session.withConnection { connection =>
        getTable(connection.getMetaData, table)
      }
    }
  }

  def writeTable(JDBCSettings: JDBCSettings, table:String) = {

    init(JDBCSettings)

    DB.withSession(){ implicit session =>
      session.withConnection { connection =>
        getTable(connection.getMetaData, table).map { t =>
          writeModel(JDBCSettings, t)
        }
      }
    }

    ()
  }

  def updateTable(JDBCSettings: JDBCSettings, table:String) = {

    init(JDBCSettings)

    DB.withSession(){ implicit session =>
      session.withConnection { connection =>
        getTable(connection.getMetaData, table).map { t =>
          updateModel(JDBCSettings, t)
        }
      }
    }

    ()
  }

  def updateModel(JDBCSettings: JDBCSettings, table:Table) = {

    val outFile = new File(
      JDBCSettings.src.getPath + "/" + JDBCSettings.packageName.replace(
        ".",
        "/"
      ) + "/" + table.modelName + ".scala"
    )

    if (!outFile.exists()){
      writeModel(JDBCSettings, table)
    } else {

      val content = Files.readString(outFile.toPath)

      val mapper = SqlMapper(table)

      val withNewFields = s"(?s)${FIELDS_START}.*?${FIELDS_END}".r.replaceFirstIn(content, mapper.genFields())
      val withNewTableDef = s"(?s)${TableDef_START}.*?${TableDef_END}".r.replaceFirstIn(withNewFields, mapper.genTableDef())


      using(new FileOutputStream(outFile)) { fos =>
        using(new OutputStreamWriter(fos)) { writer =>

          writer.write(withNewTableDef)

          println("\"" + JDBCSettings.packageName + "." + table.modelName + "\"" + " updated.")
        }
      }
    }
  }

  /**
   * Write the source code to outputFile.
   * It overwrites a file if it already exists.
   */
  def writeModel(JDBCSettings: JDBCSettings, table:Table): Unit = {

    val outFile = new File(
      JDBCSettings.src.getPath + "/" + JDBCSettings.packageName.replace(
        ".",
        "/"
      ) + "/" + table.modelName + ".scala"
    )

    mkdirRecursively(outFile.getParentFile)

    using(new FileOutputStream(outFile)) { fos =>
      using(new OutputStreamWriter(fos)) { writer =>

        val code = SqlMapper(table).genFull(JDBCSettings.packageName)

        writer.write(code)

        println("\"" + JDBCSettings.packageName + "." + table.modelName + "\"" + " created.")
      }
    }
  }

  def using[R <: {def close(): Unit}, A](resource: R)(f: R => A): A =
    ultimately {
      ignoring(classOf[Throwable]) apply resource.close()
    } apply f(resource)

  /**
   * Create directory to put the source code file if it does not exist yet.
   */
  def mkdirRecursively(file: File): Unit = {
    val parent = file.getAbsoluteFile.getParentFile
    if (!parent.exists) mkdirRecursively(parent)
    if (!file.exists) file.mkdir()
  }

  def init(JDBCSettings: JDBCSettings) = {
    val poolConfig = new HikariConfig
    poolConfig.setDriverClassName(JDBCSettings.driver)
    poolConfig.setJdbcUrl(JDBCSettings.url)
    poolConfig.setUsername(JDBCSettings.username)
    poolConfig.setPassword(JDBCSettings.password)
    poolConfig.addDataSourceProperty("cachePrepStmts", "true")
    poolConfig.addDataSourceProperty("prepStmtCacheSize", "250")
    poolConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    poolConfig.setMaximumPoolSize(5)

    DB.add(new HikariDataSource(poolConfig))
  }
}