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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.portico.ant.tasks.cpptask.CppTask.OutputArchAntEnum;
import org.portico.ant.tasks.utils.Arch;
import org.portico.ant.tasks.utils.Platform;
import org.portico.ant.tasks.utils.PropertyUtils;


/**
 * This method attempts to locate a valid JDK for the identified architecture. If it can find
 * one it will load the location into the identified property. To find a valid JDK it goes through
 * the following steps:
 * <ol>
 *   <li><b>Check provided property</b>: Checks to see if the property already has a value.
 *       If so, validate* it or move onto the next.</li>
 *   <li><b>Check the JAVA_HOME env var</b>: If this is set, try and validate it, but if it is
 *       not valid, swallow the exception and move onto the next (it could be set for other
 *       reasons - such as running the build with one Java type but targeting another).</li>
 *   <li><b>Check the java.home system property</b>: The same as JAVA_HOME. Check, but don't fail
 *       if it is not valid, just skip to the next check.</li>
 *   <li><b>Check the provided fallback</b>: If this is set, validate as per normal. If this isn't
 *       set, throw a BuildException as we have now run out of options.</li>
 * </ol>
 * 
 * *Validation: To validate a JDK we ensure that the location points to an actual JDK (not a JRE),
 * and that the JDK is of the specified architecture (this is skipped if no architecture is
 * specified). If validation fails, a BuildException is thrown. We only validate a location if we
 * find one, so builds will only fail is someone specifies a location and it is invalid (not if
 * they don't provide a location).
 * <p/>
 * Example usage:
 * &lt;getJdk property="my.property" architecture="amd64" fallback="../path"/&gt;
 * <p/>
 * All values are optional. Defaults are:
 * <ul>
 *   <li><b>property</b>: (optional) defaults to "jdk.home"</li>
 *   <li><b>architecture</b>: null (no check made)</li>
 *   <li><b>fallback</b>: (optional) location to use as last resort - maybe a platform default</li>
 * </ul>
 */
@Deprecated
public class GetJdkTask extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String property;
	private Arch architecture;
	private String fallback;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public GetJdkTask()
	{
		this.property = "jdk.home";
		this.architecture = null;
		this.fallback = System.getenv( "JAVA_HOME" );
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void execute()
	{
		logVerbose( "Searching for a JDK: " );
		logVerbose( "  => Property to use/set       = "+property );
		logVerbose( "  => Desired JVM Architecture  = "+architecture );
		logVerbose( "  => Fallback/default location = "+fallback );

		/////////////////////////////////////////////////////////////////////////////////////
		// 1. Check the given property to see if it already points to a location           //
		//    If it does, try and validate that location, failing the build it isn't valid //
		/////////////////////////////////////////////////////////////////////////////////////
		String existingProperty = getProject().getProperty( property );
		if( existingProperty != null )
		{
			if( containsJdk(existingProperty) == false )
			{
				throw new BuildException( "Property ["+property+"] does not point to a JDK. "+
				                          "Clear it or set to point at JDK (not a JRE)" );
			}
			else if( containsJdkForArchitecture(existingProperty,architecture) == false )
			{
				throw new BuildException( "Property ["+property+"] points to a JDK of wrong architecture. "+
				                          "Desired architecture is ["+architecture+"]." );
			}
			else
			{
				// if it is set, and it points to an appropriate JDK, we're all done
				return;
			}
		}

		////////////////////////////////////////////
		// 2. Check the java.home system property // 
		////////////////////////////////////////////
		// There is no existing location in the provided proprety, check the java.home
		// system property to see if it contains a valid JDK. It should always point to
		// a valid JRE, let's just hope the JRE lives inside a valid JDK. If not, skip.
		String sysprop = System.getProperty( "java.home" );
		sysprop = sysprop.replace( "/jre", "" );
		if( containsJdk(sysprop) )
		{
			if( containsJdkForArchitecture(sysprop,architecture) )
			{
				// we're all good!
				logVerbose( "Found JDK. System property java.home points to valid JDK." );
				logVerbose( "  => Set property ["+property+"] to ["+sysprop+"]" );
				PropertyUtils.setProjectProperty( getProject(), property, sysprop, false );
				logLocated( sysprop );
				return;
			}
			else
			{
				logVerbose( "java.home system property points to a JDK, but not of the "+
				            "desired architecture ["+architecture+"]. Skipping." );
			}
		}
		else
		{
			logVerbose( "java.home system proprty points to a standalone JRE. "+
			            "A full JDK is required. Skipping" );
		}
		
		////////////////////////////////////////////////////////////
		// 3. There is no existing location, try and locate a JDK //
		////////////////////////////////////////////////////////////
		// This won't necessarily always be set, and even if it is, as with the java.home system
		// property, the user may be using one Java install to build, but targeting another, so
		// if it does exist, but isn't valid, just log and move on.
		String envvar = System.getenv( "JAVA_HOME" );
		if( envvar != null )
		{
			// check to make sure it points to a JDK
			if( containsJdk(envvar) == false )
			{
				// fail the build, because it at least has to point to a JDK FFS
				// this is likely a misconfiguration, with the user THINKING its set, but it isn't
				throw new BuildException( "JAVA_HOME environment variable set, but doesn't actually"+
				                          "point to a JDK of any kind. Fix plz. kthxbai." );
			}
			
			if( containsJdkForArchitecture(envvar,architecture) == false )
			{
				// DO NOT FAIL THE BUILD! It points to a JDK, but not one we can use. Just log
				// the message to inform them and move on to the fallback. They might be running
				// in a different JDK to the one they want to get a reference to
				logVerbose( "JAVA_HOME environment variable points to a JDK, but not of the "+
				            "desired architecture ["+architecture+"]: Skipping" );
			}
			else
			{
				// we found one!
				// Skipping this for now. As it turns out, in common use I'm finding that
				// for Portico we want to hard-specify a location via the fallback. This
				// lets us ensure htat things are consistent across platforms. For Ant to
				// load up properly, JAVA_HOME must be set, so we should ALWAYS get a
				// result from here, although we typically really want to fall through to
				// the fallback so we can control what the reference points to. Better
				// thought about the name/purpose of this task is needed.

				/*
				logVerbose( "Found JDK. Environment variable JAVA_HOME points to valid JDK." );
				logVerbose( "  => Set property ["+property+"] to ["+envvar+"]" );
				PropertyUtils.setProjectProperty( getProject(), property, envvar, false );
				logLocated( envvar );
				return;
				*/
			}
		}
		else
		{
			logVerbose( "The JAVA_HOME environment variable not set, checking fallback location" );
		}

		////////////////////////////////////////////////
		// 4. We can't locate a JDK, use the fallback //
		////////////////////////////////////////////////
		// Our last chance. If there is not JDK here, or it isn't what we want we fail the build
		if( fallback != null )
		{
			// If the fallback location includes the string "(x86)" and we're on Windows 32-bit,
			// the user has probably set path as if we were on a Windows 64-bit machine. That is,
			// they've presumed that the location is Program Files (x86) which is where 32-bit
			// stuff is on a 64-bit machine, but not on a 32-bit machine. Try and be graceful
			// in this situation and change it for them
			if( Platform.getOsPlatform().isWindows32() && fallback.contains("(x86)") )
			{
				log( "Detected Win64 specification location on Win32 OS: [Program Files (x86)]" );
				fallback = fallback.replace( " (x86)", "" );
				log( "Converting to [Program Files]: "+fallback );
			}
			
			if( containsJdk(fallback) == false )
			{
				throw new BuildException( "Fallback location does not point to a JDK. "+
				                          "No JDK installed on system at location: "+fallback );
			}
			else if( containsJdkForArchitecture(fallback,architecture) == false )
			{
				throw new BuildException( "Fallback location points to a JDK of wrong architecture. "+
				                          "Desired architecture is ["+architecture+"], location: "+
				                          fallback );
			}
			else
			{
				// if it is set, and it points to an appropriate JDK, we're all done
				getProject().setNewProperty( property, fallback );
				logVerbose( "JDK located using fallback location: "+fallback );
				logLocated( fallback );
				return;
			}
		}
		else
		{
			// we haven't found a JDK and the fallback is null. we're done.
			logError( "Cannot locate a JDK in any searched location. Try the following:" );
			logError( "  => Set the Ant property ["+property+"] to point at a valid JDK" );
			logError( "  => Set the JAVA_HOME environment variable to point at a valid JDK" );
			logError( "  => Ensure the provided fallback location points to a valid JDK" );
			throw new BuildException( "Cannot locate a JDK in any searched locations" );
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

	/**
	 * Returns true if the given location contains a JDK that is of the desired architecture.
	 * If the given directory contains a JDK and the given architecture is null, true will be
	 * returned (null is equal to saying "any architecture").
	 * 
	 * @param location The location to look for a JDK in
	 * @param architecture The desired platform architecture, or null if any is acceptable
	 */
	private boolean containsJdkForArchitecture( String location, Arch architecture )
	{
		// do we even have a valid JDK in the first place?
		if( containsJdk(location) == false )
			return false;
		
		// don't even bother checking unless they've provided a desired architecture
		if( architecture == null )
			return true;
		
		// we determine the architecture a number of ways:
		//
		// 1. The Release File
		// Many new example JDKs include a file called "release" in them, which in turn contains
		// information about the JDK, such as ... the platform. This information is included in
		// the OS_ARCH property contained in that file. We try to use it first, as it is more
		// definitive
		//
		// 2. Run "java -version" and pick it up off the output
		
		// figure the architecure out
		Arch actualArchitecture = getArchFromReleaseFile( location );
		if( actualArchitecture == null )
		{
			// no release file - try asking it
			actualArchitecture = getArchFromProcess( location );
		}

		// check to see if the architecture is what we want
		if( actualArchitecture == architecture )
		{
			return true;
		}
		else
		{
			logVerbose( "JDK architecture does not match that requested (found="+
			            actualArchitecture+", expected="+architecture+")" );
			return false;
		}			
	}

	/**
	 * Load the file "location/release" as a Properties file and extract the OS_ARCH property.
	 * Convert that into an {@link Arch} and return it. If there is a probelm locating or reading
	 * the file, it doesn't contain an OS_ARCH property of the architecture is unknown, a problem
	 * is logged and null is returned.
	 */
	private Arch getArchFromReleaseFile( String location )
	{
		String archString = null;
		try
		{
			// load the properties file and extract the value of OS_ARCH
			Properties properties = new Properties();
			FileInputStream fis = new FileInputStream( location+"/release" );
			properties.load( fis );
			fis.close();
			
			// get the architecture information
			archString = properties.get("OS_ARCH").toString();
			if( archString == null )
				return null;
			
			archString = archString.replace( "\"", "" );
			return Arch.fromString( archString );
		}
		catch( IOException ioex )
		{
			log( "Problem loading JDK/release file, trying another method to identify platform",
			     Project.MSG_VERBOSE );

			return null;
		}
		catch( BuildException be )
		{
			log( "Unknown architecture in the release file ["+archString+"], trying other means. ",
			     Project.MSG_WARN );

			return null;
		}
	}

	/**
	 * Runs "java -version" for the provided JDK and gets the version information from there.
	 * <p/>
	 * When you run "java -version" you get a string like this:
	 * <pre>
	 * Java version "1.7.0_07"
	 * Java(TM) SE Runtime Environment (build 1.7.0_07-b10)
	 * Java HotSpot(TM) 64-Bit Server VM (build 23.3-b01, mixed mode)
	 * </pre>
	 * If it is a 64-bit JVM, the 64-Bit string will be included. In 32-bit cases, it won't.
	 * Just to be safe, run "java -version" and check all lines for the presence of "64-bit"
	 */
	private Arch getArchFromProcess( String location ) throws BuildException
	{
		try
		{
			Process process = Runtime.getRuntime().exec( location+"/bin/java -version" );
			BufferedReader reader = new BufferedReader( new InputStreamReader(process.getErrorStream()) );

			// inspect each line to see if we have what we need
			while( true )
			{
				String line = reader.readLine();
				if( line == null )
				{
					// no lines left - must be 32-bit
					return Arch.x86;
				}

				line = line.toLowerCase();
				if( line.contains( "64-bit" ) )
					return Arch.amd64;
			}
		}
		catch( IOException ioex )
		{
			throw new BuildException( "Error determining JDK architecture while running \"java -version\": "+
			                          ioex.getMessage(), ioex );
		}
	}

	private void logVerbose( String message )
	{
		log( message, Project.MSG_VERBOSE );
	}
	
	public void logError( String message )
	{
		log( message, Project.MSG_ERR );
	}

	/**
	 * Prints out a log message at INFO level to let the user know that the JDK they
	 * requested has been located.
	 */
	public void logLocated( String location )
	{
		String archString = "(x86)  ";
		if( architecture == Arch.amd64 )
			archString = "(amd64)";

		log( "Located JDK "+archString+": "+property+" = "+location );
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * The property to set with the location of the JDK home.
	 */
	public void setProperty( String property )
	{
		this.property = property;
	}

	/**
	 * The target platform type we'd like to use (should more 32-bit and 64-bit be available)
	 */
	public void setArch( OutputArchAntEnum arch )
	{
		this.architecture = Arch.valueOf( arch.getValue().toLowerCase() );
	}

	/**
	 * The location to use if we cannot determine the appropriate value. This is typically
	 * set to a platform default.
	 */
	public void setFallback( String fallback )
	{
		this.fallback = fallback;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
