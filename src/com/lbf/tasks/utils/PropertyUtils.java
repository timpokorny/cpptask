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
package com.lbf.tasks.utils;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

public class PropertyUtils
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
	 * Sets the given project property to the given value. If the property is already set, it will
	 * only be overwritten if the <code>overwrite</code> parameter is set. If it is set and
	 * overwrite is false, an exception will be thrown.
	 * 
	 * @param name The name of the property to set
	 * @param value The value to set the property to
	 * @param overwrite Whether or not the property should be overwritten if it is already set
	 */
	public static void setProjectProperty( Project project,
	                                       String name,
	                                       String value,
	                                       boolean overwrite )
	{
		if( project.getProperty(name) == null )
		{
			project.setProperty( name, value );
		}
		else if( overwrite )
		{
			project.setUserProperty( name, value );
		}
		else
		{
			throw new BuildException( "Property \""+name+"\" already set" );
		}
	}
}
