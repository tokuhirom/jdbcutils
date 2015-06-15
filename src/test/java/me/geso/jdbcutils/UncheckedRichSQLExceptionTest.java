package me.geso.jdbcutils;

import static junit.framework.TestCase.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UncheckedRichSQLExceptionTest {

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
	public void testMessage() {
		String sql = "SELECT * FROM unknownTableName";
		try {
			try (final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
				preparedStatement.execute();
			}
		} catch (SQLException e) {
			final UncheckedRichSQLException richSQLException = new UncheckedRichSQLException(e, sql, Collections.emptyList());
			assertTrue("'" + richSQLException.getMessage() + "' contains query", richSQLException.getMessage().contains(sql));
			assertTrue(richSQLException.getCause() instanceof RichSQLException);
		}
	}

	@Test(expected=UncheckedRichSQLException.class)
	public void testUnchecked() {
		String sql = "SELECT * FROM unknownTableName";
		try {
			try (final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
				preparedStatement.execute();
			}
		} catch (SQLException e) {
			final UncheckedRichSQLException richSQLException = new UncheckedRichSQLException(e, sql, Collections.emptyList());
			throw richSQLException;
		}
	}
}
