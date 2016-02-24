/*
 *   Copyright 2016 The Portico Project
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
package org.portico.ant.tasks.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CryptoUtils
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

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

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	
	/**
	 * Helper method for converting an input stream to a byte array.
	 * 
	 * @param is Input stream to convert.
	 * @return Byte array representation of the input stream.
	 */
	public static byte[] toByteArray( InputStream is )
		throws IOException
	{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int read;
		byte[] data = new byte[16384];
		while ( (read = is.read(data, 0, data.length)) != -1 )
		{
			buffer.write( data, 0, read );
		}
		return buffer.toByteArray();
	}
	
	/**
	 * Helper method for piping the contents of an input stream to
	 * an output stream.
	 * 
	 * @param is Input stream to pipe from.
	 * @param os Output stream to pipe to.
	 */
	public static void pipe( InputStream is, OutputStream os )
		throws IOException
	{
		int read;
		byte[] data = new byte[16384];
		while ( (read = is.read(data)) != -1 )
		{
			os.write( data, 0, read );
		}
	}
	
}
