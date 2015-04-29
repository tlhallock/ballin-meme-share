package org.cnv.shr.dmn;

import java.util.LinkedList;
import org.cnv.shr.gui.RemoteView;


public class Notifications
{   
	public static void localsChanged()
	{
		if (Services.application != null)
		{
			Services.application.refreshLocals();
		}
		Services.locals.write();
	}
	
	public static void remotesChanged()
	{
		if (Services.application != null)
		{
			Services.application.refreshRemotes();
		}
		Services.remotes.write();
	}
}
