package me.geso.jdbcutils;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetCallback<T> {
	T call(ResultSet rs) throws SQLException;
}
