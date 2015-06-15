package me.geso.jdbcutils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility functions for JDBC.
 *
 */
public class JDBCUtils {

	/**
	 * Execute query, and return stream.
	 * <B>You must call .close() after using.</B> I recommend to use try-with-resources.
	 *
	 * @param connection JDBC connection
	 * @param query SQL query
	 * @param callback callback function. It will call every row.
	 * @return Stream
	 * @throws RichSQLException
	 */
	public static <R> Stream<R> executeQueryStream(final Connection connection,
			final Query query,
			final ResultSetCallback<R> callback)
			throws RichSQLException {
		return JDBCUtils.executeQueryStream(connection,
			query.getSQL(), query.getParameters(),
			callback);
	}

	/**
	 * Execute query, and return stream.
	 * <B>You must call .close() after using.</B> I recommend to use try-with-resources.
	 *
	 * @param connection JDBC connection
	 * @param sql SQL query
	 * @param params parameters
	 * @param callback callback function. It will call every row.
	 * @return Stream
	 * @throws RichSQLException
	 */
	public static <R> Stream<R> executeQueryStream(final Connection connection,
			final String sql,
			final List<Object> params,
			final ResultSetCallback<R> callback)
			throws RichSQLException {
		try {
			final PreparedStatement ps = connection.prepareStatement(sql);
			JDBCUtils.fillPreparedStatementParams(ps, params);
			final ResultSet rs = ps.executeQuery();
			final ResultSetIterator<R> iterator = new ResultSetIterator<>(rs, sql, params, callback);
			Spliterator<R> spliterator = Spliterators.spliteratorUnknownSize(
				iterator, Spliterator.NONNULL | Spliterator.ORDERED | Spliterator.SIZED);
			final Stream<R> stream = StreamSupport.stream(spliterator, false);
			stream.onClose(() -> {
				try {
					ps.close();
				} catch (SQLException e) {
					throw new UncheckedRichSQLException(e);
				}
				try {
					rs.close();
				} catch (SQLException e) {
					throw new UncheckedRichSQLException(e);
				}
			});
			return stream;
		} catch (final SQLException ex) {
			throw new RichSQLException(ex, sql, params);
		}
	}

	/**
	 * Execute query with callback.
	 *
	 * @param connection
	 * @param query
	 * @param callback
	 * @return Generated value from the callback
	 * @throws RichSQLException
	 */
	public static <R> R executeQuery(final Connection connection,
			final Query query,
			final ResultSetCallback<R> callback)
			throws RichSQLException {
		return JDBCUtils.executeQuery(connection,
			query.getSQL(), query.getParameters(),
			callback);
	}

	/**
	 * Execute query with callback.
	 *
	 * @param connection
	 * @param sql
	 * @param params
	 * @param callback
	 * @return Generated value from the callback
	 * @throws RichSQLException
	 */
	public static <R> R executeQuery(final Connection connection,
			final String sql,
			final List<Object> params,
			final ResultSetCallback<R> callback)
			throws RichSQLException {
		try (final PreparedStatement ps = connection.prepareStatement(sql)) {
			JDBCUtils.fillPreparedStatementParams(ps, params);
			try (final ResultSet rs = ps.executeQuery()) {
				return callback.call(rs);
			}
		} catch (final SQLException ex) {
			throw new RichSQLException(ex, sql, params);
		}
	}

	/**
	 * Execute query without callback.
	 * This method is useful when calling the SELECT query has side effects, e.g. `SELECT GET_LOCK('hoge', 3)`.
	 *
	 * @param connection
	 * @param sql
	 * @param params
	 * @throws RichSQLException
	 */
	public static void executeQuery(final Connection connection,
			final String sql,
			final List<Object> params)
			throws RichSQLException {
		try (final PreparedStatement ps = connection.prepareStatement(sql)) {
			JDBCUtils.fillPreparedStatementParams(ps, params);
			try (final ResultSet rs = ps.executeQuery()) {
			}
		} catch (final SQLException ex) {
			throw new RichSQLException(ex, sql, params);
		}
	}

	/**
	 * Execute query without callback.
	 * This method returns results as {@code List<Map<String, Object>>}.
	 *
	 * @param connection
	 * @param sql
	 * @param params
	 * @return Selected rows in list of maps.
	 * @throws RichSQLException
	 */
	public static List<Map<String, Object>> executeQueryMapList(
			final Connection connection,
			final String sql,
			final List<Object> params)
			throws RichSQLException {
		try (final PreparedStatement ps = connection.prepareStatement(sql)) {
			JDBCUtils.fillPreparedStatementParams(ps, params);
			try (final ResultSet rs = ps.executeQuery()) {
				ResultSetMetaData metaData = rs.getMetaData();
				int columnCount = metaData.getColumnCount();
				List<Map<String, Object>> mapList = new ArrayList<>();
				while (rs.next()) {
					Map<String, Object> map = new HashMap<>();
					for (int i = 1; i <= columnCount; i++) {
						String name = metaData.getColumnLabel(i);
						map.put(name, rs.getObject(i));
					}
					mapList.add(map);
				}
				return mapList;
			}
		} catch (final SQLException ex) {
			throw new RichSQLException(ex, sql, params);
		}
	}

	/**
	 * [EXPERIMENTAL] Execute a query and map the result to the bean.
	 *
	 * @param connection
	 * @param sql
	 * @param params
	 * @return Selected rows in list of beans.
	 * @throws RichSQLException
	 * @throws IntrospectionException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 */
	public static <T> List<T> executeQueryForBean(
			final Connection connection,
			final String sql,
			final List<Object> params,
			final Class<T> valueClass)
			throws RichSQLException, IntrospectionException,
			InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		// Get bean information first.
		BeanInfo beanInfo = Introspector.getBeanInfo(valueClass, Object.class);
		PropertyDescriptor[] propertyDescriptors = beanInfo
			.getPropertyDescriptors();

		try (final PreparedStatement ps = connection.prepareStatement(sql)) {
			JDBCUtils.fillPreparedStatementParams(ps, params);
			try (final ResultSet rs = ps.executeQuery()) {
				List<T> valueList = new ArrayList<>();
				while (rs.next()) {
					T row = valueClass.newInstance();
					for (PropertyDescriptor prop : propertyDescriptors) {
						Method writeMethod = prop.getWriteMethod();
						if (writeMethod != null) {
							String name = prop.getName();
							Object value = rs.getObject(name);
							if (value != null) {
								writeMethod.invoke(row, value);
							}
						}
					}
					valueList.add(row);
				}
				return valueList;
			}
		} catch (final SQLException ex) {
			throw new RichSQLException(ex, sql, params);
		}
	}

	/**
	 * Execute query.
	 *
	 * @param connection
	 * @param query
	 * @return Affected rows.
	 * @throws RichSQLException
	 */
	public static int executeUpdate(final Connection connection,
			final Query query)
			throws RichSQLException {
		return JDBCUtils.executeUpdate(connection, query.getSQL(),
			query.getParameters());
	}

	/**
	 * Execute query.
	 *
	 * @param connection
	 * @param sql
	 * @param params
	 * @return Affected rows.
	 * @throws RichSQLException
	 */
	public static int executeUpdate(final Connection connection,
			final String sql,
			final List<Object> params)
			throws RichSQLException {
		try (final PreparedStatement ps = connection.prepareStatement(sql)) {
			JDBCUtils.fillPreparedStatementParams(ps, params);
			return ps.executeUpdate();
		} catch (final SQLException ex) {
			throw new RichSQLException(ex, sql, params);
		}
	}

	/**
	 * Shorthand method.
	 *
	 * @param connection
	 * @param sql
	 * @return
	 * @throws RichSQLException
	 */
	public static int executeUpdate(final Connection connection,
			final String sql)
			throws RichSQLException {
		return JDBCUtils
			.executeUpdate(connection, sql, Collections.emptyList());
	}

	/**
	 * Fill parameters for prepared statement.
	 *
	 * <pre>
	 * <code>JDBCUtils.fillPreparedStatementParams(preparedStatement, ImmutableList.of(1,2,3));</code>
	 * </pre>
	 *
	 * @param preparedStatement
	 * @param params
	 * @throws SQLException
	 */
	public static void fillPreparedStatementParams(
			final PreparedStatement preparedStatement,
			final List<Object> params) throws SQLException {
		for (int i = 0; i < params.size(); ++i) {
			preparedStatement.setObject(i + 1, params.get(i));
		}
	}

	/**
	 * Quote SQL identifier. You should get identifierQuoteString from
	 * DatabaseMetadata.
	 *
	 * @param identifier
	 * @param identifierQuoteString
	 * @return Escaped identifier.
	 */
	public static String quoteIdentifier(final String identifier,
			final String identifierQuoteString) {
		return identifierQuoteString
			+ identifier.replace(identifierQuoteString,
				identifierQuoteString + identifierQuoteString)
			+ identifierQuoteString;
	}

	/**
	 * Quote SQL identifier.
	 *
	 * @param identifier
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	public static String quoteIdentifier(final String identifier,
			final Connection connection) throws SQLException {
		if (connection == null) {
			throw new NullPointerException();
		}
		String identifierQuoteString = connection.getMetaData()
			.getIdentifierQuoteString();
		return quoteIdentifier(identifier, identifierQuoteString);
	}

}
