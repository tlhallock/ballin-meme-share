package org.cnv.shr.ports;

import java.util.NoSuchElementException;
import java.util.TreeSet;

class IntervalPersistance
{
	private TreeSet<WrittenInterval> tree = new TreeSet<>();
	private int start = 0;
	private final int maximum;

	public IntervalPersistance(int maximum)
	{
		this.maximum = maximum;
	}
	
	/* Test methods */
	int treeSize()
	{
		return tree.size();
	}
	
	int start()
	{
		return start;
	}
	
	public void add(int left, int right) 
	{
		if (left == right)
		{
			return;
		}
		if (right > maximum)
		{
			throw new IndexOutOfBoundsException();
		}
		WrittenInterval newNode = new WrittenInterval(left, right);
		
		WrittenInterval other = tree.ceiling(newNode);
		while (other != null && other.leftIndex <= newNode.rightIndex)
		{
			newNode.leftIndex  = Math.min(newNode.leftIndex, other.leftIndex);
			newNode.rightIndex = Math.max(newNode.rightIndex, other.rightIndex);
			tree.remove(other);
			other = tree.ceiling(other);
		}

		other = tree.floor(newNode);
		while (other != null && other.rightIndex >= newNode.leftIndex)
		{
			newNode.leftIndex  = Math.min(newNode.leftIndex, other.leftIndex);
			newNode.rightIndex = Math.max(newNode.rightIndex, other.rightIndex);
			tree.remove(other);
			other = tree.floor(other);
		}

		tree.add(newNode);
	}
	
	public WrittenInterval getNextBlock()
	{
		WrittenInterval returnValue = new WrittenInterval();
		returnValue.leftIndex = start;
		WrittenInterval floor = tree.floor(returnValue);
		if (floor == null || floor.rightIndex <= start)
		{
			return null;
		}
		returnValue.rightIndex = floor.rightIndex;
		return returnValue;
	}

	private final WrittenInterval DUMMY = new WrittenInterval();
	public void remove(int amount) throws NoSuchElementException
	{
		if (amount == 0)
		{
			return;
		}
		DUMMY.leftIndex = start;
		WrittenInterval next = tree.floor(DUMMY);
		if ((next == null || next.rightIndex - start < amount) 
//					&& 
//					(next.rightIndex != maximum || tree.first().leftIndex != 0)
					)
		{
			throw new NoSuchElementException("Nothing to be read right now.");
		}
		if (start == next.leftIndex)
		{
			next.leftIndex += amount;
			if (next.leftIndex == next.rightIndex)
			{
				tree.remove(next);
			}
		}
		else if (start + amount == next.rightIndex)
		{
			next.rightIndex = start;
		}
		else
		{
			tree.add(new WrittenInterval(start + amount, next.rightIndex));
			next.rightIndex = start;
		}

		start += amount;
		if ( /*tree.isEmpty() || */ start >= maximum)
		{
			start = 0;
		}
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		builder.append("start = ").append(start).append('\n');
		for (WrittenInterval node : tree)
		{
			node.toString(builder);
		}
		builder.append('\n');
		return builder.toString();
	}
	
	public static final class WrittenInterval implements Comparable<WrittenInterval>
	{
		int leftIndex;
		int rightIndex;
		
		WrittenInterval() { this (0, 0); }
		
		WrittenInterval(int left, int right)
		{
			this.leftIndex = left;
			this.rightIndex = right;
		}
		
		public int length()
		{
			return rightIndex - leftIndex;
		}
		
		@Override
		public int compareTo(WrittenInterval o)
		{
			return Integer.compare(leftIndex, o.leftIndex);
		}
		
		public boolean contains(int offset)
		{
			return leftIndex <= offset && offset <= rightIndex;
		}
		
		public StringBuilder toString(StringBuilder builder)
		{
			return builder.append(leftIndex).append('-').append(rightIndex).append('\t');
		}
		
		public String toString()
		{
			return toString(new StringBuilder()).toString();
		}
		
		public boolean equals(Object o)
		{
			if (!(o instanceof WrittenInterval))
			{
				return false;
			}
			WrittenInterval other = (WrittenInterval) o;
			return leftIndex == other.leftIndex && rightIndex == other.rightIndex;
		}
	}
}
