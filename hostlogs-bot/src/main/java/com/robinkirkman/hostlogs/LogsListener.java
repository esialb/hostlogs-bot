package com.robinkirkman.hostlogs;

import java.util.Date;

import org.apache.ibatis.session.SqlSession;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

public class LogsListener extends ListenerAdapter<PircBotX> {

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		Line l = new Line();
		l.setHost(event.getUser().getHostmask().toLowerCase());
		l.setLine(event.getMessage());
		l.setNick(event.getUser().getNick());
		l.setTo(event.getChannel().getName().toLowerCase());
		l.setTs(new Date());
		l.setUser(event.getUser().getLogin());
		
		SqlSession s = Sql.get().openSession();
		try {
			s.getMapper(LineMapper.class).insert(l);
			s.commit();
		} finally {
			s.close();
		}
	}

	@Override
	public void onAction(ActionEvent<PircBotX> event) throws Exception {
		Line l = new Line();
		l.setHost(event.getUser().getHostmask().toLowerCase());
		l.setLine(event.getMessage());
		l.setNick(event.getUser().getNick());
		l.setTo(event.getChannel().getName().toLowerCase());
		l.setTs(new Date());
		l.setUser(event.getUser().getLogin());
		
		SqlSession s = Sql.get().openSession();
		try {
			s.getMapper(LineMapper.class).insert(l);
			s.commit();
		} finally {
			s.close();
		}
	}
}
