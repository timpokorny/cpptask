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
package org.portico.ant.tasks.platform;

import org.apache.tools.ant.Task;
import org.portico.ant.tasks.utils.Arch;
import org.portico.ant.tasks.utils.PropertyUtils;


/**
 * This class gets the {@link Arch} of the underlying JVM and stores its name in the
 * provided property (defaults to "jvm.arch").
 */
public class GetJvmArchTask extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String property;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public GetJvmArchTask()
	{
		this.property = "jvm.arch";
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void execute()
	{
		PropertyUtils.setProjectProperty( getProject(),
		                                  property,
		                                  Arch.getJvmArch().toString(),
		                                  false );
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
