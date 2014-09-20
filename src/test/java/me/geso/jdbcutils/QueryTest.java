package me.geso.jdbcutils;

import static org.junit.Assert.*;

import org.junit.Test;

public class QueryTest {

	@Test
	public void test() {
		Query query = new Query("SELECT * FROM member", 1,2,3);
		assertEquals("SELECT * FROM member", query.getSQL());
		assertEquals(3, query.getParameers().length);
		assertEquals(1, query.getParameers()[0]);
		assertEquals(2, query.getParameers()[1]);
		assertEquals(3, query.getParameers()[2]);
	}

}
