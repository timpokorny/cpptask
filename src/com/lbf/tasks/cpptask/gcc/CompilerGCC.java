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
package com.lbf.tasks.cpptask.gcc;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;

import com.lbf.tasks.cpptask.BuildConfiguration;
import com.lbf.tasks.cpptask.BuildHelper;
import com.lbf.tasks.cpptask.Compiler;
import com.lbf.tasks.cpptask.CppTask;
import com.lbf.tasks.cpptask.Define;
import com.lbf.tasks.cpptask.IncludePath;
import com.lbf.tasks.cpptask.Library;
import com.lbf.tasks.cpptask.OutputType;
import com.lbf.tasks.utils.Platform;
import com.lbf.tasks.utils.StringUtilities;

/**
 * This class is responsible for the handling of the compilation process when using GCC.
 */
public class CompilerGCC implements Compiler
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private CppTask task;
	private BuildConfiguration configuration;
	private BuildHelper helper;
	private String executable;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	public CompilerGCC( String executableName )
	{
		this.executable = executableName;
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
		
		// debug build - if configured
		if( configuration.getBuildType().includesDebug() )
		{
			// compile
			compile( true );
			
			// link
			if( configuration.getOutputName() != null )
				link( true );
		}
		
		// release build - if configured
		if( configuration.getBuildType().includesRelease() )
		{
			// compile
			compile( false );
			
			// link
			if( configuration.getOutputName() != null )
				link( false );
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// Compiler Methods ////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Execute the actual compilation for each of the given files.
	 * <p/>
	 * If the given argument is true, the compile will automatically add the command to emit
	 * debug versions. If the debug command is already in the arguments, this setting will be
	 * irrelevant.
	 */
	private void compile( boolean debug )
	{
		// let everyone know what we're doing
		String extraInfo = debug ? "(debug)" : "(release)";
		task.log( "Starting Compile "+extraInfo );

		// generate the command line
		Commandline command = generateCompileCommand( debug );

		// get all the files that we should compile
		// this will run checks for things like incremental compiling
		File buildDirectory = configuration.getTempDirectory( debug );
		File[] filesToCompile = helper.getFilesThatNeedCompiling( buildDirectory );
		task.log( "" + filesToCompile.length + " files to be compiled." );

		// do the compilation
		for( File sourceFile : filesToCompile )
		{
			Commandline theCommand;
			// get the name of the output file
			File ofile = helper.getOFile( configuration.getTempDirectory(debug), sourceFile );
			if( sourceFile.getName().endsWith(".rc") )
			{	
				// Is this a win32 resource file?
				// create the full command
				theCommand = new Commandline();
				theCommand.setExecutable( "windres" );
				theCommand.createArgument().setValue( "-i" );
				theCommand.createArgument().setFile( sourceFile );
				theCommand.createArgument().setValue( "-J" );
				theCommand.createArgument().setValue( "rc" );
				theCommand.createArgument().setValue( "-o" );
				theCommand.createArgument().setFile( ofile );
				theCommand.createArgument().setValue( "-O" );
				theCommand.createArgument().setValue( "coff" );
			}
			else
			{
				// create the full command
				theCommand = (Commandline)command.clone();
				theCommand.createArgument().setFile( sourceFile );
				theCommand.createArgument().setValue( "-o" );
				theCommand.createArgument().setFile( ofile );
			}

			// create the execution object
			Execute runner = new Execute( new LogStreamHandler( configuration.getTask(),
			                                                    Project.MSG_INFO,
			                                                    Project.MSG_WARN) );
			
			//runner.setAntRun( project );
			runner.setCommandline( theCommand.getCommandline() );

			// run the command
			try
			{
				task.log( "  " + sourceFile.getName() );
				task.log( theCommand.toString(), Project.MSG_DEBUG );
				int exitValue = runner.execute();
				if( exitValue != 0 )
				{
					throw new BuildException( "Compile Failed, (exit value: " + exitValue + ")" );
				}
			}
			catch( IOException e )
			{
				String msg = "There was a problem running the compiler, this usually occurs when " + 
				             "it can't be found, make sure it is on your path. full error: " +
				             e.getMessage();
				throw new BuildException( msg, e );
			}
		}
		
		task.log( "Compile complete" );
	}

	/**
	 * Generates the command that will be used for the compile of each relevant file.
	 * This is just the extra stuff that doesn't include the name of the file being compiled.
	 * 
	 * @param debug Include the debug symbol if it is not already present.
	 */
	private Commandline generateCompileCommand( boolean debug )
	{
		// create the command line
		Commandline commandline = new Commandline();
		commandline.setExecutable( executable );
		commandline.createArgument().setValue( "-c" );
		
		////// includes //////
		for( IncludePath path : configuration.getIncludePaths() )
		{
			// make sure there is a path
			if( path.getPath() != null )
			{
				// add each path element to the line
				for( String temp : path.getPath().list() )
					commandline.createArgument().setLine( "-I" + Commandline.quoteArgument(temp) );
			}
		}
		
		////// defines ///////
		for( Define define : configuration.getDefines() )
		{
			commandline.createArgument().setLine( "-D" + define.getName() );
		}
		
		/////// additional args ////////
		String[] commands = Commandline.translateCommandline( configuration.getCompilerArgs() );
		commandline.addArguments( commands );
		
		// add in the debug command if it is not already present
		if( debug )
		{
			if( StringUtilities.arrayContains(commands, "-g") == false )
				commandline.createArgument().setValue( "-g" );
		}

		return commandline;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Linker Methods /////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * This method is the main manager of the linking process. It should only be run if an
	 * "outfile" has been provided in the configuration. It will attempt to link all the
	 * files in the objdir into a simple executable/library.
	 * <p/>
	 * If the given argument is true, the compile will automatically add the command to emit
	 * debug versions. If the debug command is already in the arguments, this setting will be
	 * irrelevant.
	 */
	private void link( boolean debug )
	{
		// generate the command line
		Commandline commandline = generateLinkCommand( debug );

		// create the execution object
		Execute runner = new Execute( new LogStreamHandler( configuration.getTask(),
		                                                    Project.MSG_INFO,
		                                                    Project.MSG_WARN) );
		
		runner.setCommandline( commandline.getCommandline() );

		// run the command
		try
		{
			String extraInfo = debug ? "(debug)" : "(release)";
			task.log( "Starting Link "+extraInfo );
			task.log( commandline.toString(), Project.MSG_DEBUG );

			int exitValue = runner.execute();
			if( exitValue != 0 )
				throw new BuildException( "Link Failed, (exit value: " + exitValue + ")" );
		}
		catch( IOException e )
		{
			String msg = "There was a problem running the linker, this usually occurs when the " +
			             "linker can't be found, make sure it is on your path. full error: " +
			             e.getMessage();
			throw new BuildException( msg, e );
		}
		
		task.log( "Link complete. Library in directory: " + configuration.getOutputDirectory() );
		task.log( "" ); // a little bit of space
	}

	/**
	 * Generates the linker execution command line including library locations, .o files etc...
	 * 
	 * @param debug Include the debug symbol if it is not already present.
	 */
	private Commandline generateLinkCommand( boolean debug )
	{
		// create the command line in which to store the information
		Commandline commandline = new Commandline();
		commandline.setExecutable( executable );

		/////// output file name ///////
		commandline.createArgument().setValue( "-o" );
		commandline.createArgument().setFile( helper.getPlatformSpecificOutputFile(debug) );
		
		////////////////////////////////////
		/////// object files to link ///////
		////////////////////////////////////
		File buildDirectory = configuration.getTempDirectory( debug );
		for( File ofile : helper.getFilesThatNeedLinking(buildDirectory) )
		{
			commandline.createArgument().setFile( ofile );
		}
		
		/////// output file type ///////
		if( configuration.getOutputType() == OutputType.SHARED )
		{
			if( Platform.getOsPlatform().isMac() )
				commandline.createArgument().setValue( "-dynamiclib" );
			else if( Platform.getOsPlatform().isLinux() )
				commandline.createArgument().setValue( "-shared" );
		}
		
		/////// library search paths /////// 
		for( Library library : configuration.getLibraries() )
		{
			if( library.getPath() != null )
			{
				for( String path : library.getPath().list() )
					commandline.createArgument().setLine( "-L" + Commandline.quoteArgument(path) );
			}
		}
		
		/////// libraries to link with /////// 
		for( Library library : configuration.getLibraries() )
		{
			if( library.getLibs().isEmpty() == false )
			{
				for( String temp : library.getLibs() )
					commandline.createArgument().setLine( "-l" + temp );
			}
		}

		/////// additional args ///////
		String[] commands = Commandline.translateCommandline( configuration.getLinkerArgs() );
		commandline.addArguments( commands );

		// add in the debug command if it is not already present
		if( debug )
		{
			if( StringUtilities.arrayContains(commands, "-g") == false )
				commandline.createArgument().setValue( "-g" );
		}

		// return the finished product!
		return commandline;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------

}
