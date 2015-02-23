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
package org.portico.ant.tasks.cpptask.msvc;

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
import org.portico.ant.tasks.cpptask.BuildConfiguration;
import org.portico.ant.tasks.cpptask.BuildHelper;
import org.portico.ant.tasks.cpptask.Compiler;
import org.portico.ant.tasks.cpptask.Define;
import org.portico.ant.tasks.cpptask.IncludePath;
import org.portico.ant.tasks.cpptask.Library;
import org.portico.ant.tasks.cpptask.OutputType;
import org.portico.ant.tasks.utils.Arch;
import org.portico.ant.tasks.utils.StringUtilities;


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
		
		// do this thing
		compile();
		link();
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// Compiler Methods ////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Execute the actual compilation step.
	 */
	private void compile()
	{
		task.log( "Starting Compile" );

		// Start the compilation, splitting it into two steps: one for source and one for resources
		/////////////////////////////////////////////////
		// 1. Get a list of all files to compile       //
		//    Run all checks for incremental compiling //
		/////////////////////////////////////////////////
		// 1. Get a list of all files to compile (considering checks for incremental compiling)
		File objectDirectory = configuration.getObjectDirectory();
		File[] filesToCompile = helper.getFilesThatNeedCompiling( objectDirectory );
		// make sure we have files to compile!
		if( filesToCompile.length == 0 )
		{
			task.log( "Skipping Compile: Up to date" );
			return;
		}
		
		////////////////////////////////////////////////////////////////////////////////////
		// 2. Split the returned set of files into "resource files" and "everything else" //
		////////////////////////////////////////////////////////////////////////////////////
		ArrayList<File> sourceFiles = new ArrayList<File>();
		ArrayList<File> resourceFiles = new ArrayList<File>();
		for( File file : filesToCompile )
		{
			if( file.getName().endsWith(".rc") )
				resourceFiles.add( file );
			else
				sourceFiles.add( file );
		}

		//////////////////////////////////////////////////
		// 4. If we have any source files, compile them //
		//////////////////////////////////////////////////
		if( sourceFiles.isEmpty() == false )
			compileSourceFiles( sourceFiles, objectDirectory );

		////////////////////////////////////////////////////
		// 5. If we have any resource files, compile them //
		////////////////////////////////////////////////////
		if( resourceFiles.isEmpty() == false )
			compileResourceFiles( resourceFiles, objectDirectory );

		task.log( "Compile complete" );
	}

	/***********************************************************************/
	/*********************** Source Compiler Methods ***********************/
	/***********************************************************************/
	/**
	 * This task will generate and execute the compile command for all located source files.
	 */
	private void compileSourceFiles( ArrayList<File> files, File objectDirectory )
	{
		task.log( "" + files.size() + " files to be compiled." );

		// generate the command base
		Commandline command = generateCompileCommand();

		// generate the argument list to put into the response file
		List<String> arguments = new ArrayList<String>();
		arguments.addAll( generateCompileCommandOptions() );
		arguments.addAll( StringUtilities.filesToStrings(files) );

		// create the response file which has all the compile information
		File responseFile = createResponseFile( "compile-files", objectDirectory, arguments );

		// put the response file on the end of the command line
		command.createArgument().setValue( "@"+responseFile.getAbsolutePath() );
		
		// prepend the pre-command (if there is one) and run this thing
		Execute runner = new Execute( new LogStreamHandler(configuration.getTask(),
		                                                   Project.MSG_INFO,
		                                                   Project.MSG_WARN) );
		
		runner.setCommandline( prependEnvironment(command.getCommandline()) );
		runner.setWorkingDirectory( objectDirectory );
		try
		{
			// log what we're doing
			task.log( "Starting compile" );
			task.log( "Running compile command: ", Project.MSG_VERBOSE );
			for( String argument : runner.getCommandline() )
				task.log( argument, Project.MSG_VERBOSE );

			int exitValue = runner.execute();
			if( exitValue != 0 )
				throw new BuildException( "Compile Failed, (exit value: " + exitValue + ")" );
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
	}

	/**
	 * Generates the command that will be used for the compile of each relevant file.
	 * This is just the extra stuff that doesn't include the name of the file being compiled.
	 */
	private Commandline generateCompileCommand()
	{
		// create the working directories if they don't already exist
		configuration.getObjectDirectory().mkdirs();

		// create the command line
		Commandline commandline = new Commandline();
		commandline.setExecutable( "cl" );
		commandline.createArgument().setValue( "/c" );
		commandline.createArgument().setValue( "/nologo" );

		// moved most of these into generateCompileCommandOptions so they can go in a reponse file
		return commandline;
	}

	/**
	 * Generates a bunch of arguments for cl.exe to go in the response file. It is nice
	 * to have everything in a response file so that it is recorded cleanly for debugging.
	 */
	private List<String> generateCompileCommandOptions()
	{
		ArrayList<String> options = new ArrayList<String>();
		
		/////// additional args ////////
		// do this up front
		String[] commands = Commandline.translateCommandline( configuration.getCompilerArgs() );
		for( String command : commands )
			options.add( command );
		
		////// includes //////
		for( IncludePath path : configuration.getIncludePaths() )
		{
			// make sure there is a path
			if( path.getPath() != null )
			{
				// add each path element to the line
				for( String temp : path.getPath().list() )
					options.add( "/I"+temp );
			}
		}
		
		////// defines ///////
		for( Define define : configuration.getDefines() )
		{
			options.add( "/D" + define.getName() );
		}
		
		return options;
	}

	/**
	 * Creates a new response file for use in compiling or linking and populates it with the
	 * given commands.
	 * 
	 * @param name The name of the response file to create (excluding the extension)
	 * @param directory The directory to put the response file in
	 * @param commands The commands to write to the file
	 */
	private File createResponseFile( String name, File directory, List<String> commands )
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

	/***********************************************************************/
	/********************** Resource Compiler Methods **********************/
	/***********************************************************************/
	/**
	 * This task will generate and execute the compile command for all located resource files.
	 */
	private void compileResourceFiles( ArrayList<File> files, File objectDirectory )
	{
		task.log( "" + files.size() + " resource files to be compiled." );

		// append all the resource file names to the end of the command line
		for( File file : files )
		{
			// generate the command base
			Commandline command = generateResourceCompileCommand( file, objectDirectory );
			command.createArgument().setValue( file.getAbsolutePath() );

			// prepend the pre-command (if there is one) and run this thing
			Execute runner = new Execute( new LogStreamHandler(configuration.getTask(),
			                                                   Project.MSG_INFO,
			                                                   Project.MSG_WARN) );
			
			runner.setCommandline( prependEnvironment(command.getCommandline()) );
			runner.setWorkingDirectory( objectDirectory );
			try
			{
				// log what we're doing
				task.log( "Starting resource compile" );
				task.log( "Running resource compile command: ", Project.MSG_VERBOSE );
				for( String argument : runner.getCommandline() )
					task.log( argument, Project.MSG_VERBOSE );

				int exitValue = runner.execute();
				if( exitValue != 0 )
					throw new BuildException( "Resource Compile Failed, (exit value: "+exitValue+")" );
			}
			catch( IOException e )
			{
				// most likely, the compiler isn't on the path and can't be found. print out
				// kill the build with a nicer error
				String msg = "There was a problem running the compiler, this usually occurs when " +
				             "windows can't find the compiler (rc.exe), make sure it is on your path." +
				             " full error: " + e.getMessage();
				throw new BuildException( msg, e );
			}
		}
	}

	/**
	 * Generates the command that will be used for the compile of each relevant resource file.
	 * This is just the extra stuff that doesn't include the name of the file being compiled.
	 */
	private Commandline generateResourceCompileCommand( File file, File outputDirectory )
	{
		// create the working directories if they don't already exist
		configuration.getObjectDirectory().mkdirs();

		// create the command line
		Commandline commandline = new Commandline();
		commandline.setExecutable( "rc" );
		//commandline.createArgument().setValue( "/nologo" ); -- doesn't work with vc8?

		//////// output file ////////
		File outputFile = new File( outputDirectory, file.getName() );
		outputFile = StringUtilities.changeExtension( outputFile, ".res" );
		commandline.createArgument().setValue( "/fo\""+outputFile.getAbsolutePath()+"\"" );

		/////// additional args ////////
		// do this up front
		//String[] commands = Commandline.translateCommandline( configuration.getCompilerArgs() );
		//commandline.addArguments( commands );
		
		////// includes //////
		for( IncludePath path : configuration.getIncludePaths() )
		{
			// make sure there is a path
			if( path.getPath() != null )
			{
				// add each path element to the line
				for( String temp : path.getPath().list() )
					commandline.createArgument().setLine( "/i" + Commandline.quoteArgument(temp) );
			}
		}
		
		////// defines ///////
		for( Define define : configuration.getDefines() )
		{
			commandline.createArgument().setLine( "/d" + define.getName() );
		}
		
		return commandline;
	}

	/***********************************************************************/
	/********************** Environment Setup Methods **********************/
	/***********************************************************************/
	/**
	 * This method takes the base command line and prepends to it:
	 * <ul>
	 *   <li>The Visual Studio environment setup calls (vcvarsall.bat)</li>
	 *   <li>Any pre-commands coming from the task</li>
	 * </ul>
	 * 
	 * This is used to ensure that the VC compiler executables can be found and that
	 * any cross-compiler options are set up.
	 * 
	 * @param baseCommand The base command line
	 * @return The updated command line
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
		// generate the base command line
		// object and library files done in a separate response file
		Commandline commandline = generateLinkCommand();

		// generate the argument list to put into the response file
		List<String> arguments = new ArrayList<String>();
		arguments.addAll( generateLinkCommandOptions() );
		arguments.addAll( getFilesAndLibrariesToLinkWith() );
		
		// build the response file with the object and library files to link with
		File responseFile = createResponseFile( "linker-files",
		                                        configuration.getObjectDirectory(),
		                                        arguments );
		
		// put the response file on the end of the command
		commandline.createArgument().setValue( "@"+responseFile.getAbsolutePath() );
		
		// create the execution object
		Execute runner = new Execute( new LogStreamHandler( configuration.getTask(),
		                                                    Project.MSG_INFO,
		                                                    Project.MSG_WARN) );
		
		runner.setCommandline( prependEnvironment(commandline.getCommandline()) );
		runner.setWorkingDirectory( configuration.getObjectDirectory().getParentFile() );

		// run the command
		try
		{
			// log what we're doing
			task.log( "Starting Link" );
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
	private Commandline generateLinkCommand()
	{
		// create the command line in which to store the information
		Commandline commandline = new Commandline();
		
		if( configuration.getOutputType() == OutputType.STATIC )
			commandline.setExecutable( "lib" );
		else
			commandline.setExecutable( "link" );
		
		// moved most of these to generateLinkCommandOptions
		return commandline;
	}

	/**
	 * Generates a bunch of arguments for link.exe to go in the response file. It is nice to have
	 * everything in a response file so that it is recorded cleanly for debugging.
	 */
	private ArrayList<String> generateLinkCommandOptions()
	{
		ArrayList<String> commands = new ArrayList<String>();
		
		if( configuration.getOutputType() == OutputType.STATIC )
		{
			commands.add( "/NOLOGO" );
			commands.add( "/OUT:" + helper.getPlatformSpecificOutputFile() );
			
			return commands;
		}
		
		
		/////// output options ///////
		commands.add( "/NOLOGO" );
		
		commands.add( "/SUBSYSTEM:CONSOLE" );
		//commands.add( "/INCREMENTAL:NO" );
		
		// figure out the output architecture and turn it into the approrpiate string
		String targetArch = configuration.getOutputArch()==Arch.x86 ? "X86" : "X64";
		commands.add( "/MACHINE:"+targetArch );
		
		/////// output file name ///////
		commands.add( "/OUT:" + helper.getPlatformSpecificOutputFile() );
		
		/////// output file type ///////
		if( configuration.getOutputType() == OutputType.SHARED )
			commands.add( "/DLL" );

		return commands;
	}
	
	/**
	 * Get an list of all the paths for files and libraries that we need to link with. These
	 * paths will be added to the response file that will be used as input for the link command.
	 */
	private List<String> getFilesAndLibrariesToLinkWith()
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
		
		List<String> returnList = new ArrayList<String>();
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
					returnList.add( possible.getAbsolutePath() );
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
					returnList.add( possible.getAbsolutePath() );
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
			returnList.add( temp );

		////////////////////////////////////
		/////// object files to link ///////
		////////////////////////////////////
		for( File ofile : helper.getFilesThatNeedLinking(configuration.getObjectDirectory()) )
		{
			returnList.add( ofile.getAbsolutePath() );
		}
		
		// return the finished product!
		return returnList;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
