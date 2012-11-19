/*
 *   Copyright 2007 littlebluefroglabs.com
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
package com.lbf.tasks.cpptask;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.tools.ant.types.Path;


public class Library
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private Path path;
	private Set<String> libs;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	public Library()
	{
		this.libs = new HashSet<String>();
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void setPath( Path givenPath )
	{
		this.path = givenPath;
	}

	public Path getPath()
	{
		return this.path;
	}

	public void setLibs( String libString )
	{
		// make sure it isn't null (not even sure if this can happen)
		if( libString == null )
			return;
		
		// break the string down with ',' as a delimiter
		StringTokenizer tokenizer = new StringTokenizer( libString, "," );
		while( tokenizer.hasMoreElements() )
		{
			this.libs.add( tokenizer.nextToken() );
		}
	}

	public Set<String> getLibs()
	{
		return this.libs;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
