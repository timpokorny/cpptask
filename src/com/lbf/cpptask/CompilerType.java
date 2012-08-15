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
import com.lbf.cpptask.msvc.Version;

public enum CompilerType
{
	//----------------------------------------------------------
	//                    ENUMERATED VALUES
	//----------------------------------------------------------
	GCC,
	GPP,
	VC7,
	VC8,
	VC9,
	VC10;

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	@Override
	public String toString()
	{
		if( this == GPP )
			return "g++";
		else
			return super.toString();
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
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
			case VC7:
				return new CompilerMSVC( Version.VC7 );
			case VC8:
				return new CompilerMSVC( Version.VC8 );
			case VC9:
				return new CompilerMSVC( Version.VC9 );
			case VC10:
				return new CompilerMSVC( Version.VC10 );
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
		else if( string.equalsIgnoreCase("vc7") )
			return CompilerType.VC7;
		else if( string.equalsIgnoreCase("vc8") )
			return CompilerType.VC8;
		else if( string.equalsIgnoreCase("vc9") )
			return CompilerType.VC9;
		else if( string.equalsIgnoreCase("vc10") )
			return CompilerType.VC10;
		else
			throw new BuildException( "Unknown compiler type: " + string );
	}
}
