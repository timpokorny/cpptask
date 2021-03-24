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
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.portico.ant.tasks.cpptask.CppTask.OutputArchAntEnum;
import org.portico.ant.tasks.utils.Arch;
import org.portico.ant.tasks.utils.Platform;

/**
 * Given a location to look in, and the desired platform architecture, this task
 * will verify that:
 * 
 *   * The directory contains a JDK installation
 *   * The target platform of the specified JDK matches that provided to the task
 *
 * If the directory does not contains a valid JDK, or one of the wrong architecture,
 * a `BuildException` will be thrown.
 *
 * ### Architecture Verification
 * To verify that the architecture of the JDK matches that requested, and to ensure that the
 * location points to a JDK (not a JRE), we employ two specific measures:
 *   
 *   1. Check for a `[JDK]/release` file and look inside it for the `OS_ARCH` setting 
 *   2. Run `[JDK]/bin/java -version` and parse the output for clues to the architecture
 * 
 * Example usage:
 * ```
 * <verifyJdk location="/path/to/JDK" architecture="amd64"/>
 * ```
 *  
 * Argument:
 *   * (mandatory) location: The location to search
 *   * (mandatory) architecture: The desired JDK arch (x86, amd64)
 */
public class VerifyJdkTask extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String location;
	private Arch architecture;
	private String javaVersion;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public VerifyJdkTask()
	{
		this.location = null;
		this.architecture = null;
		this.javaVersion = null;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	/**
	 * Check to see if the given location contains a JDK for the specified machine architecture.
	 * If we find a JDK and confirm that its target architecture matches that provided, this
	 * target will execute without error. If we can't verify the directory contains a JDK, or
	 * ensure that the arch matches that requested, a `BuildException` will be thrown to kill
	 * the build.
	 * 
	 * We employ a number of means to try and determine the architecture for a JDK:
	 * 
	 *  # Consult the `[JDK]/release` file, looking for an `OS_ARCH` property
	 *  # Run `[JDK]/bin/java -version` and inspect the output, looking for an indicator
	 * 
	 * If none of these yield a result, or we discover a JDK whose architecture we can confirm,
	 * and it does not match the target, a `BuildException` is thrown.
	 *
	 * @throws BuildException If we find platform details and they don't match the target
	 */
	public void execute() throws BuildException
	{
		// validate inputs
		if( location == null )
			throw new BuildException( "Mandatory attribute [location] missing from <verifyJdk>" );
		else if( architecture == null )
			throw new BuildException( "Mandatory attribute [architecture] missing from <verifyJdk>" );
		
		logVerbose( "Searching for "+architecture+" JDK in ["+location+"]" );
		
		// -----------------------------
		// Check for Win32/Win64 Problem
		// -----------------------------
		// On 32-bit Windows, Java is installed into `Program Files`, but on a 64-bit system,
		// that same application is installed into `Program Files (x86)`. This raises problems
		// with canonically specifying the location of a 32-bit JDK, as it depends not only on
		// the bitness of the JDK, but also that of the underlying OS. We run special checks
		// for this situation and automatically rename searched directories appropriately
		fixProgramFilesX86Problem();
		
		// ----------------------------
		// Verify that a JDK is present
		// ----------------------------
		if( containsJdk(location) == false && containsModernJdk(location) == false )
		{
			throw new BuildException( "Location does not contain a valid JDK ["+location+"]" );
		}
		
		if( providesJavaVersion( location, javaVersion ) == false )
		{
			throw new BuildException( "JDK does not support required Java version ["+location+"]" );
		}

		// ---------------------
		// Determin Architecture
		// ---------------------
		// We determine the architecture a number of ways:
		//
		// 1. The Release File
		// Many new JDKs include a file called "release" in them. This file contains information
		// about the JDK, such as ... the platform (YAY!). The release file is a standard
		// properties file, with the architecture specification under the "OS_ARCH" property.
		// We try this method first, as it is the simplest.
		//
		Arch jdkArchitecture = getArchFromReleaseFile( location );
		if( jdkArchitecture != null )
		{
			// we found the arch, verify it
			if( jdkArchitecture == this.architecture )
			{
				log( "Verified JDK ("+jdkArchitecture+"): "+location );
				return;
			}
			else
			{
				throw new BuildException( "JDK architecture ["+jdkArchitecture+
				                          "] does not match target ["+architecture+"]" );
			}
		}
		
		//
		// 2. Run "java -version"
		// If we can't find the release file we try to run the java executable up and ask it
		// about its version, picking the architecture from its output.
		//
		jdkArchitecture = getArchFromProcess( location );
		if( jdkArchitecture != null )
		{
			if( jdkArchitecture == this.architecture )
			{
				log( "Verified JDK ("+jdkArchitecture+"): "+location );
				return;
			}
			else
			{
				throw new BuildException( "JDK architecture ["+jdkArchitecture+
				                          "] does not match target ["+architecture+"]" );
			}
		}

		//
		// 3. Fail :(
		//
		logVerbose( "Could not determine JDK architecture :(" );
		throw new BuildException( "Found JDK but could not confirm its architecture (expected: "+
		                          architecture+"): "+location );
	}

	/**
	 * On a 32-bit system, applications are installed into `Program Files`. On a 64-bit system,
	 * those same applications are installed into `Program Files (x86)`. This causes problems when
	 * trying to specify the location to search for a 32-bit JDK, as the desired value will often
	 * depend on the underlying OS (which could change with the build).
	 * 
	 * This method detects those problems and adjusts the location property appropriately if it
	 * exhibits the problem.
	 */
	private void fixProgramFilesX86Problem()
	{
		// is the user searching for a Win32 JDK?
		if( Platform.getOsPlatform().isWindows32() && location.contains("(x86)") )
		{
			// remove the " (x86)" piece
			location.replace( " (x86)", "" );
		}
		else if( Platform.getOsPlatform().isWindows64() && architecture == Arch.x86 )
		{
			// We're on Win64 and looking for a 32-bit JDK, make sure we're not looking
			// in straight `Program Files`
			if( location.contains("Program Files") &&                 // looking in Prog Files
				(location.contains("Program Files (x86)") == false) ) // but no Prog Files (x86)
			{
				location.replace( "Program Files", "Program Files (x86)" );
			}
		}
	}
	
	/**
	 * Check the given location to see if it contains a JDK. To verify this, we check for
	 * the presence of a "jre" subdirectory. Not the perfect method, but this task was written
	 * to allow packaging of the correct JRE with installers, so a fair assumption for the
	 * primary use-case.
	 * 
	 * @return True if the location points to a valid JDK, false otherwise
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
	 * Check the given location to see if it contains a "modern" Java 9+ JDK. To verify this,
	 * we check for the jmods folder, which will be present if the JDK supports modules
	 * 
	 * @return true if the location points to a valid 9+ JDK, false otherwise
	 */
	private boolean containsModernJdk( String location )
	{
		File file = new File( location );
		if( file.exists() == false )
		{
			return false;
		}
		
		// jdk 9+ will have a jmods directory
		File jmodsDir = new File( file, "jmods" );
		if( jmodsDir.isDirectory() )
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Check the given location to see if it is able to meet the java version required,
	 * by reading the jdk release file and checking the version equals or exceeds the
	 * given required version
	 * 
	 * @return true if the jdk can provide the required java version, false otherwise
	 */
	private boolean providesJavaVersion( String location, String requiredVersionString )
	{
		// no requirement given, so assume user doesn't care
		if( requiredVersionString == null )
		{
			return true;
		}
		
		File jdk = new File( location );
		if ( jdk.exists() == false )
		{
			return false;
		}
		
		try
		{
			// load the properties file and extract the value of JAVA_VERSION
			Properties properties = new Properties();
			FileInputStream fis = new FileInputStream( location+"/release" );
			properties.load( fis );
			fis.close();
			
			// get actual numeric version number, e.g. 8 will stay as 8, but 1.8 becomes 8
			String[] splitString = requiredVersionString.split( Pattern.quote(".") );
			String majorVersion = splitString[ splitString.length - 1 ];
			int requiredVersion = Integer.parseInt( majorVersion );
			
			// get jdk version as above
			String jdkVersionString = properties.get("JAVA_VERSION").toString();
			String[] jdkSplit = jdkVersionString.replace("\"", "").split( Pattern.quote(".") );
			String jdkMajorVersion;
			// given as 1.major.minor
			if( jdkSplit[0].equals("1") )
			{
				jdkMajorVersion = jdkSplit[1];
			}
			else // major.minor
			{
				jdkMajorVersion = jdkSplit[0];
			}
			int jdkVersion = Integer.parseInt( jdkMajorVersion );
			
			// check if jdk can meet required version
			if( jdkVersion >= requiredVersion )
			{
				return true;
			}
			
		}
		catch( IOException e )
		{
			log( "Problem loading JDK/release file" );
		}
		
		return false;
	}

	/**
	 * Load the file `[JDK]/release` as a `Properties` instance and extract the `OS_ARCH` property.
	 * Convert that into an {@link Arch} and return it. If there is a probelm locating or reading
	 * the file, it doesn't contain an `OS_ARCH` property of the architecture is unknown, a problem
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
	 * Runs `[JDK]/bin/java -version` and scans the output for a hint towards the architecture.
	 * 
	 * When you run `java -version` you get a string like this:
	 * 
	 * ```
	 * Java version "1.7.0_07"
	 * Java(TM) SE Runtime Environment (build 1.7.0_07-b10)
	 * Java HotSpot(TM) 64-Bit Server VM (build 23.3-b01, mixed mode)
	 * ```
	 * 
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
			throw new BuildException( "Error determining JDK arch while running \"java -version\": "+
			                          ioex.getMessage(), ioex );
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * The location to search for a JDK with the specified arch
	 */
	public void setLocation( String location )
	{
		this.location = location;
	}

	/**
	 * The target platform type we'd like to use (should more 32-bit and 64-bit be available)
	 */
	public void setArch( OutputArchAntEnum arch )
	{
		this.architecture = Arch.valueOf( arch.getValue().toLowerCase() );
	}
	
	/**
	 * The version of Java we want the JDK to provide
	 */
	public void setRequiredVersion( String version )
	{
		this.javaVersion = version;
	}

	private void logVerbose( String message )
	{
		log( message, Project.MSG_VERBOSE );
	}
	
	public void logError( String message )
	{
		log( message, Project.MSG_ERR );
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
