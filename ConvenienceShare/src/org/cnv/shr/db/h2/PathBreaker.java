package org.cnv.shr.db.h2;

import java.util.LinkedList;

import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.util.Misc;

public class PathBreaker
{
	public static int PATH_ELEMENT_LENGTH = 20;
	
	private static final PathElement[] dummy = new PathElement[0];

	public static PathElement[] breakPath(PathElement parent, String path)
	{
		path = Misc.sanitizePath(path);
		System.out.println(path);
		
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
}
