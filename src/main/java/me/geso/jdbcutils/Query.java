package me.geso.jdbcutils;

import lombok.ToString;

/**
 * This class represents SQL query and parameters.
 */
@ToString
public class Query {
	private final String sql;
	private final Object[] params;

	/**
	 * Create new instance.
	 * 
	 * @param sql
	 * @param params
	 */
	public Query(final String sql, final Object... params) {
		this.sql = sql;
		this.params = params;
	}

	/**
	 * Get SQL string.
	 * 
	 * @return
	 */
	public String getSQL() {
		return sql;
	}

	/**
	 * Get parameters.
	 * @return
	 */
	public Object[] getParameers() {
		return params;
	}
}
