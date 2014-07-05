package com.robinkirkman.hostlogs;

import org.apache.ibatis.session.SqlSession;
import org.junit.Test;

import com.robinkirkman.hostlogs.LineMapper;
import com.robinkirkman.hostlogs.Sql;

public class SqlTest {
	@Test
	public void testGet() {
		Sql.get();
	}
	
	@Test
	public void testSession() {
		Sql.get().openSession().close();
	}
	
	@Test
	public void testMapper() {
		SqlSession s = Sql.get().openSession();
		try {
			s.getMapper(LineMapper.class);
		} finally {
			s.close();
		}
	}
}
