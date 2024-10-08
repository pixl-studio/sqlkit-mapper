package sqlkit.mapper

import java.sql.ResultSet

class DBResultSet(rs: ResultSet) extends Iterator[ResultSet] {
  override def hasNext: Boolean = {
    rs.next()
  }

  override def next(): ResultSet = {
    rs
  }
}
