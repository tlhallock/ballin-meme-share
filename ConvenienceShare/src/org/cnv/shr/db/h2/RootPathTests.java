package org.cnv.shr.db.h2;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.mn.Arguments;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.Misc;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RootPathTests
{
	@BeforeClass
	public static void before()
	{
		Services.settings = new Settings(Paths.get("tests/settings.props"));
		Services.settings.dbFile.set(Paths.get("tests/dbfile"));
	}

	@Before
	public void setup() throws ClassNotFoundException, SQLException, IOException 
	{
		Arguments arguments = new Arguments();
		arguments.deleteDb = true;
		Services.h2DbCache = new DbConnectionCache(arguments);
	}
	
	@After
	public void takeDown() throws ClassNotFoundException, SQLException, IOException 
	{
		Services.h2DbCache.close();
	}
	
	@Test
	public void simple() throws SQLException
	{
		testAddGet("foobar");
		Assert.assertEquals(1 + 1, DbRootPaths.getNumRootPathElements());
	}
	@Test
	public void justUnder() throws SQLException
	{
		testAddGet(Misc.getRandomString(DbRootPaths.ROOT_PATH_LENGTH-1));
		Assert.assertEquals(1 + 1, DbRootPaths.getNumRootPathElements());
	}
	@Test
	public void daLength() throws SQLException
	{
		testAddGet(Misc.getRandomString(DbRootPaths.ROOT_PATH_LENGTH));
		Assert.assertEquals(1 + 1, DbRootPaths.getNumRootPathElements());
	}
	@Test
	public void justOver() throws SQLException
	{
		testAddGet(Misc.getRandomString(DbRootPaths.ROOT_PATH_LENGTH+1));
		Assert.assertEquals(1 + 2, DbRootPaths.getNumRootPathElements());
	}
	@Test
	public void muchOver() throws SQLException
	{
		testAddGet(Misc.getRandomString(3 * DbRootPaths.ROOT_PATH_LENGTH + 5));
		Assert.assertEquals(1 + 4, DbRootPaths.getNumRootPathElements());
	}
	@Test
	public void reuseOther() throws SQLException
	{
		String randomString = Misc.getRandomString(3 * DbRootPaths.ROOT_PATH_LENGTH);
		String reuseString = randomString.substring(0, randomString.length() / 2) + Misc.getRandomString(randomString.length() / 2);
		testAddGet(randomString);
		Assert.assertEquals(1 + 3, DbRootPaths.getNumRootPathElements());
		testAddGet(reuseString);
		Assert.assertEquals(1 + 3 + 2, DbRootPaths.getNumRootPathElements());
	}
	@Test
	public void removeSimple() throws SQLException
	{
		int newPath = testAddGet("foobar");
		Assert.assertEquals(1 + 1, DbRootPaths.getNumRootPathElements());
		DbRootPaths.removeRootPath(newPath);
		Assert.assertEquals(1, DbRootPaths.getNumRootPathElements());
	}
	@Test
	public void removeLong() throws SQLException
	{
		int newPath = testAddGet(Misc.getRandomString(3 * DbRootPaths.ROOT_PATH_LENGTH + 5));
		Assert.assertEquals(1 + 4, DbRootPaths.getNumRootPathElements());
		DbRootPaths.removeRootPath(newPath);
		Assert.assertEquals(1, DbRootPaths.getNumRootPathElements());
	}
	@Test
	public void removeOneOfTwo() throws SQLException
	{
		String randomString = Misc.getRandomString(3 * DbRootPaths.ROOT_PATH_LENGTH);
		String reuseString = randomString.substring(0, randomString.length() / 2) + Misc.getRandomString(randomString.length() / 2);
		int firstPath  = testAddGet(randomString);
		Assert.assertEquals(1 + 3, DbRootPaths.getNumRootPathElements());
		int secondPath = testAddGet(reuseString);
		Assert.assertEquals(1 + 3 + 2, DbRootPaths.getNumRootPathElements());
		DbRootPaths.removeRootPath(secondPath);
		Assert.assertEquals(1 + 3, DbRootPaths.getNumRootPathElements());
		Assert.assertEquals(firstPath, DbRootPaths.getRootPath(randomString));
		DbRootPaths.removeRootPath(firstPath);
		Assert.assertEquals(1, DbRootPaths.getNumRootPathElements());
	}
	
	private static int testAddGet(String str)
	{
		int rootPath = DbRootPaths.getRootPath(str);
		String returnValue = DbRootPaths.getRootPath(rootPath);
		Assert.assertEquals(str, returnValue);
		Assert.assertEquals(rootPath, DbRootPaths.getRootPath(str));
		return rootPath;
	}
}
