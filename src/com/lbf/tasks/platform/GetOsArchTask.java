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

import org.apache.tools.ant.Task;

import com.lbf.tasks.utils.Arch;
import com.lbf.tasks.utils.PropertyUtils;

/**
 * Gets the {@link Arch} of the underyling operating system. We determine the architecture
 * by calling {@link Arch#getOsArch()}. The value is stored in property of the identified
 * name (defaults to "os.arch").
 */
public class GetOsArchTask extends Task
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
	public GetOsArchTask()
	{
		this.property = "os.arch";
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	
	public void execute()
	{
		PropertyUtils.setProjectProperty( getProject(),
		                                  property,
		                                  Arch.getOsArch().toString(),
		                                  false );
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
