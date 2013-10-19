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
package org.portico.ant.tasks.cpptask.support;

import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.portico.ant.tasks.cpptask.msvc.Version;

/**
 * Custom Ant Condition that will check for the presence of a Visual Studio version
 * on the local computer.
 */
public class IsVisualStudioPresentCondition implements Condition
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private Version version;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	public boolean eval()
	{
		if( this.version == null )
			throw new BuildException( "Version must be set" );
		
		return version.isPresent();
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Sets the compiler check for as part of this task
	 */
	public void setVersion( MsvcCompilerAntEnum version )
	{
		try
		{
			this.version = Version.valueOf( version.getValue() );
		}
		catch( Exception e )
		{
			throw new BuildException( e );
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////// Enumerated Types ///////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * The ant enumeration for specifying the valid compiler values.
	 */
	public static class MsvcCompilerAntEnum extends EnumeratedAttribute
	{
		public String[] getValues()
		{
			ArrayList<String> values = new ArrayList<String>();
			for( Version version : Version.values() )
				values.add( version.toString() );
			
			return values.toArray( new String[0] );
		}
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
