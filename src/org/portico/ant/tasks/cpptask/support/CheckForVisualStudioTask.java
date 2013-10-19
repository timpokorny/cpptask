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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.portico.ant.tasks.cpptask.msvc.Version;

/**
 * This task will check for the presence of the specified Visual Studio version on the 
 * local computer. It will check the commonly known environment variable that is set with
 * each Visual Studio install and validate that the location pointed to exists.
 * <p/>
 * If the user has specified a result property to set once the check it complete, its value
 * will be set to "true" should the location exist, otherwise it is set to "false".
 * <p/>
 * If the <code>failIfAbsent</code> value is set and the location does not exists, the task
 * will cause the build to fail (defaults to false).
 * <p/>
 * <h3>Usage:</h3>
 * <p/>
 * <code>&lt;checkForVisualStudio version="vc10" resultProperty="vc10.present"/&gt;
 */
public class CheckForVisualStudioTask extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private Version version;
	private boolean failIfAbsent;
	private String resultProperty;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public CheckForVisualStudioTask()
	{
		this.version = null;
		this.failIfAbsent = false;
		this.resultProperty = null;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void execute()
	{
		// validate the provided settings
		if( version == null )
			throw new BuildException( "The \"version\" attribute is mandatory" );
		
		if( resultProperty != null && (getProject().getProperty("") != null) )
		{
			throw new BuildException( "The result property ["+resultProperty+
			                          "] has previously been set" );
		}
		
		// check to see if the visual studio versions if present
		boolean isPresent = version.isPresent();
		if( isPresent )
		{
			if( resultProperty != null )
				getProject().setProperty( resultProperty, "true" );
		}
		else
		{
			if( failIfAbsent )
				throw new BuildException( "Could not find "+version.getLongName() );

			if( resultProperty != null )
				getProject().setProperty( resultProperty, "false" );
		}
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

	public void setFailIfAbsent( boolean failIfAbsent )
	{
		this.failIfAbsent = failIfAbsent;
	}
	
	public void setResultProperty( String resultProperty )
	{
		this.resultProperty = resultProperty;
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
