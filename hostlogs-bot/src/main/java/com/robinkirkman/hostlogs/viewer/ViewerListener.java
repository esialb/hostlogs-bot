package com.robinkirkman.hostlogs.viewer;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.UserListEvent;

public class ViewerListener extends ListenerAdapter<PircBotX> {
	
	private List<Channel> channels = new ArrayList<>();
	private Map<Channel, List<String>> members = new TreeMap<>();
	
	private DefaultMutableTreeNode root = new DefaultMutableTreeNode("Channels");
	
	private DefaultTreeModel model = new DefaultTreeModel(root);
	
	public DefaultTreeModel getModel() {
		return model;
	}
	
	public void clear() {
		while(root.getChildCount() > 0)
			model.removeNodeFromParent((DefaultMutableTreeNode) root.getChildAt(0));
	}
	
	private void joined(User botuser, Channel c, User u) {
		if(!members.containsKey(c)) {
			members.put(c, new ArrayList<String>());
			DefaultMutableTreeNode cn = new DefaultMutableTreeNode(c);
			channels.add(c);
			Collections.sort(channels);
			model.insertNodeInto(cn, root, channels.indexOf(c));
		}
		List<String> users = members.get(c);
		if(users.contains(u.getNick()))
			return;
		users.add(u.getNick());
		Collections.sort(users, String.CASE_INSENSITIVE_ORDER);
		DefaultMutableTreeNode cn = (DefaultMutableTreeNode) root.getChildAt(channels.indexOf(c));
		DefaultMutableTreeNode un = new DefaultMutableTreeNode(u);
		model.insertNodeInto(un, cn, users.indexOf(u.getNick()));
	}
	
	private void parted(User botuser, Channel c, User u) {
		int ci = channels.indexOf(c);
		DefaultMutableTreeNode cn = (DefaultMutableTreeNode) root.getChildAt(ci);

		if(botuser.equals(u)) { // remove the entire channel, we parted
			model.removeNodeFromParent(cn);
			channels.remove(c);
			members.remove(c);
			return;
		}
		
		List<String> users = members.get(c);
		int ui = users.indexOf(u.getNick());
		if(ui == -1)
			return;
		DefaultMutableTreeNode un = (DefaultMutableTreeNode) cn.getChildAt(ui);
		model.removeNodeFromParent(un);
		users.remove(ui);

	}
	
	private void nickchanged(User botuser, Channel c, User u, String oldnick, String newnick) {
		int ci = channels.indexOf(c);
		DefaultMutableTreeNode cn = (DefaultMutableTreeNode) root.getChildAt(ci);

		if(botuser.equals(u)) { // remove the entire channel, we parted
			model.removeNodeFromParent(cn);
			channels.remove(c);
			members.remove(c);
			return;
		}
		
		List<String> users = members.get(c);
		int ui = users.indexOf(oldnick);
		if(ui == -1)
			return;
		DefaultMutableTreeNode un = (DefaultMutableTreeNode) cn.getChildAt(ui);
		model.removeNodeFromParent(un);
		users.remove(ui);
		
		users.add(newnick);
		Collections.sort(users, String.CASE_INSENSITIVE_ORDER);
		un = new DefaultMutableTreeNode(u);
		model.insertNodeInto(un, cn, users.indexOf(newnick));
	}
	
	@Override
	public void onJoin(final JoinEvent<PircBotX> event) throws Exception {
		EventQueue.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				User botuser = event.getBot().getUserBot();
				Channel c = event.getChannel();
				User u = event.getUser();

				joined(botuser, c, u);
			}
		});
	}

	@Override
	public void onPart(final PartEvent<PircBotX> event) throws Exception {
		EventQueue.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				User botuser = event.getBot().getUserBot();
				Channel c = event.getChannel();
				User u = event.getUser();
				
				parted(botuser, c, u);
			}
		});
	}

	@Override
	public void onUserList(final UserListEvent<PircBotX> event) throws Exception {
		EventQueue.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				for(User u : event.getUsers())
					joined(event.getBot().getUserBot(), event.getChannel(), u);
			}
		});
	}
	
	@Override
	public void onQuit(final QuitEvent<PircBotX> event) throws Exception {
		EventQueue.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				for(Channel c : channels) {
					parted(event.getBot().getUserBot(), c, event.getUser());
				}
			}
		});
	}

	@Override
	public void onNickChange(final NickChangeEvent<PircBotX> event) throws Exception {
		EventQueue.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				for(Channel c : channels) {
					nickchanged(event.getBot().getUserBot(), c, event.getUser(), event.getOldNick(), event.getNewNick());
				}
			}
		});
	}
}
