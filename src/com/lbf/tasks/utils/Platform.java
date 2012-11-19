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

import org.apache.tools.ant.BuildException;

/**
 * This enumeration represents a particular Platform (where a Platform is a combination of
 * operating system and underlying architecture).
 */
public enum Platform
{
	//----------------------------------------------------------
	//                    ENUMERATED VALUES
	//----------------------------------------------------------
	macosx,
	win32,
	win64,
	linux32,
	linux64;

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	public boolean isLinux()
	{
		return this == linux32 || this == linux64;
	}

	public boolean isWindows()
	{
		return this == win32 || this == win64;
	}
	
	public boolean isMac()
	{
		return this == macosx;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	public static Platform getOsPlatform() throws BuildException
	{
		Arch architecture = Arch.getOsArch();
		String osname = System.getProperty( "os.name" );
		if( osname.contains("indows") )
		{
			if( architecture == Arch.x86 )
				return Platform.win32;
			else
				return Platform.win64;
		}
		else if( osname.contains("Mac") )
		{
			return Platform.macosx;
		}
		else if( osname.contains("inux") )
		{
			if( architecture == Arch.x86 )
				return Platform.linux32;
			else
				return Platform.linux64;
		}
		else
		{
			throw new BuildException( "Unable to determine the Platform. Unknown OS: "+osname );
		}
	}
}
