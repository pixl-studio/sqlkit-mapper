package sqlkit.mapper

import java.sql.JDBCType

object TypeName {
  val Any = "Any"
  val AnyArray = "Array[Any]"
  val ByteArray = "Array[Byte]"
  val Json = "Json"
  val Long = "Long"
  val Boolean = "Boolean"
  val DateTime = "DateTime"
  val LocalDate = "LocalDate"
  val LocalTime = "LocalTime"
  val TimeStamp = "LocalDateTime"
  val String = "String"
  val Byte = "Byte"
  val Int = "Int"
  val Short = "Short"
  val Float = "Float"
  val Double = "Double"
  val Blob = "Blob"
  val Clob = "Clob"
  val Ref = "Ref"
  val Struct = "Struct"
  val BigDecimal = "BigDecimal" // scala.math.BigDecimal

  val typeImports = Map(
    "LocalDate" -> "java.time.LocalDate",
    "LocalTime" -> "java.time.LocalTime",
    "LocalDateTime" -> "java.time.LocalDateTime"
  )


  def valueOf(jdbcType: JDBCType) = {
    jdbcType match {
      case JDBCType.ARRAY         => TypeName.AnyArray
      case JDBCType.BIGINT        => TypeName.Long
      case JDBCType.BINARY        => TypeName.ByteArray
      case JDBCType.BIT           => TypeName.Boolean
      case JDBCType.BLOB          => TypeName.Blob
      case JDBCType.BOOLEAN       => TypeName.Boolean
      case JDBCType.CHAR          => TypeName.String
      case JDBCType.CLOB          => TypeName.Clob
      case JDBCType.DATALINK      => TypeName.Any
      case JDBCType.DATE          => TypeName.LocalDate
      case JDBCType.DECIMAL       => TypeName.BigDecimal
      case JDBCType.DISTINCT      => TypeName.Any
      case JDBCType.DOUBLE        => TypeName.Double
      case JDBCType.FLOAT         => TypeName.Float
      case JDBCType.INTEGER       => TypeName.Int
      case JDBCType.JAVA_OBJECT   => TypeName.Json
      case JDBCType.LONGVARBINARY => TypeName.ByteArray
      case JDBCType.LONGVARCHAR   => TypeName.String
      case JDBCType.NULL          => TypeName.Any
      case JDBCType.NUMERIC       => TypeName.BigDecimal
      case JDBCType.OTHER         => TypeName.Any
      case JDBCType.REAL          => TypeName.Float
      case JDBCType.REF           => TypeName.Ref
      case JDBCType.SMALLINT      => TypeName.Short
      case JDBCType.STRUCT        => TypeName.Struct
      case JDBCType.TIME          => TypeName.LocalTime
      case JDBCType.TIMESTAMP     => TypeName.TimeStamp
      case JDBCType.TINYINT       => TypeName.Byte
      case JDBCType.VARBINARY     => TypeName.ByteArray
      case JDBCType.VARCHAR       => TypeName.String
      case JDBCType.NVARCHAR      => TypeName.String
      case JDBCType.NCHAR         => TypeName.String
      case JDBCType.LONGNVARCHAR  => TypeName.String
      case _                      => TypeName.Any
    }
  }
}
