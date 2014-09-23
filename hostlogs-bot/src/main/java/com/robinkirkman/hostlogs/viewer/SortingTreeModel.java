package com.robinkirkman.hostlogs.viewer;

import java.util.Comparator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

public class SortingTreeModel extends DefaultTreeModel {
	public static final Comparator<Object> NATURAL_ORDER = new Comparator<Object>() {
		@Override
		public int compare(Object o1, Object o2) {
			return ((Comparable) o1).compareTo(o2);
		}
	};
	
	public static final Comparator<Object> EQUALS = new Comparator<Object>() {
		@Override
		public int compare(Object o1, Object o2) {
			return (o1 == null ? o2 == null : o1.equals(o2)) ? 0 : 1;
		}
	};
	
	public SortingTreeModel(TreeNode root) {
		super(root);
	}
	
	@Override
	public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
		super.insertNodeInto(newChild, parent, index);
		nodeChanged(parent);
	}
	
	public <T> void sortNodeInto(DefaultMutableTreeNode newChild, DefaultMutableTreeNode parent, Comparator<? super T> cmp) {
		T obj = (T) newChild.getUserObject();
		int index = 0;
		for(; index < parent.getChildCount(); index++)
			if(cmp.compare(obj, (T) ((DefaultMutableTreeNode) parent.getChildAt(index)).getUserObject()) < 0)
				break;
		insertNodeInto(newChild, parent, index);
	}

	public <T> void sortValueInto(T newValue, DefaultMutableTreeNode parent, Comparator<? super T> cmp) {
		sortNodeInto(new DefaultMutableTreeNode(newValue), parent, cmp);
	}
	
	public <T> DefaultMutableTreeNode childWithObject(DefaultMutableTreeNode parent, T childObject, Comparator<? super T> cmp) {
		for(int index = 0; index < parent.getChildCount(); index++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(index);
			T nodeUserObject = (T) node.getUserObject();
			if(cmp.compare(childObject, nodeUserObject) == 0)
				return node;
		}
		return null;
			
	}
}
