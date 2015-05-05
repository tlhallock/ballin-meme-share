package org.cnv.shr.util;

import java.util.Hashtable;

public class ThreadPrivate<T>
{
	interface Allocator<T> { T create(); }
	
	private Allocator<T> a;
	private Hashtable<Long, T> values = new Hashtable<>();
	
	public ThreadPrivate(Allocator<T> a)
	{
		this.a = a;
	}
	
	public T get()
	{
		long id = Thread.currentThread().getId();
		T t = values.get(id);
		if (t == null)
		{
			t = a.create();
			values.put(id, t);
		}
		return t;
	}
}
