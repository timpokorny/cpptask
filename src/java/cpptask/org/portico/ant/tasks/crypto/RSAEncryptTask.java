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
package org.portico.ant.tasks.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.portico.ant.tasks.utils.CryptoUtils;

/**
 * Encrypts a single file using the RSA algorithm. A private key must be
 * supplied to this task. When generating the key, ensure that it has
 * a length of 2048 bits and that it is of the *.der format using PKCS#8
 * encoding, otherwise encryption may fail. 
 * <p/>
 * Example:
 * <pre>
 *   <rsa-encrypt keyfile="private.der" srcfile="file" destfile="file.encrypted"/>
 * </pre>
 * <p/>
 * Sample Key Pair Generation with OpenSSL:
 * <pre>
 *   openssl genrsa -out private.pem 2048
 *   openssl pkcs8 -topk8 -in private.pem -outform DER -out private.der -nocrypt
 *   openssl rsa -in private.pem -pubout -outform DER -out public.der
 * </pre>
 */
public class RSAEncryptTask extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------

	private File destFile;
	private File keyFile;
	private File srcFile;
	
	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	
	/**
	 *  Ant task execution implementation.
	 */
	public void execute() throws BuildException
	{
		if( destFile == null )
			throw new BuildException( "Expected the 'destfile' attribute" );
		if( keyFile == null )
			throw new BuildException( "Expected the 'keyfile' attribute" );
		if( srcFile == null )
			throw new BuildException( "Expected the 'srcfile' attribute" );
		try
		{
			Cipher pkCipher = loadCipher( keyFile );
			InputStream srcStream = new FileInputStream( srcFile );
			CipherOutputStream destStream =
				new CipherOutputStream( new FileOutputStream(destFile), pkCipher );
			CryptoUtils.pipe( srcStream, destStream );
			srcStream.close();
			destStream.close();
		}
		catch( Exception e )
		{
			throw new BuildException( "Failed to encrypt: " + e.getMessage(), e );
		}
	}
	
	/**
	 * Sets the path of the encrypted file.
	 * 
	 * @param destFile Path of the encrypted file.
	 */
	public void setDestfile( File destFile )
	{
		this.destFile = destFile;
	}
	
	/**
	 * Sets the (private) key to use to encrypt the source file.
	 * 
	 * @param keyFile Path of the key to encrypt with.
	 */
	public void setKeyfile( File keyFile )
	{
		this.keyFile = keyFile;
	}
	
	/**
	 * Sets the path of the source file to encrypt.
	 * 
	 * @param srcFile Path of the file to encrypt.
	 */
	public void setSrcfile( File srcFile )
	{
		this.srcFile = srcFile;
	}
	
	/**
	 * Returns an initialized {@link Cipher} instance that can be used to encrypt a file.
	 * 
	 * @param keyFile Private key.
	 * @return {@link Cipher} instance.
	 */
	private Cipher loadCipher( File keyFile )
		throws IOException, GeneralSecurityException
	{
		InputStream keyStream = new FileInputStream( keyFile );
		byte[] keyBytes = CryptoUtils.toByteArray( keyStream );
		keyStream.close();
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec( keyBytes );
		KeyFactory keyFactory = KeyFactory.getInstance( "RSA" );
		PrivateKey key = keyFactory.generatePrivate( keySpec );	
		Cipher pkCipher = Cipher.getInstance( "RSA" );
		pkCipher.init( Cipher.ENCRYPT_MODE, key );
		return pkCipher;
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
