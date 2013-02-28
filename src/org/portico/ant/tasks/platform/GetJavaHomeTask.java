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
package org.portico.ant.tasks.platform;

import java.io.File;

import static org.apache.tools.ant.Project.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.portico.ant.tasks.utils.PropertyUtils;


/**
 * Gets the location of the current JDK and puts it into the identified property. If the
 * property is already set, we will inspect it first to ensure it points to a JDK. We
 * locate the JDK using the following process:
 * <ol>
 *   <li><b>Check the existing property</b>: If it is already set, validate that it contains a JDK
 *       and return. If it is set but doesn't contains a valid JDK, throw a BuildException.</li>
 *   <li><b>Check the JAVA_HOME env var</b>: If it is set, validate it points to a JDK and if so,
 *       set the property to this location. If it isn't set, skip to the next check.</li>
 *   <li><b>Check the java.home system property</b>: This points to the JRE we are currently
 *       using. All JDKs contain a JRE, which is what is used when we invoke JDK_HOME/bin/java.
 *       As such, we will whip the "/jre" off the end of the system property and see if the
 *       path points to a valid JDK. If not, throw an exception as we're all out of options.</li>
 *   <li><b></b>: </li>
 * </ol>
 * 
 * <p/>
 * <b>A Valid JDK</b>
 * <p/>
 * There are many ways to determine if a folder contains a JDK, but because we're simple,
 * we use a very basic measure. All JDKs contain within them a JRE (in a directory called
 * "jre"). As such, we determine a valid JDK as a directory containing a "jre" sub-directory.
 */
public class GetJavaHomeTask extends Task
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
	public GetJavaHomeTask()
	{
		this.property = "jdk.home";
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void execute()
	{
		/////////////////////////////////////////////////////////////////////////////////////
		// 1. Check the given property to see if it already points to a location           //
		//    If it does, try and validate that location, failing the build it isn't valid //
		/////////////////////////////////////////////////////////////////////////////////////
		String existingLocation = getProject().getProperty( property );
		if( existingLocation != null )
		{
			if( containsJdk(existingLocation) )
			{
				// it's already set and valid... nothing to do
				return;
			}
			else
			{
				// it is set, but the location is not valid, FAIL TIME
				throw new BuildException( "Property ["+property+"] does not point to a valid JDK."+
				                          " Clear it or set to point at JDK (not a JRE)" );
			}
		}

		/////////////////////////////////////////////////
		// 2. Check the JAVA_HOME environment variable //
		/////////////////////////////////////////////////
		String envvar = System.getenv( "JAVA_HOME" );
		if( envvar != null )
		{
			if( containsJdk(envvar) )
			{
				// set the property and bust out of here
				log( "Found JDK: JAVA_HOME environment variable points to valid JDK", MSG_VERBOSE );
				log( "  => Set property ["+property+"] to ["+envvar+"]", MSG_VERBOSE );
				PropertyUtils.setProjectProperty( getProject(), property, envvar, false );
				return;
			}
			else
			{
				// it is set, but the location is not valid, FAIL TIME
				throw new BuildException( "JAVA_HOME env-var set, but does not point to a valid "+
				                          "JDK. Clear it or set to point at JDK (not a JRE)" );
			}
		}

		////////////////////////////////////////////
		// 3. Check the java.home system property //
		////////////////////////////////////////////
		String javahome = System.getProperty( "java.home" );
		javahome = javahome.replace( "/jre", "" );
		if( containsJdk(javahome) )
		{
			// success! set the property
			log( "Found JDK: java.home system property points to valid JDK", MSG_VERBOSE );
			log( "  => Set property ["+property+"] to ["+javahome+"]", MSG_VERBOSE );
			PropertyUtils.setProjectProperty( getProject(), property, javahome, false );
			return;
		}
		else
		{
			// it is set, but the location is not valid, FAIL TIME
			throw new BuildException( "Could not locate valid JDK, is JAVA_HOME set?" );
		}
	}

	/**
	 * Return true if the given location contains a JDK. To determine whether the location
	 * is a JDK we check to see if the folder exists, and if it does, whether it contains a
	 * "jre" sub-directory.
	 */
	private boolean containsJdk( String location )
	{
		// if this is a JDK, it will have a JRE sub-directory
		File file = new File( location );
		if( file.exists() == false )
			return false;
		
		for( String subpath : file.list() )
		{
			if( subpath.endsWith("jre") )
				return true;
		}

		// not a JDK sadly :(
		return false;
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
