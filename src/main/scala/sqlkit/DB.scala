package sqlkit

import com.zaxxer.hikari.HikariDataSource
import sqlkit.session.{IsolationLevel, SqlAuto, SqlSession}


object DB {

  val defaultSource = "default"

  private var dataSources = Map.empty[String, HikariDataSource]


  def dataSource(name: String=defaultSource): HikariDataSource = {
    dataSources.getOrElse(name, throw new Exception(s"dateSource not found: ${name}"))
  }

  def add(dataSource: HikariDataSource, name: String=defaultSource) = {
    dataSources = dataSources + (name -> dataSource)
  }

  def close(name:String=defaultSource) = {
    dataSource(name).close()
  }

  // ---

  def withSession[T](dataSource: String = defaultSource)(f: SqlSession => T): T = {
    f(SqlAuto(dataSource))
  }
}