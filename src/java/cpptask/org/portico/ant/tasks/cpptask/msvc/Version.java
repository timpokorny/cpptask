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
	vc7("VS70COMNTOOLS",     "Visual Studio 2003", "Not Defined" ),
	vc8("VS80COMNTOOLS",     "Visual Studio 2005", "Not Defined" ),
	vc9("VS90COMNTOOLS",     "Visual Studio 2008", "Not Defined" ),
	vc10("VS100COMNTOOLS",   "Visual Studio 2010", "c:\\Program Files (x86)\\Microsoft Visual Studio 10.0\\VC\\vcvarsall.bat" ),
	vc11("VS110COMNTOOLS",   "Visual Studio 2012", "c:\\Program Files (x86)\\Microsoft Visual Studio 12.0\\VC\\vcvarsall.bat" ),
	vc12("VS120COMNTOOLS",   "Visual Studio 2013", "c:\\Program Files (x86)\\Microsoft Visual Studio 13.0\\VC\\vcvarsall.bat" ),
	vc14("VS140COMNTOOLS",   "Visual Studio 2015", "C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\VC\\vcvarsall.bat" ),
	vc14_1("VS150COMNTOOLS", "Visual Studio 2017", "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\{{PACKAGE}}\\VC\\Auxiliary\\Build\\vcvarsall.bat" ),
	vc14_2("VS160COMNTOOLS", "Visual Studio 2019", "C:\\Program Files (x86)\\Microsoft Visual Studio\\2019\\{{PACKAGE}}\\VC\\Auxiliary\\Build\\vcvarsall.bat" ),
	vc14_3("VS170COMNTOOLS", "Visual Studio 2022", "C:\\Program Files (x86)\\Microsoft Visual Studio\\2022\\{{PACKAGE}}\\VC\\Auxiliary\\Build\\vcvarsall.bat" );

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String environmentVariable;
	private String longName;
	private String defaultBatchPath;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	private Version( String environmentVariable, String longName, String defaultBatchPath )
	{
		this.environmentVariable = environmentVariable;
		this.longName = longName;
		this.defaultBatchPath = defaultBatchPath;
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

	public String getDefaultBatchPath()
	{
		return this.defaultBatchPath;
	}

	/**
	 * This method will check for the presence of the specified Visual Studio version.
	 * It will check to see if the relevant environment variable has been set, and if
	 * it has it will validate that location exists.
	 */
	public boolean isPresent()
	{
		try
		{
			return new File(getVcvarsallBatchFile()).exists();
		}
		catch( BuildException be )
		{
			return false;
		}
//		
//		File file = new File( getVcvarsallBatchFile() );
//		
//		// check for the presence of the appropriate environment variable
//		String vcDirectory = System.getenv( getToolsEnvironmentVariable() );
//		if( vcDirectory == null )
//			return false;
//		
//		// check to make sure the vcvarsall.bat file exists relative to the
//		// location specified in the environment variable
//		StringBuilder builder = new StringBuilder( vcDirectory );
//		builder.append( "\\..\\..\\VC\\vcvarsall.bat" );
//		File file = new File( vcDirectory+"\\..\\..\\VC\\vcvarsall.bat" );
//		return file.exists();
	}

	/**
	 * This method returns the path to the vcvarsall.bat batch file for the selected
	 * Visual Studio version.
	 * <p/>
	 * The file is located via one of a few different methods:
	 * <ul>
	 *   <li><b>Environment Variable</b>: First check the common <code>VSxxCOMNTOOLS</code>
	 *       environment variable for the path to the tools (where xx is replaced by the version
	 *       number). If not present, skip to next.</li>
	 *   <li><b>Default Path</b>: Next, check the default installation paths. This will cycle
	 *       through combinations for VS Professional, Community and Build Tools.</li>
	 * </ul>
	 * 
	 * This method will return the canonical path to the file, throwing a RuntimeException
	 * if it cannot be found.
	 */
	public String getVcvarsallBatchFile() throws BuildException
	{
		//
		// Option 1: Environment Variable Check
		//
		String vcDirectory = System.getenv( getToolsEnvironmentVariable() );
		if( vcDirectory != null )
		{
			// The variable is specified, which means someone has set something up. Assume
			// that the path is valid and try to load from it.
			File file = null;
			
			// the vcvarsall.bat file we're after isn't in the directory pointed to by
			// the enviornment variable - we have to resolve it (and its location relative
			// to the tools directory changed after VS 2015).
			if( this.ordinal() <= Version.vc14.ordinal() )
			{
				// older path
				StringBuilder builder = new StringBuilder( vcDirectory );
				builder.append( "\\..\\..\\VC\\vcvarsall.bat" );
				file = new File( vcDirectory+"\\..\\..\\VC\\vcvarsall.bat" );
			}
			else
			{
				// newer path
				StringBuilder builder = new StringBuilder( vcDirectory );
				builder.append( "\\..\\..\\VC\\vcvarsall.bat" );
				file = new File( vcDirectory+"\\..\\..\\VC\\Auxiliary\\Build\\vcvarsall.bat" );
			}

			try
			{
				return file.getCanonicalPath();			
			}
			catch( IOException ioex )
			{
				throw new BuildException( ioex.getMessage(), ioex );
			}
		}
		
		//
		// Option 2: Check Default Install Locations
		//
		// Path is basically the same, bug slightly different depending on the package installed
		String pathProfessional = getDefaultBatchPath().replace( "{{PACKAGE}}", "Professional" );
		String pathCommunity = getDefaultBatchPath().replace( "{{PACKAGE}}", "Community" );
		String pathBuildTools = getDefaultBatchPath().replace( "{{PACKAGE}}", "BuildTools" );
		String[] options = new String[] { pathProfessional, pathCommunity, pathBuildTools };
		
		// Loop through all options until we find one that exists (or we don't)
		File file = null;
		for( String option : options )
		{
			file = new File( option );
			if( file.exists() )
				break;
		}
		
		// We could not find it - generate exception
		if( file == null || file.exists() == false )
		{
			String expected = getDefaultBatchPath().replace( "{{PACKAGE}}",
			                                                 "{{Professional|Community|BuildTools}}" );
			throw new BuildException( "Did not find vcvarsall.bat at location: "+expected );
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
