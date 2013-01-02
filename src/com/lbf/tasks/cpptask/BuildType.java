/*
 *   Copyright 2013 littlebluefroglabs.com
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

/**
 * The type of build that is being executed. Is it a debug build, a release build or both?
 */
public enum BuildType
{
	DEBUG,
	RELEASE,
	BOTH;

	/**
	 * @return True if this build includes building debug libraries (is DEBUG or BOTH)
	 */
	public boolean includesDebug()
	{
		return this == DEBUG || this == BOTH;
	}

	/**
	 * @return True if this build includes building release libraries (is RELEASE or BOTH).
	 */
	public boolean includesRelease()
	{
		return this == RELEASE || this == BOTH;
	}
}
