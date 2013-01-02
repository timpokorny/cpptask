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
package com.lbf.tasks.cpptask.msvc;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;

import com.lbf.tasks.cpptask.BuildConfiguration;
import com.lbf.tasks.cpptask.BuildHelper;
import com.lbf.tasks.cpptask.Compiler;
import com.lbf.tasks.cpptask.Define;
import com.lbf.tasks.cpptask.IncludePath;
import com.lbf.tasks.cpptask.Library;
import com.lbf.tasks.cpptask.OutputType;
import com.lbf.tasks.utils.Arch;
import com.lbf.tasks.utils.StringUtilities;

public class CompilerMSVC implements Compiler
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------
	public static final String FILE_SEPARATOR = System.getProperty( "file.separator" );

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private Version version;
	private BuildConfiguration configuration;
	private Task task;
	private BuildHelper helper;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	public CompilerMSVC( Version version )
	{
		this.version = version;

		// update the Utilities with object file extension
		BuildHelper.O_EXTENSION = ".obj";
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void runCompiler( BuildConfiguration configuration ) throws BuildException
	{
		// extract the necessary information
		this.configuration = configuration;
		this.task = configuration.getTask();
		this.helper = new BuildHelper( configuration );

		// make sure we're ready to go
		this.helper.prepareBuildSpace();
		
		// run the compiler
		// NOTE: This will include "/Zi" which generates debug information - apparently
		//       (and I hope I'm right) this won't screw anything else up, as debug info
		//       goes into a separate PDB, but will come in handy later if we try to
		//       link debug versions. Theory: Can't hurt, so just add it
		compile();

		// run the linker
		if( configuration.getOutputFile() != null )
		{
    		if( configuration.getBuildType().includesDebug() )
    			link( true );
    
    		if( configuration.getBuildType().includesRelease() )
    			link( false );
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// Compiler Methods ////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Execute the actual compilation step. Note that we will always include debugging information
	 * for VS compiles as it adds pretty much nothing to the generated object files (putting debug
	 * information in PDB files instead).
	 */
	private void compile()
	{
		task.log( "Starting Compile" );

		// create the command line that will be used for each compile
		// this is just the part of it that doesn't include the file being compiled
		// NOTE: This will include "/Zi" which generates debug information - apparently
		//       (and I hope I'm right) this won't screw anything else up, as debug info
		//       goes into a separate PDB, but will come in handy later if we try to
		//       link debug versions. Theory: Can't hurt, so just add it
		Commandline command = generateCompileCommand();

		// get all the files that we should compile
		// this will run checks for things like incremental compiling
		File buildDirectory = configuration.getTempDirectory();
		File[] filesToCompile = helper.getFilesThatNeedCompiling( buildDirectory );
		task.log( "" + filesToCompile.length + " files to be compiled." );

		// make sure we have files to compile!
		if( filesToCompile.length == 0 )
		{
			task.log( "Skipping Compile: Up to date" );
			return;
		}
		
		// create the response file which has all the compile information
		File responseFile = createResponseFile( "compile-files",
		                                        buildDirectory,
		                                        StringUtilities.filesToStrings(filesToCompile) );
		
		// put the response file on the end of the command line
		command.createArgument().setValue( "@"+responseFile.getAbsolutePath() );
		
		// prepend the pre-command (if there is one) and run this thing
		Execute runner = new Execute( new LogStreamHandler(configuration.getTask(),
		                                                   Project.MSG_INFO,
		                                                   Project.MSG_WARN) );
		
		runner.setCommandline( prependEnvironment(command.getCommandline()) );
		runner.setWorkingDirectory( buildDirectory );
		try
		{
			// log what we're doing
			task.log( "Starting Compile" );
			task.log( "Running compile command: ", Project.MSG_VERBOSE );
			for( String argument : runner.getCommandline() )
				task.log( argument, Project.MSG_VERBOSE );

			int exitValue = runner.execute();
			if( exitValue != 0 )
			{
				throw new BuildException( "Compile Failed, (exit value: " + exitValue + ")" );
			}
		}
		catch( IOException e )
		{
			// most likely, the compiler isn't on the path and can't be found. print out
			// kill the build with a nicer error
			String msg = "There was a problem running the compiler, this usually occurs when " +
			             "windows can't find the compiler (cl.exe), make sure it is on your path." +
			             " full error: " + e.getMessage();
			throw new BuildException( msg, e );
		}
		
		// NOTE: I've been making some changes and now RC file support isn't as
		//       simple as it once was. This will need to be added back as some
		//       point in the future, perhaps by splitting the compile file array
		//       into two separate ones (one for rc files, one for the rest):
		//if(sourceFile.getName().endsWith(".rc"))
		//{
		//	// Is this a win32 resource file?
		//	theCommand = new Commandline();
		//	theCommand.setExecutable( "rc" );
		//	theCommand.createArgument().setFile( sourceFile );
		//}

		task.log( "Compile complete" );
	}

	/**
	 * Generates the command that will be used for the compile of each relevant file.
	 * This is just the extra stuff that doesn't include the name of the file being compiled.
	 */
	private Commandline generateCompileCommand()
	{
		// create the working directories if they don't already exist
		configuration.getTempDirectory(true).mkdirs();

		// create the command line
		Commandline commandline = new Commandline();
		commandline.setExecutable( "cl" );
		commandline.createArgument().setValue( "/c" );
		commandline.createArgument().setValue( "/nologo" );
		commandline.createArgument().setValue( "/Zi" );

		/////// additional args ////////
		// do this up front
		String[] commands = Commandline.translateCommandline( configuration.getCompilerArgs() );
		commandline.addArguments( commands );
		
		////// includes //////
		for( IncludePath path : configuration.getIncludePaths() )
		{
			// make sure there is a path
			if( path.getPath() != null )
			{
				// add each path element to the line
				for( String temp : path.getPath().list() )
					commandline.createArgument().setLine( "/I" + Commandline.quoteArgument(temp) );
			}
		}
		
		////// defines ///////
		for( Define define : configuration.getDefines() )
		{
			commandline.createArgument().setLine( "/D" + define.getName() );
		}
		
		return commandline;
	}

	/**
	 * This method takes the base command line and prepends to it:
	 * <ul>
	 *   <li>The Visual Studio environment setup calls (vcvarsall.bat)</li>
	 *   <li>Any pre-commands coming from the task</li>
	 * </ul>
	 * @param baseCommand
	 * @return
	 */
	private String[] prependEnvironment( String[] baseCommand )
	{
		// 1. Get a reference to the Visual Studio environment setup file and argument
		//    The argument we provide depends on the operating system we are on and the
		//    bitness of what we are targeting. If we are on 32- or 64-bit systems and
		//    targeting the same, all is normal. If we are on 32-bit and targeting 64-bit
		//    we need to use a special cross compiler
		String vcvarsall = this.version.getVcvarsallBatchFile();
		String outputArch = getCompilerArchitecture();
		
		// 2. Get the pre-command stored in the build configuration
		String precommand = configuration.getPreCommand();
		
		// 3. Throw all the commands together into one big command line
		ArrayList<String> fullCommand = new ArrayList<String>();
		// vs environment
		fullCommand.add( vcvarsall );
		fullCommand.add( outputArch );
		fullCommand.add( "&&" );
		// pre-command
		if( (precommand != null) && (precommand.trim().isEmpty() == false) )
		{
			String[] precommands = configuration.getPreCommand().split( "\\s+" );
			for( String temp : precommands )
				fullCommand.add( temp );
			fullCommand.add( "&&" );
		}
		// base command line
		for( String temp : baseCommand )
			fullCommand.add( temp );

		return fullCommand.toArray( new String[0] );
	}

	/**
	 * Return the type of compiler we need to use. Calculated as follows:
	 * <ul>
	 *   <li>If we are on a 32-bit system and building a 32-bit target: return x86</li>
	 *   <li>If we are on a 64-bit system and building a 64-bit target: return amd64</li>
	 *   <li>If we are on a 32-bit system and building a 64-bit target: return x86_amd64</li>
	 * </ul>
	 */
	private String getCompilerArchitecture()
	{
		Arch osArch = Arch.getOsArch();
		Arch outArch = configuration.getOutputArch();
		if( osArch == Arch.x86 )
		{
			if( outArch == Arch.x86 )
			{
				task.log( "Building 32-bit target on 32-bit system. Using x86 compiler.",
				          Project.MSG_VERBOSE );
				return "x86";
			}
			else
			{
				task.log( "Building 64-bit target on 32-bit system. Using x86_amd64 cross-compiler.",
				          Project.MSG_VERBOSE );
				return "x86_amd64";
			}
		}
		else
		{
			if( outArch == Arch.x86 )
			{
				task.log( "Building 32-bit target on 64-bit system. Using x86 compiler.",
				          Project.MSG_VERBOSE);
				return "x86";
			}
			else
			{
				task.log( "Building 64-bit target on 64-bit system. Using amd64 compiler.",
				          Project.MSG_VERBOSE );
				return "amd64";
			}
		}
	}

	/**
	 * Creates a new response file for use in compiling or linking and populates it with the
	 * given commands.
	 * 
	 * @param name The name of the response file to create (excluding the extension)
	 * @param directory The directory to put the response file in
	 * @param commands The commands to write to the file
	 */
	private File createResponseFile( String name, File directory, String[] commands )
		throws BuildException
	{
		File responseFile = new File( directory, name+".rsp" );
		
		// write the commands to the response file, one per line
		// this will truncate the file if it exists
		try
		{
			task.log( "Writing response file ["+responseFile+"]", Project.MSG_VERBOSE );
			PrintWriter writer = new PrintWriter( responseFile );
			for( String command : commands )
				writer.println( "\"" + command + "\"" );
			
			writer.close();
		}
		catch( Exception e )
		{
			throw new BuildException( "Problem writing response file: " + e.getMessage(), e );
		}
		
		return responseFile;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Linker Methods /////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * This method is the main manager of the linking process. It should only be run if an
	 * "outfile" has been provided in the configuration. It will attempt to link all the
	 * files in the objdir into a simple executable/library.
	 * 
	 * @param isDebugRun If true, this call will always include "/DEBUG" in the linker arguments.
	 *                   This is used when buildDebugLibs is included in the main Ant task call
	 *                   instructing us to build both release and debug libraries
	 */
	private void link( boolean isDebugRun )
	{
		// generate the base command line
		// object and library files done in a separate response file
		Commandline commandline = generateLinkCommand( isDebugRun );
		
		// build the response file with the object and library files to link with
		File responseFile = createResponseFile( "linker-files",
		                                        configuration.getTempDirectory(),
		                                        getFilesAndLibrariesToLinkWith() );
		
		// put the response file on the end of the command
		commandline.createArgument().setValue( "@"+responseFile.getAbsolutePath() );
		
		// create the execution object
		Execute runner = new Execute( new LogStreamHandler( configuration.getTask(),
		                                                    Project.MSG_INFO,
		                                                    Project.MSG_WARN) );
		
		runner.setCommandline( prependEnvironment(commandline.getCommandline()) );
		runner.setWorkingDirectory( configuration.getTempDirectory().getParentFile() );

		// run the command
		try
		{
			// log what we're doing
			String extraInfo = isDebugRun ? "(debug)" : "(release)";
			task.log( "Starting Link "+extraInfo );
			task.log( "Running link command: ", Project.MSG_DEBUG );
			for( String argument : commandline.getCommandline() )
				task.log( argument, Project.MSG_DEBUG );

			int exitValue = runner.execute();
			if( exitValue != 0 )
			{
				throw new BuildException( "Link Failed, (exit value: " + exitValue + ")" );
			}
		}
		catch( IOException e )
		{
			// most likely, the compiler isn't on the path and can't be found. print out
			// kill the build with a nicer error
			String msg = "There was a problem running the linker, this usually occurs when windows"+
			             " can't find the linker (link.exe), make sure it is on your path." +
			             " full error: " + e.getMessage();
			throw new BuildException( msg, e );
		}
		
		task.log( "Link complete. Library in directory: " + configuration.getOutputDirectory() );
		task.log( "" ); // a little bit of space
	}

	/**
	 * Generates the linker command line. The specification of object files and libraries that
	 * are to be linked with is handled through a special response file. That file is built
	 * outside of this method. This method simply builds base the link.exe command line.
	 */
	private Commandline generateLinkCommand( boolean isDebugRun )
	{
		// create the command line in which to store the information
		Commandline commandline = new Commandline();
		commandline.setExecutable( "link" );
		
		/////// output file name ///////
		commandline.createArgument().setValue( "/NOLOGO" );
		commandline.createArgument().setValue( "/SUBSYSTEM:CONSOLE" );
		//commandline.createArgument().setValue( "/INCREMENTAL:NO" );
		commandline.createArgument().setValue( "/OUT:" + helper.getPlatformSpecificOutputFile(isDebugRun) );
		if( isDebugRun )
			commandline.createArgument().setValue( "/DEBUG" );
		
		/////// output file type ///////
		if( configuration.getOutputType() == OutputType.SHARED )
			commandline.createArgument().setValue( "/DLL" );
		
		return commandline;
	}

	/**
	 * Get an array of all the paths for files and libraries that we need to link with. These
	 * paths will be added to the response file that will be used as input for the link command.
	 */
	private String[] getFilesAndLibrariesToLinkWith()
	{
		/////// libraries to link with ///////
		// for each specified library, search in the library paths for a file of the name
		// "name.lib" and add it to the command line
		// step 1: generate complete list of libraries to link with
		Set<String> linkWith = new HashSet<String>();
		Set<String> linkPaths = new HashSet<String>();
		for( Library library : configuration.getLibraries() )
		{
			// get the names of each contained library
			for( String temp : library.getLibs() )
				linkWith.add( temp );
			
			for( String path : library.getPath().list() )
				linkPaths.add( path );
		}
		
		List<String> returnArray = new ArrayList<String>();
		// locate each library
		for( String libToFind : linkWith )
		{
			task.log( "Beginning search for ["+libToFind+"]", Project.MSG_VERBOSE );
			
			// all libraries should have a ".lib" as their file type
			// some libraries will have "lib" on the front as well (e.g. libRTI-NG.lib)
			// thus, we should check for both forms (with and without "lib" on the front)
			libToFind += ".lib";
			boolean found = false;

			// try to locate the file
			for( String path : linkPaths )
			{
				// without "lib" on the front
				File possible = new File( path + FILE_SEPARATOR + libToFind );
				task.log( "[check] "+possible, Project.MSG_DEBUG );
				if( possible.exists() )
				{
					// found it!
					found = true;
					returnArray.add( possible.getAbsolutePath() );
					//commandline.createArgument().setFile( possible );
					task.log( "[located] "+possible, Project.MSG_VERBOSE );
					break;
				}
				
				// regular doesn't exist, try with "lib" on front
				possible = new File( path + FILE_SEPARATOR + "lib" + libToFind );
				task.log( "[check] "+possible, Project.MSG_DEBUG );
				if( possible.exists() )
				{
					// found it now :)
					found = true;
					returnArray.add( possible.getAbsolutePath() );
					//commandline.createArgument().setFile( possible );
					task.log( "[located] "+possible, Project.MSG_VERBOSE );
					break;
				}
			}
			
			// check to make sure we found it
			if( found == false )
				throw new BuildException( "Couldn't find library: " + libToFind + " for linking" );
		}

		/////// additional linker args ///////
		String[] commands = Commandline.translateCommandline( configuration.getLinkerArgs() );
		//commandline.addArguments( commands );
		for( String temp : commands )
			returnArray.add( temp );

		////////////////////////////////////
		/////// object files to link ///////
		////////////////////////////////////
		for( File ofile : helper.getFilesThatNeedLinking(configuration.getTempDirectory()) )
		{
			returnArray.add( ofile.getAbsolutePath() );
		}
		
		// return the finished product!
		return returnArray.toArray( new String[0] );
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
