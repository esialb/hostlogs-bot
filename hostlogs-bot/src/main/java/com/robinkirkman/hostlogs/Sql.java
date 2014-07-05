package com.robinkirkman.hostlogs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class Sql {
	private static SqlSessionFactory sql;
	
	private static Properties overrides = new Properties();
	
	public static synchronized SqlSessionFactory get() {
		if(sql != null)
			return sql;
		try {
			Properties p = new Properties();
			p.load(Sql.class.getResourceAsStream("mybatis.properties"));
			p.putAll(overrides);
			return sql = new SqlSessionFactoryBuilder().build(Sql.class.getResourceAsStream("mybatis.xml"), p);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Properties getOverrides() {
		return overrides;
	}
}
