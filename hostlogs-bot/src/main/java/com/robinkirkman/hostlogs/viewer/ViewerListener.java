package com.robinkirkman.hostlogs.viewer;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

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

import com.robinkirkman.hostlogs.FromHost;

import static com.robinkirkman.hostlogs.viewer.SortingTreeModel.*;

public class ViewerListener extends ListenerAdapter<PircBotX> {
	private static final Comparator<Channel> CHANNEL_ORDER = new Comparator<Channel>() {
		@Override
		public int compare(Channel o1, Channel o2) {
			return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
		}
	};
	
	private static final Comparator<User> USER_ORDER = new Comparator<User>() {
		@Override
		public int compare(User o1, User o2) {
			return String.CASE_INSENSITIVE_ORDER.compare(o1.getNick(), o2.getNick());
		}
	};
	
	private static final Comparator<Object> USER_NICK_ORDER = new Comparator<Object>() {
		@Override
		public int compare(Object o1, Object o2) {
			String s1 = (o1 instanceof User) ? ((User) o1).getNick() : String.valueOf(o1);
			String s2 = (o2 instanceof User) ? ((User) o2).getNick() : String.valueOf(o2);
			return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
		}
	};
	
	private static final Comparator<FromHost> HOST_ORDER = new Comparator<FromHost>() {
		@Override
		public int compare(FromHost o1, FromHost o2) {
			return String.CASE_INSENSITIVE_ORDER.compare(o1.getHost(), o2.getHost());
		}
	};
	
	private DefaultMutableTreeNode root = new DefaultMutableTreeNode("Channels");
	
	private SortingTreeModel model = new SortingTreeModel(root);
	
	public DefaultTreeModel getModel() {
		return model;
	}
	
	public void clear() {
		while(root.getChildCount() > 0)
			model.removeNodeFromParent((DefaultMutableTreeNode) root.getChildAt(0));
	}
	
	private DefaultMutableTreeNode channelNode(Channel c) {
		return model.childWithObject(root, c, EQUALS);
	}
	
	private DefaultMutableTreeNode ensureChannelNode(Channel c) {
		DefaultMutableTreeNode node = model.childWithObject(root, c, EQUALS);
		if(node != null)
			return node;
		node = new DefaultMutableTreeNode(c);
		model.sortNodeInto(node, root, CHANNEL_ORDER);
		model.insertNodeInto(new DefaultMutableTreeNode("Users"), node, 0);
		model.insertNodeInto(new DefaultMutableTreeNode("Hosts"), node, 1);
		return node;
	}
	
	private DefaultMutableTreeNode usersNode(Channel c) {
		DefaultMutableTreeNode channelNode = model.childWithObject(root, c, EQUALS);
		if(channelNode == null)
			return null;
		return (DefaultMutableTreeNode) channelNode.getChildAt(0);
	}
	
	private DefaultMutableTreeNode hostsNode(Channel c) {
		DefaultMutableTreeNode channelNode = model.childWithObject(root, c, EQUALS);
		if(channelNode == null)
			return null;
		return (DefaultMutableTreeNode) channelNode.getChildAt(1);
	}
	
	private DefaultMutableTreeNode hostNode(Channel c, FromHost host) {
		DefaultMutableTreeNode hostsNode = hostsNode(c);
		return model.childWithObject(hostsNode, host, HOST_ORDER);
	}
	
	private DefaultMutableTreeNode ensureHostNode(Channel c, FromHost host) {
		DefaultMutableTreeNode hostsNode = hostsNode(c);
		DefaultMutableTreeNode hostNode = model.childWithObject(hostsNode, host, HOST_ORDER);
		if(hostNode != null)
			return hostNode;
		hostNode = new DefaultMutableTreeNode(host);
		model.sortNodeInto(hostNode, hostsNode, HOST_ORDER);
		return hostNode;
	}
	
	private void joined(User botuser, Channel c, User u) {
		ensureChannelNode(c);
		
		DefaultMutableTreeNode usersNode = usersNode(c);
		
		if(model.childWithObject(usersNode, u, EQUALS) != null)
			return;
		
		model.sortValueInto(u, usersNode, USER_ORDER);
		
		FromHost host = new FromHost(u.getHostmask());
		DefaultMutableTreeNode hostNode = ensureHostNode(c, host);
		model.sortValueInto(u, hostNode, USER_ORDER);
	}
	
	private void parted(User botuser, Channel c, User u) {
		if(botuser.equals(u)) {
			model.removeNodeFromParent(channelNode(c));
			return;
		}

		DefaultMutableTreeNode usersNode = usersNode(c);
		DefaultMutableTreeNode userNode = model.childWithObject(usersNode, u, USER_ORDER);
		if(userNode == null)
			return;
		
		model.removeNodeFromParent(userNode);
		
		FromHost host = new FromHost(u.getHostmask());
		DefaultMutableTreeNode hostNode = hostNode(c, host);
		
		model.removeNodeFromParent(model.childWithObject(hostNode, u, USER_ORDER));
		
		if(hostNode.getChildCount() == 0)
			model.removeNodeFromParent(hostNode);
	}
	
	private void nickchanged(User botuser, Channel c, User u, String oldnick, String newnick) {
		DefaultMutableTreeNode usersNode = usersNode(c);
		
		model.removeNodeFromParent(model.childWithObject(usersNode, oldnick, USER_NICK_ORDER));
		model.sortValueInto(u, usersNode, USER_ORDER);
		
		FromHost host = new FromHost(u.getHostmask());
		DefaultMutableTreeNode hostNode = hostNode(c, host);
		
		model.removeNodeFromParent(model.childWithObject(hostNode, oldnick, USER_NICK_ORDER));
		model.sortValueInto(u, hostNode, USER_ORDER);
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
				for(Object channelNode : Collections.list(root.children())) {
					Channel c = (Channel) ((DefaultMutableTreeNode) channelNode).getUserObject();
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
				for(Object channelNode : Collections.list(root.children())) {
					Channel c = (Channel) ((DefaultMutableTreeNode) channelNode).getUserObject();
					nickchanged(event.getBot().getUserBot(), c, event.getUser(), event.getOldNick(), event.getNewNick());
				}
			}
		});
	}
}
