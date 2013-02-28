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
package com.lbf.tasks.cpptask.msvc;

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
	VC7("VS70COMNTOOLS"),
	VC8("VS80COMNTOOLS"),
	VC9("VS90COMNTOOLS"),
	VC10("VS100COMNTOOLS");

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String environmentVariable;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	private Version( String environmentVariable )
	{
		this.environmentVariable = environmentVariable;
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
