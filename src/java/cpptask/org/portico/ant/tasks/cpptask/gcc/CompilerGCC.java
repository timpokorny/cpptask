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
package org.portico.ant.tasks.cpptask.gcc;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.portico.ant.tasks.cpptask.BuildConfiguration;
import org.portico.ant.tasks.cpptask.BuildHelper;
import org.portico.ant.tasks.cpptask.Compiler;
import org.portico.ant.tasks.cpptask.CppTask;
import org.portico.ant.tasks.cpptask.Define;
import org.portico.ant.tasks.cpptask.IncludePath;
import org.portico.ant.tasks.cpptask.Library;
import org.portico.ant.tasks.cpptask.OutputType;
import org.portico.ant.tasks.utils.Arch;
import org.portico.ant.tasks.utils.Platform;


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
		
		// do that wild thing! and compile, probably link as well
		compile();
		link();
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// Compiler Methods ////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Execute the actual compilation for each of the given files.
	 */
	private void compile()
	{
		// let everyone know what we're doing
		task.log( "Starting Compile " );

		// generate the command line
		Commandline command = generateCompileCommand();

		// get all the files that we should compile
		// this will run checks for things like incremental compiling
		File objectDirectory = configuration.getObjectDirectory();
		File[] filesToCompile = helper.getFilesThatNeedCompiling( objectDirectory );
		task.log( "" + filesToCompile.length + " files to be compiled." );

		// Do the compile
		// We have to support parallel builds by ourselves, so we throw a bunch of compile tasks
		// into a queue and process with an executor and then wait for them all to finish
		ExecutorService executor = Executors.newFixedThreadPool( configuration.getThreadCount() );
		
		// create a runnable task for the compilation of each file and submit it
		for( File sourceFile : filesToCompile )
			executor.submit( new CompileTask(sourceFile,objectDirectory,command) );

		// run the executor over the queue
		executor.shutdown();
		while( executor.isTerminated() == false )
		{
			try
			{
				executor.awaitTermination( 500, TimeUnit.MILLISECONDS );
			}
			catch( InterruptedException ie )
			{ /* just carry on */ }
		}
		
		task.log( "Compile complete" );
	}

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
		
		////// platform architecture //////
		appendCompilerArchitecture( commandline );
		
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
	 * Return the type of compiler we need to use. Calculated as follows:
	 * <ul>
	 *   <li>If we are on a 32-bit system and building a 32-bit target: return "-arch i386" (mac) or "-m32"</li>
	 *   <li>If we are on a 64-bit system and building a 64-bit target: return amd64</li>
	 *   <li>If we are on a 32-bit system and building a 64-bit target: return x86_amd64</li>
	 * </ul>
	 */
	private void appendCompilerArchitecture( Commandline commandline )
	{
		Arch osArch = Arch.getOsArch();
		Arch outArch = configuration.getOutputArch();
		
		// warn them if they're trying to do a 64-bit cross-compile
		if( (osArch == Arch.x86) && (outArch == Arch.amd64) )
			debug( "Building 64-bit target on 32-bit system. Must have multilib installed." );
		
		// generate the appropriate compile command line argument
		if( outArch == Arch.x86 )
		{
			debug( "(system: "+osArch+") Build target arch: x86" );
			if( Platform.getOsPlatform().isLinux() )
			{
				commandline.createArgument().setValue( "-m32" );
			}
			else if( Platform.getOsPlatform().isMac() )
			{
				commandline.createArgument().setValue( "-arch" );
				commandline.createArgument().setValue( "i386" );
			}
		}
		else
		{
			debug( "(system: "+osArch+") Build target arch: amd64" );
			if( Platform.getOsPlatform().isLinux() )
			{
				commandline.createArgument().setValue( "-m64" );
			}
			else if( Platform.getOsPlatform().isMac() )
			{
				commandline.createArgument().setValue( "-arch" );
				commandline.createArgument().setValue( "x86_64" );
			}
		}
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
			task.log( "Starting Link " );
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
	 */
	private Commandline generateLinkCommand()
	{
		// Creating a static library in gcc/clang uses ar instead of ld via gcc.
		// The command line for ar is different, it has been delegated into its own function
		if( configuration.getOutputType() == OutputType.STATIC )
			return generateStaticLinkCommand();
		
		// create the command line in which to store the information
		Commandline commandline = new Commandline();
		commandline.setExecutable( executable );

		/////// output file name ///////
		File outputFile = helper.getPlatformSpecificOutputFile();
		commandline.createArgument().setValue( "-o" );
		commandline.createArgument().setFile( outputFile );
		
		////////////////////////////////////
		/////// object files to link ///////
		////////////////////////////////////
		File objectDirectory = configuration.getObjectDirectory();
		for( File ofile : helper.getFilesThatNeedLinking(objectDirectory) )
		{
			commandline.createArgument().setFile( ofile );
		}
		
		/////// output file type ///////
		if( configuration.getOutputType() == OutputType.SHARED )
		{
			if( Platform.getOsPlatform().isMac() )
			{
				commandline.createArgument().setValue( "-dynamiclib" );
				// add the rpath setting if one isn't present
				if( configuration.getLinkerArgs().contains("install_name") == false )
					commandline.createArgument().setLine( "-install_name @rpath/"+outputFile.getName() );
			}
			else if( Platform.getOsPlatform().isLinux() )
			{
				commandline.createArgument().setValue( "-shared" );
			}
		}
		

		////// platform architecture //////
		appendCompilerArchitecture( commandline );

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
	
	private Commandline generateStaticLinkCommand()
	{
		// gcc/clang create static libraries through the "ar" utility 
		Commandline commandline = new Commandline();
		commandline.setExecutable( "ar" );
		
		// r=Replace, c=Create, s=Write Index
		commandline.createArgument().setValue( "rcs" );
		
		/////// output file name ///////
		File outputFile = helper.getPlatformSpecificOutputFile();
		commandline.createArgument().setFile( outputFile );
		
		////////////////////////////////////
		/////// object files to link ///////
		////////////////////////////////////
		File objectDirectory = configuration.getObjectDirectory();
		for( File ofile : helper.getFilesThatNeedLinking(objectDirectory) )
		{
			commandline.createArgument().setFile( ofile );
		}

		/////// additional args ///////
		String[] commands = Commandline.translateCommandline( configuration.getLinkerArgs() );
		commandline.addArguments( commands );

		// return the finished product!
		return commandline;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////// Private Helper Methods /////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	private void debug( String message )
	{
		task.log( message, Project.MSG_DEBUG );
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// Private Inner Class: CompileTask ////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	private class CompileTask implements Runnable
	{
		private File sourceFile;
		private File objectDirectory;
		private Commandline command;

		public CompileTask( File sourceFile, File objectDirectory, Commandline command )
		{
			this.sourceFile = sourceFile;
			this.objectDirectory = objectDirectory;
			this.command = command;
		}
		
		public void run()
		{
			Commandline theCommand;
			// get the name of the output file
			File ofile = helper.getOFile( objectDirectory, sourceFile );
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
	}

	
	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------

}
