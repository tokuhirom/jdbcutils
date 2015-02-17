package me.geso.jdbcutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import lombok.Data;

public class JDBCUtilsTest {
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
			2,
			JDBCUtils
				.executeUpdate(
					connection,
					"INSERT INTO x (name) VALUES (?),(?)",
					Arrays.asList("hoge", "fuga")));
		assertEquals("hoge", JDBCUtils.executeQuery(
			connection,
			"SELECT * FROM x WHERE name=?",
			Arrays.asList("hoge"),
			(rs) -> {
				assertTrue(rs.next());
				return rs.getString("name");
			}));
		JDBCUtils.executeQuery(
			connection,
			"SELECT GET_LOCK('hoge', 100)",
			Arrays.asList());
		assertEquals(
			Arrays.asList(
				new MapBuilder<String, Object>()
					.put("id", 2L)
					.put("name", "fuga")
					.build(),
				new MapBuilder<String, Object>()
					.put("id", 1L)
					.put("name", "hoge")
					.build()
				),
			JDBCUtils.executeQueryMapList(connection,
				"SELECT * FROM x ORDER BY id DESC",
				Collections.emptyList()));
		// Support `AS` for `executeQueryMapList
		assertEquals(
			Arrays.asList(
				new MapBuilder<String, Object>()
					.put("iii", 2L)
					.build(),
				new MapBuilder<String, Object>()
					.put("iii", 1L)
					.build()
				),
			JDBCUtils.executeQueryMapList(connection,
				"SELECT id AS iii FROM x ORDER BY id DESC",
				Collections.emptyList()));
	}

	public static class MapBuilder<K, V> {
		private Map<K, V> map;

		public MapBuilder() {
			this.map = new HashMap<>();
		}

		public MapBuilder<K, V> put(K key, V value) {
			map.put(key, value);
			return this;
		}

		public Map<K, V> build() {
			return Collections.unmodifiableMap(map);
		}
	}

	@Test
	public void testQuoteIdentifier() throws SQLException {
		{
			String got = JDBCUtils.quoteIdentifier(
				"hogefuga\"higehige\"hagahaga",
				"\"");
			assertEquals("\"hogefuga\"\"higehige\"\"hagahaga\"", got);
		}
		{
			String q = this.connection.getMetaData().getIdentifierQuoteString();
			assertEquals("`", q);
			String got = JDBCUtils.quoteIdentifier(
				"hogefuga`higehige`hagahaga",
				this.connection);
			assertEquals("`hogefuga``higehige``hagahaga`", got);
		}
	}

	@Test
	public void testBean() throws RichSQLException, InstantiationException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, IntrospectionException {
		assertEquals(0, JDBCUtils
			.executeUpdate(
				connection,
				"DROP TABLE IF EXISTS bean"));
		assertEquals(
			0,
			JDBCUtils
				.executeUpdate(
					connection,
					"CREATE TABLE bean (id integer unsigned, name varchar(255))"));
		assertEquals(
			2,
			JDBCUtils
				.executeUpdate(
					connection,
					"INSERT INTO bean (id,name) VALUES (?,?), (?,?)",
					Arrays.asList(1, "hoge", 2, "fuga")));
		List<Bean> beans = JDBCUtils.executeQueryForBean(connection,
			"SELECT * FROM bean ORDER BY id", Collections.emptyList(),
			Bean.class);
		assertEquals(2, beans.size());
		assertEquals(1, beans.get(0).getId());
		assertEquals("hoge", beans.get(0).getName());
		assertEquals(2, beans.get(1).getId());
		assertEquals("fuga", beans.get(1).getName());
	}

	@Data
	public static class Bean {
		private long id;
		private String name;
	}
}
