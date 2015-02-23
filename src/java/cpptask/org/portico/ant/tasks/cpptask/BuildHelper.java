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

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.portico.ant.tasks.utils.Platform;


/**
 * This class contains methods that will help a build complete its required steps.
 * It provides a bunch of utility methods designed around the various steps that
 * are consistent across most compilers (with the exclusion of msdev-based builds)
 * so as to ensure that directories are created appropriately, files to compile found
 * and such.
 */
public class BuildHelper
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------
	public static final String FILE_SEPARATOR = System.getProperty( "file.separator" );

	// the extension for object files, defaults to GCC style
	// if your compiler is different, it should change this value when it starts
	public static String O_EXTENSION = ".o";

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private BuildConfiguration configuration;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public BuildHelper( BuildConfiguration configuration )
	{
		this.configuration = configuration;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// 1. Build Space Preparation Methods ////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Prepares the build space for the compilation process by trying to create the various
	 * build directorys (if they do not exist). These can be configured by the user, but they
	 * default to:
	 * <ul>
	 *   <li>${workingDirectory}                 -- location for all working files</li>
	 *   <li>${workingDirectory}/temp/[platform] -- temp files generated during the build</li> 
	 *   <li>${workingDirectory}/complete        -- completed compilation units (libs/exe's)</li>
	 * </ul>
	 */
	public void prepareBuildSpace()
	{
		configuration.getWorkingDirectory().mkdirs();
		configuration.getOutputDirectory().mkdirs();
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////// 2. Compilation Helper Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Get an array of all the files that need to be compiled. If we are doing incremental
	 * compiling, check the objdir (as identified in the {@link BuildConfiguration}) to see
	 * if we need to compile the file or not.
	 * <p/>
	 * If incremental compiling is NOT enabled, this method will just return all the source files
	 * so that they are all recompiled. If it IS enabled, any source file that has a timestamp
	 * later than the .o file of the same name will need to be recompiled (as it has been modified
	 * in the mean time) and will be returned. 
	 * 
	 * @param buildDirectory The directory where compiled files will exist. This is used to check
	 *                       timestamps of source files against existing compiled versions if
	 *                       incremental building is used. 
	 */
	public File[] getFilesThatNeedCompiling( File buildDirectory )
	{
		// used for logging
		Task task = configuration.getTask();
		
		////////////////////////////////////////////////////////////////////
		// generate a list of ALL files, regardless of incremental status //
		////////////////////////////////////////////////////////////////////
		ArrayList<File> sourceFiles = new ArrayList<File>();
		for( FileSet set : configuration.getSourceFiles() )
		{
			for( File file : listFiles(set) )
			{
				// check that is it a source file
				if( isSourceFile(file) == false )
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
				File[] ofiles = buildDirectory.listFiles( new Filter(localName) );
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
	 * Returns true if the file is a source file (ends with .c, .cpp, ...)
	 */
	private boolean isSourceFile( File file )
	{
		String filename = file.getName();
		if( filename.endsWith(".c") ||
		    filename.endsWith(".cpp") ||
		    filename.endsWith(".rc") ||
		    filename.endsWith(".cxx") ||
		    filename.endsWith(".hxx") )
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Convert the FileSet into a fully resolved list of file (basedir+separator+file)
	 */
	private File[] listFiles( FileSet set )
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

	////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 3. Linking Helper Methods /////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * This method will take the output file and convert the name as necessary. The conversion
	 * that takes place depends on both the output file (executable/library) and the platform
	 * on which Ant is running. See the code for more information.
	 */
	public File getPlatformSpecificOutputFile()
	{
		// This method will generate a file from the configured output directory and output
		// name. The initial assessment below will just update the local outputName variable
		// and we'll turn it into a file at the end. This is important, because we'll also
		// update the name with a "d" at the end if we're on a debug run.
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

		// create a new File object from the information and return it
		return new File( configuration.getOutputDirectory(), outputName );
	}

	/**
	 * Get a list of all the ofiles that should be included in the link. This will be any ofiles
	 * specified explicitly in the filesets, or the ofiles relating to any source files specified
	 * in the filesets.
	 * 
	 * @param buildDirectory The directory to search for the object files in
	 */
	public File[] getFilesThatNeedLinking( File buildDirectory )
	{
		ArrayList<File> ofiles = new ArrayList<File>();
		for( FileSet set : configuration.getSourceFiles() )
		{
			for( File file : listFiles(set) )
			{
				// If this is an o-file, it has been explicitly mentioned, so include it
				// Otherwise, if this file is a source file, get the o-file equiv for it
				// e.g. If MyClass.cpp is in the source files, MyClass.o should be in the link
				if( file.getName().endsWith(O_EXTENSION) )
					ofiles.add( file );
				else if( isSourceFile(file) )
					ofiles.add( getOFile(buildDirectory,file) );
			}
		}
		
		return ofiles.toArray( new File[0] );
	}	

	/**
	 * Get a file that represents the .o file equiv of the source file. The .o file should
	 * reside in the objectDirectory.
	 * <p/>
	 * This takes the filename of the source file, replaces the file type suffix (e.g. ".cpp")
	 * with ".o" and appends the full path to the object directory to the front.
	 */
	public File getOFile( File objectDirectory, File sourceFile )
	{
		// remove the file type suffix from the source file name
		String fileName = sourceFile.getName();
		fileName = fileName.substring( 0, fileName.lastIndexOf('.') );
		
		// append ".o" to the end of the file name or if it's a resource, leave it
		if( sourceFile.getName().endsWith(".rc") )
			fileName += ".res";
		else
			fileName += O_EXTENSION;
		
		// prefix the location with the object directory
		fileName = objectDirectory.getAbsolutePath() + FILE_SEPARATOR + fileName;
		
		// create a new file and return it
		return new File( fileName );
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
