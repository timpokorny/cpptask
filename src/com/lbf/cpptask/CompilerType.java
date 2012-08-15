/*
 *   Copyright 2007 littlebluefroglabs.com
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
package com.lbf.cpptask;

import org.apache.tools.ant.BuildException;

import com.lbf.cpptask.gcc.CompilerGCC;
import com.lbf.cpptask.msvc.CompilerMSVC;

public enum CompilerType
{
	GCC,
	GPP,
	MSVC;

	/**
	 * Create a new instance of the identifeid compiler and return it.
	 */
	public static Compiler newInstance( CompilerType type )
	{
		switch( type )
		{
			case GCC:
				return new CompilerGCC("gcc");
			case GPP:
				return new CompilerGCC("g++");
			case MSVC:
				return new CompilerMSVC();
			default:
				throw new BuildException( "unsupported compiler type: " + type );
		}
	}

	/**
	 * Gets a compiler type from a string. This allows the strings to have names an enum value
	 * can't (such as "g++")
	 */
	public static CompilerType fromString( String string )
	{
		if( string.equalsIgnoreCase("gcc") )
			return CompilerType.GCC;
		else if( string.equalsIgnoreCase("g++") )
			return CompilerType.GPP;
		else if( string.equalsIgnoreCase("msvc") )
			return CompilerType.MSVC;
		else
			throw new BuildException( "Unknown compiler type: " + string );
	}
}
