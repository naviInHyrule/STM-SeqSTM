package data;

import java.io.*;
import java.util.ArrayList;

public class DataFileFilter implements FileFilter 
{
	private ArrayList<String> fileNameExt = new ArrayList<String>();
	
	public DataFileFilter(String fileNameExt)
	{
		this.fileNameExt.add(fileNameExt);
	}
	
	public DataFileFilter(String extOne, String extTwo)
	{
		this.fileNameExt.add(extOne);
		this.fileNameExt.add(extTwo);
	}
	
	public DataFileFilter(String[] fileNameExts){
		for(int i = 0; i < fileNameExts.length; i++){
			if(fileNameExts[i] != null)
				this.fileNameExt.add(fileNameExts[i]);
		}
	}
	
	public boolean accept(File file)
	{
		for(int i = 0; i < fileNameExt.size(); i++)
			if(file.toString().endsWith(fileNameExt.get(i)))
				return true;
		return false;
	}
}
