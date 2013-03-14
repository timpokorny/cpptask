/*
 *   Copyright 2013 The Portico Project
 *
 *   This file is part of cpptask.
 *
 *   cpptask is free software; you can redistribute it and/or modify
 *   it under the terms of the Common Developer and Distribution License (CDDL) 
 *   as published by Sun Microsystems. For more information see the LICENSE file.
 *
 *   Use of this software is strictly AT YOUR OWN RISK!!!
 *   If something bad happens you do not have permission to come crying to me.
 *   (that goes for your lawyer as well)
 *
 */
package org.portico.ant.tasks.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StringUtilities
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	/**
	 * Converts a File[] to a String[] that contains the absolute paths of each file
	 */
	public static List<String> filesToStrings( File[] files )
	{
		ArrayList<String> strings = new ArrayList<String>();
		for( File file : files )
			strings.add( file.getAbsolutePath() );
		
		return strings;
	}
	
	/**
	 * Converts a List<File> to a String[] that contains the absolute paths of each file
	 */
	public static List<String> filesToStrings( List<File> files )
	{
		ArrayList<String> strings = new ArrayList<String>();
		for( File file : files )
			strings.add( file.getAbsolutePath() );
		
		return strings;
	}

	/**
	 * This method takes the given file and gets its absolute path. It then locates the last
	 * instance of the "." character in that path, strips off everything from there on out and
	 * replaces it with the given extension, returning a *new* file for the path. If there is
	 * no extension, the given extension is added.
	 * <p/>
	 * No check is made to see whether the new file exists or not. The <code>newExtension</code>
	 * parameter can be provided with or without a prepending '.' character. Examples of valid
	 * values are ".new", "new".
	 * 
	 * @param existingFile The existing file
	 * @param newExtension The extension to use in place of what already exists. Can be provided
	 *                     with or without a prepended "." character
	 * @return A new file pointing to the path of the old file with a new extension. No check is
	 *         made to see whether or not this exists
	 */
	public static File changeExtension( File existingFile, String newExtension )
	{
		// make sure we have a period on the front of the extension
		if( newExtension.startsWith(".") == false )
			newExtension = "."+newExtension;
		
		// get the current path of the existing file and strip off the extension
		String existingPath = existingFile.getAbsolutePath();
		int lastPeriodIndex = existingPath.lastIndexOf( "." );
		if( lastPeriodIndex > -1 )
			existingPath = existingPath.substring( 0, lastPeriodIndex );
		
		return new File( existingPath+newExtension );
	}

	/**
	 * Returns a String[] that is a combination of the given commands followed by the absolute
	 * paths of the given files.
	 */
	public static String[] concat( String[] commands, File[] files )
	{
		String[] complete = new String[commands.length+files.length];
		for( int i = 0; i < commands.length; i++ )
			complete[i] = commands[i];
		
		for( int i = 0; i < files.length; i++ )
			complete[i+commands.length] = files[i].getAbsolutePath();
			
		return complete;
	}

	/**
	 * Returns true if the given array contains the given value. False otherwise. Each
	 * element will be stripped of whitespace before being compared.
	 * 
	 * @param array The array to look into
	 * @param value The value to look for
	 */
	public static boolean arrayContains( String[] array, String value )
	{
		for( String potential : array )
		{
			if( potential.trim().equals(value) )
				return true;
		}

		// didn't find it
		return false;
	}

}
