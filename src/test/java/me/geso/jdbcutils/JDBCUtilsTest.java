package me.geso.jdbcutils;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;

import lombok.SneakyThrows;

import org.junit.Before;
import org.junit.Test;

public class JDBCUtilsTest {
	private Connection connection;

	@Before
	@SneakyThrows
	public void before() {
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

	@Test
	public void test() throws RichSQLException {
		assertEquals(0, JDBCUtils
				.executeUpdate(
						connection,
						"DROP TABLE IF EXISTS x"));
		assertEquals(
				0,
				JDBCUtils
						.executeUpdate(
								connection,
								"CREATE TABLE x (id integer unsigned auto_increment primary key, name varchar(255) not null)"));
		assertEquals(
				1,
				JDBCUtils
						.executeUpdate(
								connection,
								"INSERT INTO x (name) VALUES (?)",
								Arrays.asList("hoge")));
		assertEquals("hoge", JDBCUtils.executeQuery(
				connection,
				"SELECT * FROM x WHERE name=?",
				Arrays.asList("hoge"),
				(rs) -> {
					assertTrue(rs.next());
					return rs.getString("name");
				}));
	}
}
