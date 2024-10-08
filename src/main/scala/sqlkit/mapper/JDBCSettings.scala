package sqlkit.mapper

import java.io.File

case class JDBCSettings(
  driver:String,
  url:String,
  username:String,
  password:String,
  schema:String,
  src:File,
  packageName:String
)
