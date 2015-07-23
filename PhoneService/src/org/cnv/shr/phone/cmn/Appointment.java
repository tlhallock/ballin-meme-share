package org.cnv.shr.phone.cmn;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class Appointment implements Comparable<Appointment>, Storable
{
	private static final long APPOINTMENT_ACTIVE_WINDOW = 5 * 60 * 1000;
	
	private long date;
	private PhoneNumberWildCard number;
	
	public Appointment(long date, PhoneNumberWildCard number)
	{
		this.date = date;
		this.number = number;
	}

	public Appointment(JsonParser parser)
	{
		parse(parser);
	}

	@Override
	public int compareTo(Appointment o)
	{
		int c = Long.compare(date, o.date);
		if (c != 0)
		{
			return c;
		}
		return toString().compareTo(o.toString());
	}
	
	public boolean equals(Object other)
	{
		if (!(other instanceof Appointment))
		{
			return false;
		}
		Appointment app = (Appointment) other;
		return app.date == date && number.equals(app.number);
	}
	
	public boolean isPast()
	{
		return date < System.currentTimeMillis() - APPOINTMENT_ACTIVE_WINDOW; 
	}
	
	public boolean isActive()
	{
		long diff = System.currentTimeMillis() - date;
		return diff > -APPOINTMENT_ACTIVE_WINDOW && diff < APPOINTMENT_ACTIVE_WINDOW;
	}

	public long getDate()
	{
		return date;
	}
	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK

	@Override
	public void generate(JsonGenerator generator, String key)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void parse(JsonParser parser)
	{
		// TODO Auto-generated method stub
		
	}
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
