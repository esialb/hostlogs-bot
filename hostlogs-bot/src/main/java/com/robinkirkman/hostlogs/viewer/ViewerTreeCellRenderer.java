package com.robinkirkman.hostlogs.viewer;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.pircbotx.Channel;
import org.pircbotx.User;

public class ViewerTreeCellRenderer extends DefaultTreeCellRenderer {

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		
		if(value instanceof DefaultMutableTreeNode)
			value = ((DefaultMutableTreeNode) value).getUserObject();
		
		if(value instanceof Channel)
			value = ((Channel) value).getName();
		if(value instanceof User)
			value = ((User) value).getNick();
		
		return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
	}

}
