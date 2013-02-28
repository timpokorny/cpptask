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
package com.lbf.tasks.platform;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class GetCompilerFromTargetNameTask extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String property = "vc.compiler";

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	public void execute()
	{
		// make sure we're actually inside a target
		if( this.getOwningTarget() == null )
			throw new BuildException( "Must call "+getTaskName()+" from within a target" );
		
		// get the target name
		String targetName = this.getOwningTarget().getName();
		String compilerName = "unknown";
		if( targetName.contains("vc6") )
			compilerName = "vc6";
		else if( targetName.contains("vc7") )
			compilerName = "vc7";
		else if( targetName.contains("vc8") )
			compilerName = "vc8";
		else if( targetName.contains("vc9") )
			compilerName = "vc9";
		else if( targetName.contains("vc10") )
			compilerName = "vc10";
		else if( targetName.contains("vc11") )
			compilerName = "vc11";
		else if( targetName.contains("vc12") ) // all hail the future!
			compilerName = "vc12";
		else if( targetName.contains("vc13") ) // all hail the future!
			compilerName = "vc13";
		else if( targetName.contains("vc14") ) // all hail the future!
			compilerName = "vc14";
		else if( targetName.contains("vc15") ) // all hail the future!
			compilerName = "vc15";
		else
			throw new BuildException( "Unable to find vcXX marker in target name: "+targetName );

		// set the name
		this.getProject().setProperty( property, compilerName );
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	public void setProperty( String property )
	{
		this.property = property;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
