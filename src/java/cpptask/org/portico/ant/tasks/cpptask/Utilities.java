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
package org.portico.ant.tasks.cpptask;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.portico.ant.tasks.utils.Platform;


public class Utilities
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------
	public static final String LINE_SEPARATOR = System.getProperty( "line.separator" );
	public static final String FILE_SEPARATOR = System.getProperty( "file.separator" );
	
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
					configuration.getWorkingDirectory().listFiles( new Filter(localName) );
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
					ofiles.add( getOFile(configuration.getWorkingDirectory(),file) );
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
		File outputDirectory = configuration.getOutputDirectory();
		String outputName = configuration.getOutputName();
		
		// if this is an executable file, and we're on windows, make sure .exe is on the end
		if( configuration.getOutputType() == OutputType.EXECUTABLE )
		{
			///////////////////////////////////////////
			///////////// Executable File /////////////
			///////////////////////////////////////////
			// if we're not in windows, do nothing
			// if we are windows, put a .exe on the end
			if( Platform.getOsPlatform().isWindows() )
			{
				// does the output file have .exe on the end?
				if( configuration.getOutputName().endsWith(".exe") == false )
					outputName += ".exe";
			}
		}
		else if( configuration.getOutputType() == OutputType.SHARED )
		{
			////////////////////////////////////////////
			////////////// Shared Library //////////////
			////////////////////////////////////////////
			// it's a shared library, do the checks
			if( Platform.getOsPlatform().isWindows() )
			{
				if( outputName.endsWith(".dll") == false )
					outputName += ".dll";
			}
			else if( Platform.getOsPlatform().isMac() )
			{
				// put a "lib" on the front
				if( outputName.startsWith("lib") == false )
					outputName = "lib" + outputName;
				
				// check for .dylib
				if( outputName.endsWith(".dylib") == false )
					outputName += ".dylib";
			}
			else
			{
				// put a "lib" on the front
				if( outputName.startsWith("lib") == false )
					outputName = "lib" + outputName;
				
				if( outputName.endsWith(".so") == false ) 
					outputName += ".so";
			}
		}
		else if( configuration.getOutputType() == OutputType.STATIC )
		{
			////////////////////////////////////////////
			////////////// Static Library //////////////
			////////////////////////////////////////////
			if( Platform.getOsPlatform().isWindows() )
			{
				if( outputName.endsWith(".lib") == false )
					outputName += ".lib";
			}
			else
			{
				// put a "lib" on the front
				if( outputName.startsWith("lib") == false )
					outputName = "lib" + outputName;
				
				// check for .dylib
				if( outputName.endsWith(".a") == false )
					outputName += ".a";
			}
		}

		File newFile = new File( outputDirectory, outputName );
		// update the configuration
		configuration.setOutputName( outputName );
		return newFile;
	}	

	/**
	 * Takes the given file or directory and appends a "d" to the end to signify it is a
	 * debug version. If the parameter represents a directory, a "d" is appened and new
	 * file representing the path returned. If it is a file, a "d" is inserted infront of
	 * the last "."
	 * <p/>
	 * Note that the path does not represent a file that actually exists, and no check is
	 * made to see if this is the case or not.
	 * @param file
	 * @return
	 */
	public static File getDebugVersionOfFile( File file )
	{
		// get the current name of the file
		String name = file.getName();

		// find the place where the last "." is and slip "d" in before it
		// if there is no final ".", just tack "d" onto the end
		int lastPeriod = name.lastIndexOf( "." );
		if( lastPeriod == -1 )
			return new File( file.getParent(), name+"d" );

		// example input string "mylibrary.so"
		String newname = name.substring( 0, lastPeriod ) + // "mylibrary"
		                 "d" +                             // the "d" for debug. yay!
		                 name.substring( lastPeriod );     // ".so"

		// return the new file
		return new File( file.getParent(), newname );
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

	/**
	 * Returns true if the given array contains the given value. False otherwise. Each
	 * element will be stripped of whitespace before being compared.
	 * 
	 * @param array The array to look into
	 * @param value The value to look for
	 */
	public static boolean arrayContains( String[] array, String value )
	{
		for( String potential : array )
		{
			if( potential.trim().equals(value) )
				return true;
		}

		// didn't find it
		return false;
	}
}
