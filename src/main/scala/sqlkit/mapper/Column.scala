package sqlkit.mapper

import java.sql.JDBCType
import StringHelper._

case class Column(
  sql_name: String,
  sql_dataType: Int,
  sql_dataTypeName: String,
  sql_size: String,
  sql_nullable: String,
  sql_autoIncrement: String
){

  def name = sql_name.toCamelCase()

  def scalaType = {
    if (nullable) s"Option[${rawType}]" else rawType
  }

  def rawType(): String = {
    TypeName.valueOf(JDBCType.valueOf(sql_dataType))
  }

  def nullable = sql_nullable == "YES"
  def autoIncrement = sql_autoIncrement == "YES"
}