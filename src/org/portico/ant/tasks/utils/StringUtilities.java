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
	public static String[] filesToStrings( File[] files )
	{
		String[] strings = new String[files.length];
		for( int i = 0; i < files.length; i++ )
			strings[i] = files[i].getAbsolutePath();
		
		return strings;
	}
	
	/**
	 * Converts a List<File> to a String[] that contains the absolute paths of each file
	 */
	public static String[] filesToStrings( List<File> files )
	{
		String[] strings = new String[files.size()];
		for( int i = 0; i < files.size(); i++ )
			strings[i] = files.get(i).getAbsolutePath();
		
		return strings;
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
