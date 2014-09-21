package me.geso.jdbcutils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builder class for {@code Query} class.
 *
 */
public class QueryBuilder {
	private final List<Object> parameters;
	private final StringBuilder query;
	private final String identifierQuoteString;

	public QueryBuilder(final String identifierQuoteString) {
		this.parameters = new ArrayList<>();
		this.query = new StringBuilder();
		this.identifierQuoteString = identifierQuoteString;
	}

	public QueryBuilder(final Connection connection) {
		try {
			this.parameters = new ArrayList<>();
			this.query = new StringBuilder();
			this.identifierQuoteString = connection.getMetaData()
					.getIdentifierQuoteString();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public QueryBuilder appendQuery(final String s) {
		this.query.append(s);
		return this;
	}

	public QueryBuilder appendQuery(final long l) {
		this.query.append(""+l);
		return this;
	}

	public QueryBuilder appendIdentifier(final String identifier) {
		this.query.append(JDBCUtils.quoteIdentifier(identifier,
				identifierQuoteString));
		return this;
	}

	public QueryBuilder append(final Query q) {
		this.query.append(q.getSQL());
		this.parameters.addAll(q.getParameters());
		return this;
	}

	public QueryBuilder addParameter(final Object o) {
		this.parameters.add(o);
		return this;
	}

	public <T> QueryBuilder addParameters(final Collection<T> o) {
		this.parameters.addAll(o);
		return this;
	}

	public Query build() {
		return new Query(new String(this.query), this.parameters);
	}

	public List<Object> getParameters() {
		return parameters;
	}

	public StringBuilder getQuery() {
		return query;
	}

	public QueryBuilder appendQueryAndParam(String s, Object o) {
		this.appendQuery(s);
		this.addParameter(o);
		return this;
	}
}
