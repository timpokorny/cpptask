/*
 *   Copyright 2013 The Portico Project
 *
 *   This file is part of cpptask.
 * 
 *   cpptask is free software; you can redistribute and/or modify it under the
 *   terms of the Common Development and Distribution License (the "License").
 *   You may not use this file except in compliance with the License.
 *
 *   Use of this software is strictly AT YOUR OWN RISK!!!
 *   Obtain a copy of the License at http://opensource.org/licenses/CDDL-1.0
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.portico.ant.tasks.cpptask;

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
