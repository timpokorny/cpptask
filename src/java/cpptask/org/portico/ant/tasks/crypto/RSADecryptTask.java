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
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.portico.ant.tasks.utils.CryptoUtils;

/**
 * Decrypts a single file that was encoded using the RSA algorithm.
 * Decryption requires a public key corresponding to the private key used
 * during encryption. Note also that the format of the key must be *.der
 * and PKCS#8 encoding.
 * <p/>
 * Example:
 * <pre>
 *   <rsa-decrypt keyfile="public.der" srcfile="file.encrypted" destfile="file.decrypted"/>
 * </pre>
 */
public class RSADecryptTask extends Task
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
			throw new BuildException( "Failed to decrypt: " + e.getMessage(), e );
		}
	}
	
	/**
	 * Sets the path of the decrypted file.
	 * 
	 * @param destFile Path of the decrypted file.
	 */
	public void setDestfile( File destFile )
	{
		this.destFile = destFile;
	}
	
	/**
	 * Sets the (public) key to use to decrypt the source file.
	 * 
	 * @param keyFile Path of the key to encrypt with.
	 */
	public void setKeyfile( File keyFile )
	{
		this.keyFile = keyFile;
	}
	
	/**
	 * Sets the path of the source file to decrypt.
	 * 
	 * @param srcFile Path of the file to decrypt.
	 */
	public void setSrcfile( File srcFile )
	{
		this.srcFile = srcFile;
	}
	
	/**
	 * Returns an initialized {@link Cipher} instance that can be used to decrypt a file.
	 * 
	 * @param keyFile Public key.
	 * @return {@link Cipher} instance.
	 */
	private Cipher loadCipher( File keyFile )
		throws IOException, GeneralSecurityException
	{
		InputStream keyStream = new FileInputStream( keyFile );
		byte[] keyBytes = CryptoUtils.toByteArray( keyStream );
		keyStream.close();
		X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec( keyBytes );
		KeyFactory keyFactory = KeyFactory.getInstance( "RSA" );
		PublicKey key = keyFactory.generatePublic( pubKeySpec );
		Cipher pkCipher = Cipher.getInstance( "RSA" );
		pkCipher.init( Cipher.DECRYPT_MODE, key );
		return pkCipher;
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
