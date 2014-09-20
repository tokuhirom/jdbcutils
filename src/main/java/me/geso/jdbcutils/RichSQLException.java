package me.geso.jdbcutils;

import java.sql.SQLException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rich SQL exception.
 *
 */
public class RichSQLException extends Exception {
	private static final Logger logger = LoggerFactory.getLogger(RichSQLException.class);

	private final String sql;
	private final Object[] params;

	public RichSQLException(SQLException ex, String sql, Object[] params) {
		super(ex);
		logger.error("SQLException: {} {} {}", ex.getMessage(), sql, Arrays.toString(params));
		this.sql = sql;
		this.params = params;
	}

	public String getSql() {
		return sql;
	}

	public Object[] getParams() {
		return params;
	}

	private static final long serialVersionUID = 1L;

}
