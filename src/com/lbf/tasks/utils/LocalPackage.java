/*
 *   Copyright 2008 littlebluefroglabs.com
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
package com.lbf.tasks.utils;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * This task takes a package name and returns just the local portion of it, setting the value in
 * the identified property. For example, if the package name "com.lbf.tasks" were passed, the
 * value of the identified property would be set to "tasks".
 */
public class LocalPackage extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String property;
	private String packageName;
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
		
		// get the package name
		int lastIndex = packageName.lastIndexOf( "." );
		String localName = packageName.substring( lastIndex+1, packageName.length() );
		
		// do the replacement and set the value
		Utils.setProjectProperty( super.getProject(), property, localName, overwrite );
	}

	private void checkValues()
	{
		if( this.property == null )
			throw new BuildException( "Must set the \"property\" attribute" );
		else if( this.packageName == null )
			throw new BuildException( "Must set the \"package\" attribute" );
	}
	
	////////////////////////////////////////////////////////////
	//////////////////////// Attributes ////////////////////////
	////////////////////////////////////////////////////////////
	public void setProperty( String property )
	{
		this.property = property;
	}
	
	public void setPackage( String packageName )
	{
		this.packageName = packageName;
	}
	
	public void setOverwrite( boolean overwrite )
	{
		this.overwrite = overwrite;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------

}
