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
package com.lbf.tasks.cpptask;

import org.apache.tools.ant.BuildException;

public interface Compiler
{
	/**
	 * Run the compilation process using the data contained in the given task.
	 * Should throw a BuildException if there is a problem.
	 */
	public void runCompiler( BuildConfiguration configuration ) throws BuildException;
}
