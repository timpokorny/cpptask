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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

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
 * <ul>
 *   <li>The directory contains a JDK installation</li>
 *   <li>The target platform of the specified JDK matches that provided to the task</li>
 *   <li>THe target version (if specified) matches the JDK version</li>
 * </ul>
 *
 * If the directory does not contains a valid JDK, or the configuration properties cannot
 * be verified, a {@link BuildException} will be thrown.
 *
 * 
 * Example usage:
 * <pre>
 * <verifyJdk location="/path/to/JDK" architecture="amd64" requiredVersion="11"/>
 * </pre>
 * 
 * Arguments:
 * <ul>
 *   <li>(mandatory) location: The location to search</li>
 *   <li>(optional)  architecture: The desired JDK arch (x86, amd64) - defaults to amd64</li>
 *   <li>(optional)  requiredVersion: The required major version of Java - defaults to "any"</li> 
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
	private String requiredVersion;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public VerifyJdkTask()
	{
		this.location = null;
		this.architecture = Arch.amd64; // default to 64-bit
		this.requiredVersion = "any";   // default to "any", which will cause check to skip
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	/**
	 * Check to see if the configured location contains a valid JDK according to our configured
	 * parameters. If the JDK exists, is the required architecture and (if specified) the required
	 * version, return happily.
	 * 
	 * If the JDK doesn't exist, isn't the required architecture or the required version, then 
	 * throw a BuildException and exit.
	 * 
	 * To verify a JDK we employ one of two approaches:
	 * 
	 *  - Check for a `[JDK]/release` file, and if present look at values inside it
	 *  - Run `[JDK]/bin/java -version` and inspect the output, looking for an indicator
	 * 
	 * If none of these yield a result, a `BuildException` is thrown.
	 *
	 * @throws BuildException If we find platform details and they don't match the target
	 */
	public void execute() throws BuildException
	{
		// validate inputs
		if( location == null )
			throw new BuildException( "Mandatory attribute [location] missing from <verifyJdk>" );
		
		log( "Scanning for JDK {arch="+architecture+",ver="+requiredVersion+"}: "+location );
		
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
		if( containsJdk(location) == false )
			throw new BuildException( "(Verify JDK) Location not identifiable as JDK ["+location+"]" );

		// -----------------------------------
		// Check {JDK}/release file for values
		// -----------------------------------
		// does the release file even exist?
		File jdkLocation = new File( this.location );
		File releaseFile = new File( jdkLocation, "release" );
		if( releaseFile.exists() == false )
		{
			throw new BuildException( "(Verify JDK) Could not find release file needed to verify: "+
			                          jdkLocation.getAbsolutePath() );
		}

		// get the properties from the release
		Properties releaseProperties = getPropertiesFromReleaseFile( releaseFile );
		if( releaseProperties == null )
		{
			throw new BuildException( "(Verify JDK) Could not read release file needed to verify: "+
			                          releaseFile.getAbsolutePath() );
		}

		//---------------------------------------------------
		// Check the ARCHITECTURE specified in the properties
		//---------------------------------------------------
		logVerbose( "Found {JDK}/release file; interrogating..." );
		Arch jdkArchitecture = getArchFromProperties( releaseProperties );
		if( jdkArchitecture == null )
		{
			throw new BuildException( "(Verify JDK) Could not verify arch; OS_ARCH missing ("+
			                          jdkLocation+")" );
		}

		if( jdkArchitecture != this.architecture )
		{
			throw new BuildException( "(Verify JDK) Architecture ["+jdkArchitecture+
			                          "] does not match target ["+architecture+"] ("+jdkLocation+")" );
		}
		else
		{
			logVerbose( "Verified JDK Arch (found="+jdkArchitecture+",required="+architecture+")" );
		}
		
		//---------------------------------------------------
		// Check the java VERSION specified in the properties
		//---------------------------------------------------
		String jdkVersion = releaseProperties.getProperty( "JAVA_VERSION" );
		// strip the " from the value
		if( jdkVersion != null )
			jdkVersion = jdkVersion.replace( "\"", "" );
		if( isVersionMatching(jdkVersion,this.requiredVersion) == false )
		{
			throw new BuildException( "(Verify JDK) Version ["+jdkVersion+"] does not match target ["+
			                          requiredVersion+"] ("+jdkLocation+")" );
		}
		else
		{
			logVerbose( "Verified JDK Version (found="+jdkVersion+",required="+requiredVersion+")" );
		}
		
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
	 * Check the given location to see if it contains a JDK. Starting with Java 9, there is no
	 * longer a JRE/JDK distinction, so the verification process will vary depending on version.
	 * 
	 * To verify this, we first check for the presence of a "jre" subdirectory. Not the perfect
	 * method, but it worked for Java 8 or earlier. 
	 * 
	 * If there is no jre directory, we check to see whether it is a Java 9+ JDK by looking for
	 * the jmods folder.
	 * 
	 * @return true if the location points to a valid JDK, false otherwise
	 */
	private boolean containsJdk( String location )
	{
		// Check for Java 8 and earlier style JDK
		File file = new File( location, "jre" );
		if( file.exists() )
			return true;
		
		// Check for the javac executable
		file = new File( location, "bin/javac" );
		if( Platform.getOsPlatform().isWindows() )
			file = new File( location, "bin/javac.exe" );

		if( file.exists() )
			return true;
		
		// Did not pass any checks... not a JDK
		return false;
	}
	
	/**
	 * Load the given properties file and return it. Returns <code>null</code> if the file cannot
	 * be read.
	 * 
	 * @param releaseFile The file to load as properties
	 * @return The file, as a Properties object, or null if the file cannot be read
	 */
	private Properties getPropertiesFromReleaseFile( File releaseFile )
	{
		try
		{
			// load the properties file and extract the value of OS_ARCH
			Properties properties = new Properties();
			FileInputStream fis = new FileInputStream( releaseFile );
			properties.load( fis );
			fis.close();
			return properties;
		}
		catch( IOException ioex )
		{
			return null;
		}
	}

	/**
	 * Look in the given properties for the value of <code>OS_ARCH</code> and if present, return it
	 * as an instance of {@link Arch}. If the property is not found, <code>null</code> is returned.
	 * If the property <i>is</i> found, but is not recognized, a {@link BuildException} is thrown.
	 * 
	 * @param properties The properties to search for architecture information in.
	 * @return An instance of {@link Arch} is the value is found and valid, or null if the
	 *         properties do not contain the key
	 */
	private Arch getArchFromProperties( Properties properties )
	{
		if( properties.containsKey("OS_ARCH") )
		{
			String archString = properties.get("OS_ARCH").toString();
			archString = archString.replace( "\"", "" );
			return Arch.fromString( archString );
		}
		else
		{
			return null;
		}
	}

	/**
	 * Check to see whether the java version we found is a match for what we want
	 * 
	 * @param found The version from the properties file
	 * @param required The version we require
	 * @return True if they are a match (equivalent), false if they are not
	 */
	private boolean isVersionMatching( String found, String required )
	{
		// make sure the value actually exists in the JDK
		if( found == null )
			return false;

		// if we will take anything, don't even bother checking
		if( requiredVersion.equalsIgnoreCase("any") )
			return true;

		// we need to check - if it's an earlier JDK, it'll start with "1.x"
		if( found.startsWith("1.") )
		{
			return found.startsWith( "1."+this.requiredVersion );
		}
		else
		{
			return found.startsWith( this.requiredVersion );
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
	 * The version of Java we want the JDK to provide. This should be the major version
	 * number, e.g. 8, 9, 10, 11, 17, 21, ...
	 */
	public void setRequiredVersion( String version )
	{
		this.requiredVersion = version;
	}

	private void logVerbose( String message )
	{
		log( message, Project.MSG_VERBOSE );
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
