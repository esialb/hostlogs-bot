package com.robinkirkman.hostlogs.viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.ibatis.session.SqlSession;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.exception.IrcException;
import org.slf4j.impl.SimpleLogger;

import com.robinkirkman.hostlogs.FromHost;
import com.robinkirkman.hostlogs.Line;
import com.robinkirkman.hostlogs.LineMapper;
import com.robinkirkman.hostlogs.LogsListener;
import com.robinkirkman.hostlogs.Sql;

public class Viewer {
	private static final Options OPT = new Options();
	static {
		OPT.addOption("n", "nick", true, "bot bouncer nickname (req)");
		OPT.addOption("u", "user", true, "bot bouncer username (req)");
		OPT.addOption("s", "server", true, "bot bouncer server (req)");
		OPT.addOption("p", "port", true, "bot bouncer server port");
		OPT.addOption(null, "pass", true, "bot bouncer server password");
		OPT.addOption("c", "config", true, "use config properties file <arg> (default hostlogs-viewer.config )");
		OPT.addOption("w", "writeconfig", true, "write config propreties to <arg>, then quit");
		OPT.addOption(null, "dbname", true, "database name (default: irc )");
		OPT.addOption(null, "dbuser", true, "database username (default: irc )");
		OPT.addOption(null, "dbpass", true, "database password (default: irc )");
		OPT.addOption(null, "dburl", true, "database url (default: jdbc:mysql://localhost/ )");
		OPT.addOption(null, "dbcreate", false, "create the table 'lines' for logging, then quit");
		OPT.addOption("l", "lines", true, "number of history lines to view");
	}
	
	public static void main(String[] args) throws Exception {
		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "error");
		
		PosixParser pp = new PosixParser();
		CommandLine cli;
		try {
			Properties p = new Properties();
			p.setProperty("config", "hostlogs-viewer.config");
			cli = pp.parse(OPT, args, p);
			if(new File(cli.getOptionValue("config")).exists()) {
				Properties config = new Properties();
				FileInputStream in = new FileInputStream(cli.getOptionValue("config"));
				try {
					config.load(in);
				} finally {
					in.close();
				}
				cli = pp.parse(OPT, args, config);
			}
			if(!cli.hasOption("nick") || !cli.hasOption("user") || !cli.hasOption("server"))
				throw new RuntimeException();
		} catch(Exception e) {
			new HelpFormatter().printHelp("java -jar hostlogs-viewer.jar", OPT);
			System.exit(-1);
			throw new Error();
		}
		
		if(cli.hasOption("writeconfig")) {
			List<String> skipped = Arrays.asList("dbcreate", "config", "writeconfig");
			Properties config = new Properties();
			for(Option opt : cli.getOptions()) {
				if(skipped.contains(opt.getLongOpt()))
					continue;
				if(opt.getValue() != null)
					config.setProperty(opt.getLongOpt(), opt.getValue());
			}
			FileOutputStream out = new FileOutputStream(cli.getOptionValue("writeconfig"));
			try {
				config.store(out, "hostlogs-bot.jar config");
			} finally {
				out.close();
			}
			System.out.println("Wrote bot config to " + cli.getOptionValue("writeconfig"));
			System.exit(0);
			throw new Error();
		}
		
		if(cli.hasOption("dbname"))
			Sql.getOverrides().setProperty("database", cli.getOptionValue("dbname"));
		if(cli.hasOption("dbuser"))
			Sql.getOverrides().setProperty("username", cli.getOptionValue("username"));
		if(cli.hasOption("dbpass"))
			Sql.getOverrides().setProperty("password", cli.getOptionValue("dbpass"));
		if(cli.hasOption("dburl"))
			Sql.getOverrides().setProperty("url", cli.getOptionValue("dburl"));
		
		if(cli.hasOption("dbcreate")) {
			SqlSession s = Sql.get().openSession();
			try {
				s.getMapper(LineMapper.class).create();
			} finally {
				s.close();
			}
			System.exit(0);
			throw new Error();
		}
		
		int port = 6667;
		if(cli.hasOption("port"))
			port = Integer.parseInt(cli.getOptionValue("port"));
	
		final int lines = Integer.parseInt(cli.getOptionValue("lines", "100"));
		
		final ViewerListener vl = new ViewerListener();

		final Configuration<PircBotX> c = 
				new Configuration.Builder<PircBotX>().
				setName(cli.getOptionValue("nick"))
				.setLogin(cli.getOptionValue("user"))
				.setServer(cli.getOptionValue("server"), port, cli.getOptionValue("pass"))
				.addListener(vl)
				.buildConfiguration();
		
		
		Runnable irc = new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						EventQueue.invokeAndWait(new Runnable() {
							@Override
							public void run() {
								vl.clear();
							}
						});
					} catch (InvocationTargetException | InterruptedException e) {
						e.printStackTrace();
					}
					
					try {
						new PircBotX(c).startBot();
					} catch (IOException | IrcException e) {
						e.printStackTrace();
					}
				}
			}
		};
		
		JFrame frame = new ViewerFrame(vl, lines);
				
		frame.setVisible(true);
		
		new Thread(irc).start();
	}
}
