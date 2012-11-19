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
package com.lbf.tasks.platform;

import java.util.HashMap;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.lbf.tasks.utils.Platform;

/**
 * The OS Platform task will look at the underyling operating system platform (NOT the version
 * of the JDK that Ant is running on) and will set system properties appropriately to mark what
 * we have. The following properties will be set appropriately depending on the platform:
 * <ul>
 *   <li>${platform} : One of "macosx, win32, win64, linux32, linux64"</li>
 *   <li>${platform.macosx} : "true" if we are running on Mac OS X</li>
 *   <li>${platform.windows} : "true" if we are running on Windows in general</li>
 *   <li>${platform.win32} : "true" if we are running on 32-bit Windows</li>
 *   <li>${platform.win64} : "true" if we are running on 64-bit Windows</li>
 *   <li>${platform.linux} : "true" if we are running on Linux in general</li>
 *   <li>${platform.linux32} : "true" if we are running on 32-bit Linux</li>
 *   <li>${platform.linux64} : "true" if we are running on 64-bit Linux</li>
 * </ul>
 */
public class GetOsPlatformTask extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private HashMap<String,String> properties;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public GetOsPlatformTask()
	{
		this.properties = new HashMap<String,String>();
		this.properties.put( "platform", System.getProperty("os.name") );
		this.properties.put( "macosx", "unset" );
		this.properties.put( "windows", "unset" );
		this.properties.put( "win32", "unset" );
		this.properties.put( "win64", "unset" );
		this.properties.put( "linux", "unset" );
		this.properties.put( "linux32", "unset" );
		this.properties.put( "linux64", "unset" );		
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void execute()
	{
		Platform platform = Platform.getOsPlatform();
		switch( platform )
		{
			case win32:
				this.properties.put( "windows", "true" );
				this.properties.put( "win32", "true" );
				break;
			case win64:
				this.properties.put( "windows", "true" );
				this.properties.put( "win64", "true" );
				break;
			case macosx:
				this.properties.put( "macosx", "true" );
				this.properties.put( "platform", "macosx" );
				break;
			case linux32:
				this.properties.put( "linux32", "true" );
				this.properties.put( "platform", "linux32" );
				break;
			case linux64:
				this.properties.put( "linux64", "true" );
				this.properties.put( "platform", "linux64" );
				break;
		}
		
		// complete, now set the properties
		log( "Operating System platform is: "+properties.get("platform"), Project.MSG_INFO );
		
		log( "Determined platform values, setting the following properties:", Project.MSG_VERBOSE );
		log( "   platform: "+ properties.get("platform"), Project.MSG_VERBOSE );
		log( "     macosx: "+ properties.get("macosx"), Project.MSG_VERBOSE );
		log( "    windows: "+ properties.get("windows"), Project.MSG_VERBOSE );
		log( "      win32: "+ properties.get("win32"), Project.MSG_VERBOSE );
		log( "      win64: "+ properties.get("win64"), Project.MSG_VERBOSE );
		log( "      linux: "+ properties.get("linux"), Project.MSG_VERBOSE );
		log( "    linux32: "+ properties.get("linux32"), Project.MSG_VERBOSE );
		log( "    linux64: "+ properties.get("linux64"), Project.MSG_VERBOSE );
		
		getProject().setUserProperty( "platform", properties.get("platform") );
		setProperty( "platform.macosx", properties.get("macosx") );
		setProperty( "platform.windows", properties.get("windows") );
		setProperty( "platform.win32", properties.get("win32") );
		setProperty( "platform.win64", properties.get("win64") );
		setProperty( "platform.linux", properties.get("linux") );
		setProperty( "platform.linux32", properties.get("linux32") );
		setProperty( "platform.linux64", properties.get("linux64") );
	}

	/**
	 * Sets the property if the given value is "true". If not, no action is taken.
	 */
	private void setProperty( String property, String value )
	{
		if( value != null && value.equals("true") )
			getProject().setUserProperty( property, value );
	}
	
	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
