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
		
		Integer qty = null;
		
		if(value instanceof DefaultMutableTreeNode) {
			qty = ((DefaultMutableTreeNode) value).getChildCount();
			value = ((DefaultMutableTreeNode) value).getUserObject();
		}
		
		if(value instanceof Channel) {
			value = ((Channel) value).getName();
			qty = null;
		}
		if(value instanceof User)
			value = ((User) value).getNick();
		
		if(qty != null && qty > 1)
			value = "(" + qty + ") " + value; 
		
		return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
	}

}
