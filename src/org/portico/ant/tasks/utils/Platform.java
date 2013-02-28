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
package org.portico.ant.tasks.utils;

import org.apache.tools.ant.BuildException;

/**
 * This enumeration represents a particular Platform (where a Platform is a combination of
 * operating system and underlying architecture).
 */
public enum Platform
{
	//----------------------------------------------------------
	//                    ENUMERATED VALUES
	//----------------------------------------------------------
	macosx,
	win32,
	win64,
	linux32,
	linux64;

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	public boolean isLinux()
	{
		return this == linux32 || this == linux64;
	}
	
	public boolean isLinux32()
	{
		return this == linux32;
	}
	
	public boolean isLinux64()
	{
		return this == linux64;
	}

	public boolean isWindows()
	{
		return this == win32 || this == win64;
	}
	
	public boolean isWindows32()
	{
		return this == win32;
	}
	
	public boolean isWindows64()
	{
		return this == win64;
	}

	public boolean isMac()
	{
		return this == macosx;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	public static Platform getOsPlatform() throws BuildException
	{
		Arch architecture = Arch.getOsArch();
		String osname = System.getProperty( "os.name" );
		if( osname.contains("indows") )
		{
			if( architecture == Arch.x86 )
				return Platform.win32;
			else
				return Platform.win64;
		}
		else if( osname.contains("Mac") )
		{
			return Platform.macosx;
		}
		else if( osname.contains("inux") )
		{
			if( architecture == Arch.x86 )
				return Platform.linux32;
			else
				return Platform.linux64;
		}
		else
		{
			throw new BuildException( "Unable to determine the Platform. Unknown OS: "+osname );
		}
	}
}
