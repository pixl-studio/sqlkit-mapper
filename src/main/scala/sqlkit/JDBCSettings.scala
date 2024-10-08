package sqlkit

case class JDBCSettings(
  driver:String,
  url:String,
  username:String,
  password:String,
  schema:String
)
