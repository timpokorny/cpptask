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
package com.lbf.cpptask.gcc;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;

import com.lbf.cpptask.BuildConfiguration;
import com.lbf.cpptask.Compiler;
import com.lbf.cpptask.CppTask;
import com.lbf.cpptask.Define;
import com.lbf.cpptask.IncludePath;
import com.lbf.cpptask.Library;
import com.lbf.cpptask.OutputType;
import com.lbf.cpptask.Utilities;

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
	
	public void compile( BuildConfiguration configuration ) throws BuildException
	{
		// extract the necessary information
		this.configuration = configuration;
		this.task = configuration.getTask();
		
		// create the command line that will be used for each compile
		// this is just the part of it that doesn't include the file being compiled
		Commandline command = generateCompileCommand();

		// run the compile
		compile( command );
		
		// run the linker
		if( configuration.getOutputFile() != null )
			link();
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// Compiler Methods ////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Generates the command that will be used for the compile of each relevant file.
	 * This is just the extra stuff that doesn't include the name of the file being compiled.
	 */
	private Commandline generateCompileCommand()
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
		
		return commandline;
	}

	/**
	 * Execute the actual compilation for each of the given files, using the command line
	 * that is provided. The command line information is NOT the full command line, but rather,
	 * just the stuff that will be used when compiling each file.
	 */
	private void compile( Commandline command )
	{
		// get all the files that we should compile
		// this will run checks for things like incremental compiling
		File[] filesToCompile = Utilities.getFilesToCompile( configuration, task );
		task.log( "" + filesToCompile.length + " files to be compiled." );

		// do the compilation
		for( File sourceFile : filesToCompile )
		{
			Commandline theCommand;
			// get the name of the output file
			File ofile = Utilities.getOFile( configuration.getObjectDirectory(), sourceFile );

			if(sourceFile.getName().endsWith(".rc"))
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

	//////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Linker Methods /////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * This method is the main manager of the linking process. It should only be run if an
	 * "outfile" has been provided in the configuration. It will attempt to link all the
	 * files in the objdir into a simple executable/library.
	 */
	private void link()
	{
		// generate the command line
		Commandline commandline = generateLinkCommand();

		// create the execution object
		Execute runner = new Execute( new LogStreamHandler( configuration.getTask(),
		                                                    Project.MSG_INFO,
		                                                    Project.MSG_WARN) );
		
		runner.setCommandline( commandline.getCommandline() );

		// run the command
		try
		{
			task.log( "Starting link" );
			task.log( commandline.toString(), Project.MSG_DEBUG );

			int exitValue = runner.execute();
			if( exitValue != 0 )
			{
				throw new BuildException( "Link Failed, (exit value: " + exitValue + ")" );
			}
		}
		catch( IOException e )
		{
			String msg = "There was a problem running the linker, this usually occurs when the " +
			             "linker can't be found, make sure it is on your path. full error: " +
			             e.getMessage();
			throw new BuildException( msg, e );
		}
		
		task.log( "Link complete: " + configuration.getOutputFile() );
	}

	/**
	 * Generates the linker execution command line including library locations, .o files etc...
	 */
	private Commandline generateLinkCommand()
	{
		// create the command line in which to store the information
		Commandline commandline = new Commandline();
		commandline.setExecutable( executable );

		/////// output file name ///////
		commandline.createArgument().setValue( "-o" );
		commandline.createArgument().setFile( Utilities.getLibraryFile(configuration) );
		
		////////////////////////////////////
		/////// object files to link ///////
		////////////////////////////////////
		for( File ofile : Utilities.getOFilesForLinking(configuration) )
		{
			commandline.createArgument().setFile( ofile );
		}
		
		/////// output file type ///////
		if( configuration.getOutputType() == OutputType.SHARED )
		{
			if( Utilities.MACOS )
				commandline.createArgument().setValue( "-dynamiclib" );
			else if( Utilities.LINUX )
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
		
		// return the finished product!
		return commandline;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------

}
