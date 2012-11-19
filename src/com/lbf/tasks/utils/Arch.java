/*
 *   Copyright 2012 Calytrix Technologies
 *
 *   This file is part of cpptask.
 *
 *   NOTICE:  All information contained herein is, and remains
 *            the property of Calytrix Technologies Pty Ltd.
 *            The intellectual and technical concepts contained
 *            herein are proprietary to Calytrix Technologies Pty Ltd.
 *            Dissemination of this information or reproduction of
 *            this material is strictly forbidden unless prior written
 *            permission is obtained from Calytrix Technologies Pty Ltd.
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */
package com.lbf.tasks.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.tools.ant.BuildException;

/**
 * Enumeration to represent architecture we are building for.
 */
public enum Arch
{
	//----------------------------------------------------------
	//                    ENUMERATED VALUES
	//----------------------------------------------------------
	x86,
	amd64;

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	/**
	 * @return The architecture enumeration represented in the given string. Throws an exception
	 *         if the string is not known.
	 */
	public static Arch fromString( String arch ) throws BuildException
	{
		// 32-bit architectures
		if( arch.equals("x86") )
			return x86;
		else if( arch.equals("i386") )
			return x86;
		else if( arch.equals("i586") )
			return x86;
		else if( arch.equals("i686") )
			return x86;

		// 64-bit architectures
		else if( arch.equals("amd64") )
			return amd64;
		else if( arch.equals("x86_64") )
			return amd64;
		else if( arch.equals("x64") )
			return amd64;

		// at worst, default to 32-bit
		throw new BuildException( "Unknown architecture: "+arch );
	}

	/**
	 * Return the underlying architecture of the operating system. This can be a bit difficult
	 * to determine and the method used changes from OS to OS.
	 * <p/>
	 * For Mac OS X, we only support {@link Arch#amd64}.
	 * <p/>
	 * For Windows, we look to see if there is a "C:\Program Files (x86)" directory. If there is
	 * we must be running Windows 64-bit, otherwise we're on a 32-bit machine.
	 * <p/>
	 * For Linux, we use the result of running "uname -m".  
	 */
	public static Arch getOsArch()
	{
		// go through and set the appropriate values to true before we write the properties out
		String osname = System.getProperty( "os.name" );
		if( osname.contains("indows") )
		{
			// check the architecture
			File file = new File( "C:\\Program Files (x86)" );
			if( file.exists() )
				return Arch.amd64;
			else
				return Arch.x86;
		}
		else if( osname.contains("Mac") )
		{
			return Arch.amd64;
		}
		else if( osname.contains("inux") )
		{
			// check the architecture using uname -m
			try
			{
				// run "uname -m" which should return "i686" or "x86_64"
				Process process = Runtime.getRuntime().exec( "uname -m" );
				BufferedReader reader =
				    new BufferedReader( new InputStreamReader(process.getInputStream()) );
				return Arch.fromString( reader.readLine() );
			}
			catch( IOException ioex )
			{
				throw new BuildException( "Problem running uname to determine architecture", ioex );
			}
		}
		else
		{
			throw new BuildException( "Unable to determine operating system architecture" );
		}
	}
	
	/**
	 * @return The underlying architecture of the operating system. Actually this just
	 *         returns the underlying architecture of the JVM at this point, as it queries
	 *         the system property "os.arch" (which is the jvm architecture, not the os,
	 *         oddly enough).
	 */
	public static Arch getJvmArch()
	{
		String osarch = System.getProperty( "os.arch" );
		return fromString( osarch );
	}
}
