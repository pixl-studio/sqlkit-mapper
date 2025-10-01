package sqlkit.mapper

import sqlkit.mapper.StringHelper.*

import java.sql.JDBCType

case class Column(
  sql_name: String,
  sql_dataType: Int,
  sql_dataTypeName: String,
  sql_size: String,
  sql_nullable: String,
  sql_autoIncrement: String
) {

  val reservedWords = List(
    "abstract", "case", "catch", "class", "def", "do", "else",
    "enum", "export", "extends", "false", "final", "finally", "for",
    "given", "if", "implicit", "import", "lazy", "match", "new",
    "null", "object", "override", "package", "private", "protected", "return",
    "sealed", "super", "then", "throw", "trait", "true", "try",
    "type", "val", "var", "while", "with", "yield",
    "as", "derives", "end", "extension", "infix", "inline", "opaque",
    "open", "transparent", "using"
  )

  def name = {
    val columnName = sql_name.toCamelCase()
    if (reservedWords.contains(columnName)) s"""`$columnName`""" else columnName
  }

  def scalaType = {
    if (nullable) s"Option[${rawType}]" else rawType
  }

  def rawType(): String = {
    TypeName.valueOf(JDBCType.valueOf(sql_dataType))
  }

  def nullable = sql_nullable == "YES"

  def autoIncrement = sql_autoIncrement == "YES"
}