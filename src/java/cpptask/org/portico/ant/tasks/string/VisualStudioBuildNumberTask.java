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
package org.portico.ant.tasks.string;

import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.portico.ant.tasks.utils.PropertyUtils;

public class VisualStudioBuildNumberTask extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String property;
	private String value;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public VisualStudioBuildNumberTask()
	{
		this.property = null;
		this.value = null;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	public void execute()
	{
		// validate given values
		if( this.property == null )
			throw new BuildException( "Argument \"property\" must be provided" );
		else
			this.property = property.trim();
		
		if( this.value == null )
			throw new BuildException( "Argument \"value\" must be provided" );
		else
			this.value = value.trim();
		
		// break the string apart based on "." characters and replace them with ","
		// sanitize the input on the way
		String[] values = new String[4];
		StringTokenizer tokenizer = new StringTokenizer( this.value, "." );
		for( int i = 0; i < 4; i++ ) // we need 4 values max
		{
			String token = "0";
			if( tokenizer.hasMoreTokens() )
				token = tokenizer.nextToken();
			
			// remove all non-numeric characters
			token = token.replaceAll( "[^\\d]", "" );
			values[i] = token;
		}
		
		// we now have four numbers - put them in the x,x,x,x format and set the property
		String finishedVersion = values[0]+","+values[1]+","+values[2]+","+values[3];
		PropertyUtils.setProjectProperty( super.getProject(), property, finishedVersion, true );
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	public void setProperty( String property )
	{
		this.property = property;
	}
	
	public void setValue( String value )
	{
		this.value = value;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
