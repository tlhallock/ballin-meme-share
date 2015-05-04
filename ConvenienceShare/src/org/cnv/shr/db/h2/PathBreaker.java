package org.cnv.shr.db.h2;

import java.util.LinkedList;

import org.cnv.shr.mdl.PathElement;

public class PathBreaker
{
	public static int PATH_ELEMENT_LENGTH = 10;
	
	private static final PathElement[] dummy = new PathElement[0];
	
	public static PathElement[] breakPath(String path)
	{
		LinkedList<PathElement> returnValue = new LinkedList<>();
		
		StringBuilder builder = new StringBuilder(PATH_ELEMENT_LENGTH);
		
		int idx = 0;
		
		outer:
		while (idx < path.length())
		{
			for (int i = 0; i < PATH_ELEMENT_LENGTH; i++)
			{
				char c = path.charAt(idx++);
				builder.append(c);
				if (c == '/')
				{
					returnValue.add(new PathElement(builder.toString(), false));
					builder.setLength(0);
					continue outer;
				}
			}
			returnValue.add(new PathElement(builder.toString(), true));
			builder.setLength(0);
		}
		
		return returnValue.toArray(dummy);
	}

	public static String join(PathElement[] eles)
	{
		StringBuilder builder = new StringBuilder(eles.length * PATH_ELEMENT_LENGTH);
		
		for (PathElement ele : eles)
		{
			builder.append(ele.getName());
		}
		
		return builder.toString();
	}
}
