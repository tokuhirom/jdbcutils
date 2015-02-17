package me.geso.jdbcutils;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rich SQL exception.
 *
 */
public class RichSQLException extends Exception {
	private static final Logger LOGGER = LoggerFactory
		.getLogger(RichSQLException.class);

	private final String sql;
	private final List<Object> params;

	public RichSQLException(SQLException ex, String sql, List<Object> params) {
		super(ex);
		LOGGER.error("SQLException: {} {} {}", ex.getMessage(), sql,
			params.toString());
		this.sql = sql;
		this.params = params;
	}

	public String getSql() {
		return sql;
	}

	public List<Object> getParams() {
		return params;
	}

	private static final long serialVersionUID = 1L;

}
