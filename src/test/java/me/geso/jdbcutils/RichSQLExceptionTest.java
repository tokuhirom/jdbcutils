package me.geso.jdbcutils;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RichSQLExceptionTest {
	private Connection connection;

	@Before
	public void before() throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver").newInstance();

		String dburl = System.getProperty("test.dburl");
		String dbuser = System.getProperty("test.dbuser");
		String dbpassword = System.getProperty("test.dbpassword");
		if (dburl == null) {
			dburl = "jdbc:mysql://localhost/test";
			dbuser = "root";
			dbpassword = "";
		}

		connection = DriverManager.getConnection(dburl, dbuser, dbpassword);
	}

	@After
	public void after() throws SQLException {
		if (connection != null) {
			connection.close();
		}
	}

	@Test
	public void test() {
		String sql = "SELECT * FROM unknownTableName";
		try {
			try (final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
				preparedStatement.execute();
			}
		} catch (SQLException e) {
			final RichSQLException richSQLException = new RichSQLException(e, sql, Collections.emptyList());
			assertTrue("'" + richSQLException.getMessage() + "' contains query", richSQLException.getMessage().contains(sql));
		}
	}

}
