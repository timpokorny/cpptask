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
package org.portico.ant.tasks.cpptask;

import org.apache.tools.ant.BuildException;
import org.portico.ant.tasks.cpptask.gcc.CompilerGCC;
import org.portico.ant.tasks.cpptask.msvc.CompilerMSVC;
import org.portico.ant.tasks.cpptask.msvc.Version;


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
	VC10,   // 2010
	VC11,   // 2012
	VC12,   // 2013
	VC14,   // 2015
	VC14_1, // 2017
	VC14_2; // 2019

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
				return new CompilerMSVC( Version.vc7 );
			case VC8:
				return new CompilerMSVC( Version.vc8 );
			case VC9:
				return new CompilerMSVC( Version.vc9 );
			case VC10:
				return new CompilerMSVC( Version.vc10 );
			case VC11:
				return new CompilerMSVC( Version.vc11 );
			case VC12:
				return new CompilerMSVC( Version.vc12 );
			case VC14:
				return new CompilerMSVC( Version.vc14 );
			case VC14_1:
				return new CompilerMSVC( Version.vc14_1 );
			case VC14_2:
				return new CompilerMSVC( Version.vc14_2 );
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
		else if( string.equalsIgnoreCase("vc11") )
			return CompilerType.VC11;
		else if( string.equalsIgnoreCase("vc12") )
			return CompilerType.VC12;
		else if( string.equalsIgnoreCase("vc14") )
			return CompilerType.VC14;
		else if( string.equalsIgnoreCase("vc14.1") || string.equalsIgnoreCase("vc14_1") )
			return CompilerType.VC14_1;
		else if( string.equalsIgnoreCase("vc14.2") || string.equalsIgnoreCase("vc14_2") )
			return CompilerType.VC14_2;
		else
			throw new BuildException( "Unknown compiler type: " + string );
	}
}
