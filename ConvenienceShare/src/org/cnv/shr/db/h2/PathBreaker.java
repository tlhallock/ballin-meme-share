package org.cnv.shr.db.h2;

import java.util.LinkedList;

import org.cnv.shr.mdl.PathElement;

public class PathBreaker
{
	public static int PATH_ELEMENT_LENGTH = 10;
	
	private static final PathElement[] dummy = new PathElement[0];

	public static PathElement[] breakPath(PathElement parent, String path)
	{
		LinkedList<PathElement> returnValue = new LinkedList<>();
		
		StringBuilder builder = new StringBuilder(PATH_ELEMENT_LENGTH);
		
		int idx = 0;
		
		outer:
		while (idx < path.length())
		{
			for (int i = 0; i < PATH_ELEMENT_LENGTH && idx < path.length(); i++)
			{
				char c = path.charAt(idx++);
				builder.append(c);
				if (c == '/')
				{
					returnValue.add(parent = new PathElement(parent, builder.toString(), false));
					builder.setLength(0);
					continue outer;
				}
			}
			if (idx == path.length())
			{
				returnValue.add(parent = new PathElement(parent, builder.toString(), false));
				builder.setLength(0);
				return returnValue.toArray(dummy);
			}
			returnValue.add(parent = new PathElement(parent, builder.toString(), true));
			builder.setLength(0);
		}
		
		return returnValue.toArray(dummy);
	}
	
	public static PathElement[] breakPath(String path)
	{
		return breakPath(DbPaths.ROOT, path);
	}

	public static String join(PathElement[] eles)
	{
		StringBuilder builder = new StringBuilder(eles.length * PATH_ELEMENT_LENGTH);
		
		for (PathElement ele : eles)
		{
//			builder.append("[");
			builder.append(ele.getName());
//			if (ele.isBroken())
//			{
//				builder.append("...");
//			}
//			builder.append("]");
		}
		
		return builder.toString();
	}
	
	public static void main(String[] args0)
	{
//		String str = "Combinatorial optimization.. theory and algorithms.pdf";
		String str = "/home/thallock/Documents/Combinatorial optimization.. theory and algorithms.pdf";
		System.out.println(str);
		System.out.println(join(breakPath(str)));
		System.out.println(breakPath(str)[breakPath(str).length-1].getUnbrokenName());
	}
}
