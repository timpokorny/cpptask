/*
 *   Copyright 2007 littlebluefroglabs.com
 *
 *   This file is part of cpptask.
 *
 *   cpptask is free software; you can redistribute it and/or modify
 *   it under the terms of the Common Developer and Distribution License (CDDL) 
 *   as published by Sun Microsystems. For more information see the LICENSE file.
 *
 *   Use of this software is strictly AT YOUR OWN RISK!!!
 *   If something bad happens you do not have permission to come crying to me.
 *   (that goes for your lawyer as well)
 *
 */
package com.lbf.tasks.cpptask;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

public class Utilities
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------
	public static final String LINE_SEPARATOR = System.getProperty( "line.separator" );
	public static final String FILE_SEPARATOR = System.getProperty( "file.separator" );
	
	public static final String PLATFORM = System.getProperty( "os.name" );
	public static final boolean WIN32 = PLATFORM.toUpperCase().contains( "WINDOWS" );
	public static final boolean LINUX = PLATFORM.toUpperCase().contains( "LINUX" );
	public static final boolean MACOS = PLATFORM.toUpperCase().contains( "MAC" );

	// the extension for object files, defaults to GCC style
	// if your compiler is different, it should change this value when it starts
	public static String O_EXTENSION = ".o";

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
	 * Get an array of all the files that need to be compiled. If we are doing incremental
	 * compiling, check the objdir (as identified in the {@link BuildConfiguration} to see
	 * if we need to compile the file or not. If the source file has a timestamp that is
	 * later than the .o file of the same name, then the file will need to be recompiled
	 * (as it has been modified in the mean time). If increment compiling is NOT enabled, this
	 * method will just return all the source files so that they are all recompiled.
	 * 
	 * @param configuration The configuraiton information for this particular run
	 * @param task The build task that is currently executing
	 * @param project The project that is currently being executed
	 */
	public static File[] getFilesToCompile( BuildConfiguration configuration, Task task )
	{
		////////////////////////////////////////////////////////////////////
		// generate a list of ALL files, regardless of incremental status //
		////////////////////////////////////////////////////////////////////
		ArrayList<File> sourceFiles = new ArrayList<File>();
		for( FileSet set : configuration.getSourceFiles() )
		{
			for( File file : Utilities.listFiles(set) )
			{
				// check that is it a source file
				if( Utilities.isSourceFile(file) == false )
				{
					task.log( "Skipping " + file + ", not a source file", Project.MSG_DEBUG );
				}
				else if( file.canRead() == false )
				{
					task.log( "Skipping " + file + ", can't find/read it", Project.MSG_DEBUG );
				}
				else
				{
					sourceFiles.add( file );
				}
			}
		}

		/////////////////////////////////////////////////////////////////////
		// if it is an incremental compile, remove files that are uptodate //
		/////////////////////////////////////////////////////////////////////
		if( configuration.isIncremental() )
		{
			// create somewhere to store all the files that are up to date and shouldn't be
			// compiled. we can't modify the collection while iterating, so we just have to
			// collect names and remove them from the set afterwards
			ArrayList<File> uptodate = new ArrayList<File>();
			task.log( "Starting up-to-date analysis for " + sourceFiles.size() + " source files." );

			// check each file to see if it needs to be compiled
			for( File sourceFile : sourceFiles )
			{
				// get the name of the file without the suffix
				String localName = sourceFile.getName();
				localName = localName.substring( 0, localName.indexOf('.') );
				
				// check for the presence of an ".o" file in the target directory
				File[] ofiles =
					configuration.getObjectDirectory().listFiles( new Filter(localName) );
				if( ofiles.length != 0 )
				{
					// an .o file exists, check modification times
					File ofile = ofiles[0];
					if( ofile.lastModified() > sourceFile.lastModified() )
					{
						// the ofile is older than the source file, so it hasn't
						// been changed since the last time we compiled, we can skip it
						task.log( "Skipping file (up to date): " + sourceFile, Project.MSG_DEBUG );
						uptodate.add( sourceFile );
					}
				}
			}
			
			// remove from the collection any source files which are up to date
			sourceFiles.removeAll( uptodate );
			task.log( "" + uptodate.size() + " file are up to date." );
		}
		
		// return all the files that are to be compiled
		//File[][] returnValue = new File[][]{ sourceFiles.toArray(new File[0]),
		//                                     uptodate.toArray(new File[0]) };
		return sourceFiles.toArray( new File[0] );
	}

	/**
	 * Get a file that represents the .o file equiv of the source file. The .o file should
	 * reside in the objectDirectory.
	 * <p/>
	 * This takes the filename of the source file, replaces the file type suffix (e.g. ".cpp")
	 * with ".o" and appends the full path to the object directory to the front.
	 */
	public static File getOFile( File objectDirectory, File sourceFile )
	{
		// remove the file type suffix from the source file name
		String fileName = sourceFile.getName();
		fileName = fileName.substring( 0, fileName.lastIndexOf('.') );
		
		// append ".o" to the end of the file name
		fileName += O_EXTENSION;
		
		// prefix the location with the object directory
		fileName = objectDirectory.getAbsolutePath() + FILE_SEPARATOR + fileName;
		
		// create a new file and return it
		return new File( fileName );
	}

	/**
	 * Get a list of all the ofiles that should be included in the link. This will be any ofiles
	 * specified explicitly in the filesets, or the ofiles relating to any source files specified
	 * in the filesets.
	 */
	public static File[] getOFilesForLinking( BuildConfiguration configuration )
	{
		ArrayList<File> ofiles = new ArrayList<File>();
		
		// get any explicitly mentioned ofiles from the filesets
		for( FileSet set : configuration.getSourceFiles() )
		{
			for( File file : Utilities.listFiles(set) )
			{
				// check that is it an object file
				if( file.getName().endsWith(O_EXTENSION) )
					ofiles.add( file );
			}
		}
		
		// get the source equivs for any source files from the filesets
		// e.g. If MyClass.cpp is in the source files, MyClass.o should be in the link
		for( FileSet set: configuration.getSourceFiles() )
		{
			for( File file : Utilities.listFiles(set) )
			{
				if( Utilities.isSourceFile(file) )
				{
					// get the .o version of it
					ofiles.add( getOFile(configuration.getObjectDirectory(),file) );
				}
			}
		}
		
		return ofiles.toArray( new File[0] );
	}	
	
	/**
	 * This method will take the output file and convert the name as necessary. The conversion
	 * that takes place depends on both the output file (executable/library) and the platform
	 * on which Ant is running. See the code for more information.
	 */
	public static File getLibraryFile( BuildConfiguration configuration )
	{
		///////////////////////////////////////////
		///////////// Executable File /////////////
		///////////////////////////////////////////
		// if this is an executable file, and we're on windows, make sure .exe is on the end
		if( configuration.getOutputType() == OutputType.EXECUTABLE )
		{
			// if we're not in windows, do nothing
			if( !WIN32 )
				return configuration.getOutputFile();
			
			// does the output file have .exe on the end?
			if( configuration.getOutputFile().getAbsolutePath().endsWith(".exe") == false )
			{
				File newFile = new File( configuration.getOutputFile().getAbsolutePath() + ".exe" );
				configuration.setOutputFile( newFile );
				return configuration.getOutputFile();
			}
			else
			{
				return configuration.getOutputFile();
			}
		}

		//////////////////////////////////////////
		////////////// Library File //////////////
		//////////////////////////////////////////
		// it's a shared library, do the checks
		String filename = configuration.getOutputFile().getName();
		if( WIN32 )
		{
			if( filename.endsWith(".dll") == false )
				filename += ".dll";
		}
		else if( MACOS )
		{
			// put a "lib" on the front
			if( filename.startsWith("lib") == false )
				filename = "lib" + filename;
			
			// check for .dylib
			if( filename.endsWith(".dylib") == false )
				filename += ".dylib";
		}
		else
		{
			// put a "lib" on the front
			if( filename.startsWith("lib") == false )
				filename = "lib" + filename;
			
			if( filename.endsWith(".so") == false ) 
				filename += ".so";
		}

		File newFile = new File( configuration.getOutputFile().getParent() +
		                         FILE_SEPARATOR +
		                         filename );
		
		// update the configuration
		configuration.setOutputFile( newFile );
		return newFile;
	}	
	
	/**
	 * This method takes care of the dirty work of creating a StringTokenizer and breaking
	 * apart the given String (using the provided delimiter). 
	 */
	public static String[] explodeString( String theString, String delim )
	{
		StringTokenizer tokenizer = new StringTokenizer( theString, delim );
		ArrayList<String> strings = new ArrayList<String>();
		while( tokenizer.hasMoreTokens() )
		{
			strings.add( tokenizer.nextToken() );
		}
		
		return strings.toArray( new String[0] );
	}
	
	/**
	 * Convert the FileSet into a fully resolved list of file (basedir+separator+file)
	 */
	public static File[] listFiles( FileSet set )
	{
		// check for a null set
		if( set == null )
			return new File[0];
		
		ArrayList<File> locations = new ArrayList<File>();
		DirectoryScanner scanner = set.getDirectoryScanner();
		for( String file : scanner.getIncludedFiles() )
		{
			locations.add( new File(scanner.getBasedir()+FILE_SEPARATOR+file) );
		}
		
		return locations.toArray( new File[0] );
	}
	
	/**
	 * Converts a File[] to a String[] that contains the absolute paths of each file
	 */
	public static String[] filesToStrings( File[] files )
	{
		String[] strings = new String[files.length];
		for( int i = 0; i < files.length; i++ )
			strings[i] = files[i].getAbsolutePath();
		
		return strings;
	}

	/**
	 * Returns a String[] that is a combination of the given commands followed by the absolute
	 * paths of the given files.
	 */
	public static String[] concat( String[] commands, File[] files )
	{
		String[] complete = new String[commands.length+files.length];
		for( int i = 0; i < commands.length; i++ )
			complete[i] = commands[i];
		
		for( int i = 0; i < files.length; i++ )
			complete[i+commands.length] = files[i].getAbsolutePath();
			
		return complete;
	}
	
	/**
	 * Returns true if the file is a source file (ends with .c, .cpp, ...)
	 */
	public static boolean isSourceFile( File file )
	{
		if( file.getName().endsWith(".c") ||
		    file.getName().endsWith(".cpp") ||
		    file.getName().endsWith(".rc") )
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Returns true if the given location exists and is a directory
	 */
	public static boolean directoryExists( String location )
	{
		return (new File(location)).isDirectory();
	}
}
