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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.portico.ant.tasks.utils.PropertyUtils;


/**
 * This class will replace all occurances of a given string in another given string and will
 * set the result in the property identified. It also has the ability to override the mutable
 * aspect of ant properties. It is meant for cases when simple find/replace behaviour is needed.
 * The primary motivating case was to take a java package name and convert it into a path
 * (converting all the "." into "/").
 */
public class StringReplaceTask extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String property;
	private String input;
	private String find;
	private String replace;
	private boolean overwrite = false;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void execute()
	{
		// check to see that all the values are set
		checkValues();
		
		// do the replacement and set the value
		PropertyUtils.setProjectProperty( super.getProject(),
		                                  property,
		                                  input.replace(find,replace),
		                                  overwrite );
	}

	private void checkValues()
	{
		if( this.property == null )
			throw new BuildException( "Must set the \"property\" attribute" );
		else if( this.input == null )
			throw new BuildException( "Must set the \"input\" attribute" );
		else if( this.find == null )
			throw new BuildException( "Must set the \"find\" attribute" );
		else if( this.replace == null )
			throw new BuildException( "Must set the \"replace\" attribute" );
	}
	
	////////////////////////////////////////////////////////////
	//////////////////////// Attributes ////////////////////////
	////////////////////////////////////////////////////////////
	public void setProperty( String property )
	{
		this.property = property;
	}
	
	public void setInput( String input )
	{
		this.input = input;
	}
	
	public void setFind( String find )
	{
		this.find = find;
	}

	public void setReplace( String replace )
	{
		this.replace = replace;
	}
	
	public void setOverwrite( boolean overwrite )
	{
		this.overwrite = overwrite;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
