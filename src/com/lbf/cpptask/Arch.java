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
package com.lbf.cpptask;

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
	 * @return The underlying architecture of the operating system. Actually this just
	 *         returns the underlying architecture of the JVM at this point, as it queries
	 *         the system property "os.arch" (which is the jvm architecture, not the os,
	 *         oddly enough).
	 */
	public static Arch getOsArch()
	{
		String osarch = System.getProperty( "os.arch" );
		// 32-bit architectures
		if( osarch.equals("x86") )
			return x86;
		else if( osarch.equals("i386") )
			return x86;
		else if( osarch.equals("i586") )
			return x86;
		else if( osarch.equals("i686") )
			return x86;

		// 64-bit architectures
		else if( osarch.equals("amd64") )
			return amd64;
		else if( osarch.equals("x86_64") )
			return amd64;
		else if( osarch.equals("x64") )
			return amd64;

		// at worst, default to 32-bit
		return x86;
	}
}
