package com.robinkirkman.hostlogs.viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.ibatis.session.SqlSession;
import org.pircbotx.Channel;
import org.pircbotx.User;

import com.robinkirkman.hostlogs.FromHost;
import com.robinkirkman.hostlogs.Line;
import com.robinkirkman.hostlogs.LineMapper;
import com.robinkirkman.hostlogs.Sql;

public class ViewerFrame extends JFrame {
	protected static final SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm");
	
	protected ViewerListener vl;
	protected JTree tree;
	protected JEditorPane logs;
	protected Timer refresh;
	
	protected int lines;
	protected String logsHtml;
	
	public ViewerFrame(ViewerListener vl, int lines) {
		super("IRC");
		
		this.vl = vl;
		this.lines = lines;
		
		tree = new JTree(vl.getModel());
		
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(new ViewerTreeCellRenderer());
		tree.setShowsRootHandles(true);
		
		logs = new JEditorPane("text/html", "");
		logs.setEditable(false);
		
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				showLogs(e.getPath());
			}
		});
		
		refresh = new Timer(10000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showLogs(tree.getSelectionPath());
			}
		});
		
		refresh.setCoalesce(true);
		refresh.setRepeats(true);
		
		JScrollPane treescroll = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		treescroll.setPreferredSize(new Dimension(200, 300));
		
		setLayout(new BorderLayout());
		add(treescroll, BorderLayout.WEST);
		add(new JScrollPane(logs, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
		pack();
		setSize(800, 400);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
	}

	@Override
	public void setVisible(boolean b) {
		if(b)
			refresh.start();
		else
			refresh.stop();
		super.setVisible(b);
	}
	
	protected void showLogs(TreePath path) {
		if(path == null)
			return;
		
		Object o = path.getLastPathComponent();
		if(o instanceof DefaultMutableTreeNode)
			o = ((DefaultMutableTreeNode) o).getUserObject();
		if(!(o instanceof User) && !(o instanceof FromHost)) {
			logs.setText("");
			setTitle("IRC");
			return;
		}
		final String host;
		if(o instanceof User) {
			host = ((User) o).getHostmask();
		} else {
			host = ((FromHost) o).getHost();
		}
		Object[] p = path.getPath();
		String ch = null;
		for(Object n : p) {
			Object userObject = ((DefaultMutableTreeNode) n).getUserObject();
			if(userObject instanceof Channel)
				ch = ((Channel) userObject).getName();
		}
		final String channel = ch;

		final SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
		
		Runnable task = new Runnable() {
			@Override
			public void run() {
				SqlSession sq = Sql.get().openSession();
				try {
					
					FromHost from = new FromHost();
					from.setHost(host.toLowerCase());
					from.setTo(channel.toLowerCase());
					from.setLines(lines);
					List<Line> lines = sq.getMapper(LineMapper.class).last(from);
					Collections.reverse(lines);
					String text = "<html>";
					Line prev = null;
					for(Line l : lines) {
						text += lineHeader(prev, l) + l.getLine() + "<br>\n";
						prev = l;
					}
					text += "</html>";
					if(text.equals(logsHtml))
						return;
					final String t = text;
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							logs.setText(logsHtml = t);
							setTitle(host + " / " + channel);
						}
					});
					
				} finally {
					loop.exit();
					sq.close();
				}
			}
		};
		
		new Thread(task).start();
		loop.enter();		
	}

	protected String lineHeader(Line prev, Line line) {
		String ph = prev != null ? (df.format(prev.getTs()) + " " + prev.getNick() + ": ") : "";
		String lh = df.format(line.getTs()) + " " + line.getNick() + ": ";
		ph = ph.replaceAll("(\\S+)", "<b>$1</b>");
		lh = lh.replaceAll("(\\S+)", "<b>$1</b>");
		int i = 0;
		int s = 0;
		for(; i < ph.length() && i < lh.length(); i++) {
			if(ph.charAt(i) != lh.charAt(i))
				break;
			if(ph.charAt(i) == ' ')
				s = i;
		}
		return lh.substring(s).replaceAll("^ ?", "\u2192");
	}
}
