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
package org.portico.ant.tasks.cpptask.support.cpplocals;

/**
 * Holds values to set for properties if a debug build has been flagged.
 */
public class Debug extends Configuration
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

	public void setCargs( String cargs ) { this.compilerArgs = cargs; }
	public void setLargs( String largs ) { this.linkerArgs = largs; }
	public void setSymbols( String symbols ) { this.symbols = symbols; }

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
