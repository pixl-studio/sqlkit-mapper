import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.scalatest.funsuite.AnyFunSuite
import sqlkit.DB
import sqlkit.mapper.SqlMapper

import java.sql.DatabaseMetaData

class GenTableTest extends AnyFunSuite {

  def init() = {
    val poolConfig = new HikariConfig
    poolConfig.setDriverClassName("com.mysql.jdbc.Driver")
    poolConfig.setJdbcUrl("jdbc:mysql://localhost:3306/sqlkit?useSSL=false")
    poolConfig.setUsername("sqlkit")
    poolConfig.setPassword("sqlkit")
    poolConfig.addDataSourceProperty("cachePrepStmts", "true")
    poolConfig.addDataSourceProperty("prepStmtCacheSize", "250")
    poolConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    poolConfig.setMaximumPoolSize(5)

    DB.add(new HikariDataSource(poolConfig))
  }

  test("general") {
    init()

    DB.withSession() { dbSession =>

      dbSession.withConnection { c =>

        val m: DatabaseMetaData = c.getMetaData

        val tables = SqlMapper.getTables(m).map(SqlMapper(_))

        tables.map { t =>

          println(t.genFull("models.db"))
        }

      }
    }


    DB.close()
  }
}