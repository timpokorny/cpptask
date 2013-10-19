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
package org.portico.ant.tasks.cpptask.msvc;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;

/**
 * This enumeration represents a specific Visual Studio version. It exists to both identify
 * which version of Visual Studio is in use and to provide some simple helper methods to
 * find important parts of all the supported Visual Studio install locations.
 */
public enum Version
{
	//----------------------------------------------------------
	//                    ENUMERATED VALUES
	//----------------------------------------------------------
	vc7("VS70COMNTOOLS", "Visual Studio 2003"),
	vc8("VS80COMNTOOLS", "Visual Studio 2005"),
	vc9("VS90COMNTOOLS", "Visual Studio 2008"),
	vc10("VS100COMNTOOLS", "Visual Studio 2010"),
	vc11("VS110COMNTOOLS", "Visual Studio 2012");

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String environmentVariable;
	private String longName;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	private Version( String environmentVariable, String longName )
	{
		this.environmentVariable = environmentVariable;
		this.longName = longName;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	/**
	 * @return The name of the environment variable associated with this Visual Studio version
	 */
	public String getToolsEnvironmentVariable()
	{
		return this.environmentVariable;
	}

	public String getLongName()
	{
		return this.longName;
	}

	/**
	 * This method will check for the presence of the specified Visual Studio version.
	 * It will check to see if the relevant environment variable has been set, and if
	 * it has it will validate that location exists.
	 */
	public boolean isPresent()
	{
		// check for the presence of the appropriate environment variable
		String vcDirectory = System.getenv( getToolsEnvironmentVariable() );
		if( vcDirectory == null )
			return false;
		
		// check to make sure the vcvarsall.bat file exists relative to the
		// location specified in the environment variable
		StringBuilder builder = new StringBuilder( vcDirectory );
		builder.append( "\\..\\..\\VC\\vcvarsall.bat" );
		File file = new File( vcDirectory+"\\..\\..\\VC\\vcvarsall.bat" );
		return file.exists();
	}

	/**
	 * This method returns the path to the vcvarsall.bat batch file for the selected
	 * Visual Studio version.
	 * <p/>
	 * This location is based off the location pointed to by an environment variable
	 * present on each system that has Visual Studio installed. The VSxxCOMNTOOLS variable
	 * (where xx is replaced by the version number) points to a location from which we
	 * can locate the desired batch file by jumping up and across a couple of directories.
	 * <p/>
	 * This method will return the canonical path to the file, throwing a RuntimeException
	 * if it cannot be found.
	 */
	public String getVcvarsallBatchFile() throws BuildException
	{
		// get the value of the environment variable
		String vcDirectory = System.getenv( getToolsEnvironmentVariable() );
		if( vcDirectory == null )
		{
			throw new RuntimeException( "Visual Studio tools missing: %"+
			                            environmentVariable+"% Required to locate install dir" );
		}
		
		// the tools directory isn't actually what we want - we want to jump up
		// and across into the Visual C++ area to get to the compiler/linker setup script
		StringBuilder builder = new StringBuilder( vcDirectory );
		builder.append( "\\..\\..\\VC\\vcvarsall.bat" );
		File file = new File( vcDirectory+"\\..\\..\\VC\\vcvarsall.bat" );

		// check to make sure we can actually have a reference to the file
		if( file.exists() == false )
		{
			throw new BuildException( "Did not find vcvarsall.bat at expected location: "+
			                          file.getAbsolutePath() );
		}

		// return the canonical path (won't have any of that icky relative stuff)
		try
		{
			return file.getCanonicalPath();			
		}
		catch( IOException ioex )
		{
			throw new BuildException( ioex.getMessage(), ioex );
		}
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
