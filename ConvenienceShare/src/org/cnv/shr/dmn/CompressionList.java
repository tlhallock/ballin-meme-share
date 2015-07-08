package org.cnv.shr.dmn;

import org.cnv.shr.json.JsonStringSet;

public class CompressionList
{
	JsonStringSet whiteList;
	JsonStringSet blackList;
	
	private static final String[] compressionWhiteList = new String[] {
		".txt",
		".html",
		".xml",
		".json",
		".java",
		".cpp",
		".c",
		".py",

//		".sh",
	};
	// Files are already compressed.
	private static final String[] compressionBlackList = new String[] {
		".gz",
		".rar",
		".bz2",
		".zip",
		".jar",
		".war",
		".7zip",
		".7z",
		".png",
		".jpeg",
		".jpg",
		".avi",
		".mp3",
		".mp4",
		".mkv",
		// ...
	};

	public boolean shouldCompressFile(String name)
	{
		// TODO: hash based lookup of extension
		for (String ext : compressionBlackList)
		{
			if (name.toLowerCase().endsWith(ext))
			{
				return false;
			}
		}
		return true;
	}

	public boolean alwaysCompress(String name)
	{
		// TODO: hash based lookup of extension
		for (String ext : compressionWhiteList)
		{
			if (name.toLowerCase().endsWith(ext))
			{
				return true;
			}
		}
		return false;
	}
}
