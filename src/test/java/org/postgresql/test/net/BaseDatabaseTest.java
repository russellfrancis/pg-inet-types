package org.postgresql.test.net;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.ClassRule;
import org.postgresql.PGConnection;
import org.postgresql.net.PGcidr;
import org.postgresql.net.PGinet;
import org.postgresql.net.PGmacaddr;
import org.testcontainers.containers.PostgreSQLContainer;

public class BaseDatabaseTest {
  @ClassRule
  public static PostgreSQLContainer dbServer = new PostgreSQLContainer();

  Connection openDB() throws SQLException {
    String jdbcUrl = dbServer.getJdbcUrl();
    String username = dbServer.getUsername();
    String password = dbServer.getPassword();
    Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
    ((PGConnection)connection).addDataType("cidr", PGcidr.class);
    ((PGConnection)connection).addDataType("inet", PGinet.class);
    ((PGConnection)connection).addDataType("macaddr", PGmacaddr.class);
    return connection;
  }

  void createTable(Connection connection, String tableName, String... columnDefinition)
      throws SQLException {
    StringBuilder sql = new StringBuilder("CREATE TABLE ").append(tableName).append("\n")
        .append("(\n");
    for(int i = 0; i < columnDefinition.length; ++i) {
      sql.append(columnDefinition[i]);
      if (i+1 < columnDefinition.length) {
        sql.append(",\n");
      }
    }
    sql.append("\n);");
    try (Statement stmt = connection.createStatement()) {
      assertEquals(0, stmt.executeUpdate(sql.toString()));
    }
  }

  void dropTable(Connection connection, String tableName) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      assertEquals(0, stmt.executeUpdate("DROP TABLE " + tableName + ";"));
    }
  }
}
