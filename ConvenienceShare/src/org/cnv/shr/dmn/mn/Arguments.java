package org.cnv.shr.dmn.mn;

import java.io.File;

import org.cnv.shr.stng.Settings;

public class Arguments
{
	// needs to have a quitting and testing...
	public boolean connectedToTestStream = false;
	public boolean deleteDb = false;
	public Settings settings = new Settings(new File("app" + File.separator + "settings.props"));
	public Quiter quiter = new Quiter() {
		@Override
		public void doFinal()
		{
			System.exit(0);
		}};
	String testIp;
	String testPort;
	
	public String updateManagerDirectory;
}