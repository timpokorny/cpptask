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
package org.portico.ant.tasks.utils;

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
