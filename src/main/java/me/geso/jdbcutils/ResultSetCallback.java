package me.geso.jdbcutils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Callback function, that receives ResultSet.
 *
 * @param <T>
 */
@FunctionalInterface
public interface ResultSetCallback<T> {
	T call(ResultSet rs) throws SQLException;
}
