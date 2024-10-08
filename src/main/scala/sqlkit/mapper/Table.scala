package sqlkit.mapper

import sqlkit.mapper.StringHelper._

case class Table(
  sql_name: String,
  columns: List[Column]
){

  def name = sql_name.toCamelCase()
  def modelName = sql_name.toPascalCase()

  def modelExtend = {
    if (autoIncrement){
      s"""SqlModelAutoInc[${modelName}] {
         |  def withId(id: Long) = this.copy(id = id)
         |}""".stripMargin
    } else {
      s"""SqlModel[${modelName}]"""
    }
  }
  def autoIncrement = columns.exists(_.autoIncrement)
}

