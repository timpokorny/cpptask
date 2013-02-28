/*
 *   Copyright 2013 The Portico Project
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
package org.portico.ant.tasks.cpptask;

import java.io.File;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.portico.ant.tasks.utils.Arch;


/**
 * Example of Task usage:
 * 
 *  <cpptask outfile=""         // (required) NAME of the file to produce. Extension auto appended. 
 *           workdir=""         // (required) Directory for working files.
 *           objdir=""          // Directory for compiled artefacts. Defaults to workdir/complete. 
 *           type=""            // Output type: shared, static or executable
 *           arch=""            // Output architecture: x86 or amd64 (defaults to same as OS)
 *           compiler=""        // Compiler to use: gcc, g++, vc8, vc9, vc10, ... )
 *           compilerArgs=""    // Additional compiler arguments
 *           linkerArgs=""      // Additional linker arguments
 *           preCommand=""      // Command to run before the compile and link commands
 *           incremental=""     // Use incremental compilation and linking? (defaults to true)
 *           failOnError=""     // Fail the build on an error (defaults to true)
 *  >
 *      <fileset...>                // fileset that contains source code
 *      <includepath path=""/>      // paths for include files (can have many)
 *      <define name=""/>           // compiler symbol definition
 *      
 *      <library libs=""/>          // names of libraries to link with
 *      <library path=""/>          // paths to library file to link with
 *      <library path="" libs=""/>  // combination of the above
 *  </cpptask>
 *
 */
public class CppTask extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private BuildConfiguration configuration;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void init()
	{
		this.configuration = new BuildConfiguration( this );
	}

	public void execute()
	{
		// log the current values
		logValues();
		
		// validate the input as best we can at this point - we will have some things
		// we can check, but compiler-specific stuff will have to be delayed until later
		this.configuration.validateConfiguration();
		
		// create the compiler class and pass ourselves to it for processing
		Compiler theCompiler = CompilerType.newInstance( configuration.getCompilerType() );
		
		// pass the information to the compiler so it can do its thing
		try
		{
			theCompiler.runCompiler( configuration );
		}
		catch( BuildException be )
		{
			if( configuration.isFailOnError() )
			{
				throw be;
			}
			else
			{
				log( "Build Failed: " + be.getMessage(), Project.MSG_ERR );
			}
		}
	}

	/**
	 * Logs the current state of the task at DEBUG level
	 */
	private void logValues()
	{
		log( "compiler      : " + configuration.getCompilerType(), Project.MSG_VERBOSE );
		log( "preCommand    : " + configuration.getPreCommand(), Project.MSG_VERBOSE );
		log( "compiler args : " + configuration.getCompilerArgs(), Project.MSG_VERBOSE );
		log( "linker args   : " + configuration.getLinkerArgs(), Project.MSG_VERBOSE );
		log( "outfile       : " + configuration.getOutputName(), Project.MSG_VERBOSE );
		log( "workdir       : " + configuration.getWorkingDirectory(), Project.MSG_DEBUG );
		log( "outdir        : " + configuration.getOutputDirectory(), Project.MSG_DEBUG );
		log( "type          : " + configuration.getOutputType(), Project.MSG_VERBOSE );
		log( "arch          : " + configuration.getOutputArch(), Project.MSG_DEBUG );
		log( "incremental   : " + configuration.isIncremental(), Project.MSG_VERBOSE );
		log( "failOnError   : " + configuration.isFailOnError(), Project.MSG_VERBOSE );

		log( "source to be compiled:", Project.MSG_VERBOSE );
		for( FileSet set : configuration.getSourceFiles() )
		{
			for( File file : Utilities.listFiles(set) )
				log( "  -> " + file, Project.MSG_VERBOSE );
		}

		log( "include paths: ", Project.MSG_VERBOSE );
		for( IncludePath path : configuration.getIncludePaths() )
		{
			if( path.getPath() != null )
			{
				for( String temp : path.getPath().list() )
					log( "  -> " + temp, Project.MSG_VERBOSE );
			}
		}
		
		log( "symbol definitions: ", Project.MSG_VERBOSE );
		for( Define define : configuration.getDefines() )
		{
			log( "  -> " + define.getName(), Project.MSG_VERBOSE );
		}
		
		log( "library search path: ", Project.MSG_VERBOSE );
		for( Library library : configuration.getLibraries() )
		{
			if( library.getPath() != null )
			{
				for( String temp : library.getPath().list() )
					log( "  -> " + temp, Project.MSG_VERBOSE );
			}
		}
		
		log( "libraries to link with: ", Project.MSG_VERBOSE );
		for( Library library : configuration.getLibraries() )
		{
			if( library.getLibs().isEmpty() == false )
			{
				for( String temp : library.getLibs() )
					log( "  -> " + temp, Project.MSG_VERBOSE );
			}
		}
	}

	public BuildConfiguration getBuildConfiguration()
	{
		return this.configuration;
	}

	/////////////////////////////////////////////////////////////////////////////////
	///////////////////////// Attribute Get and Set Methods /////////////////////////
	/////////////////////////////////////////////////////////////////////////////////

	////////////////////////////
	///// Output Arguments /////
	////////////////////////////
	/**
	 * Sets the name of the output file to build
	 */
	public void setOutfile( String outputName )
	{
		configuration.setOutputName( outputName );
	}
	
	/**
	 * Set the location to put all the working files (the task will build
	 * a hierarchy under this directory). Defaults to "[outputDirectory]/working"
	 */
	public void setWorkdir( File workingDirectory )
	{
		configuration.setWorkingDirectory( workingDirectory );
	}

	/**
	 * Set the location to dump the completed files
	 */
	public void setOutdir( File outputDirectory )
	{
		configuration.setOutputDirectory( outputDirectory );
	}

	/**
	 * Sets the output type that should be build (shared library, executable...)
	 */
	public void setType( OutputTypeAntEnum type )
	{
		configuration.setOutputType( OutputType.valueOf(type.getValue().toUpperCase()) );
	}

	/**
	 * Sets the output type of the library or exe we are building.
	 */
	public void setArch( OutputArchAntEnum arch )
	{
		configuration.setOutputArch( Arch.valueOf(arch.getValue().toLowerCase()) );
	}

	/////////////////////////////
	///// Compiler Settings /////
	/////////////////////////////
	/**
	 * Sets the compiler to use for this task. Valid values come from the {@link Compiler#COMPILERS}
	 * variable.
	 */
	public void setCompiler( CompilerAntEnum compiler )
	{
		try
		{
			configuration.setCompilerType( CompilerType.fromString(compiler.getValue()) );
		}
		catch( Exception e )
		{
			throw new BuildException( e );
		}
	}
	
	/**
	 * Set some additional arguments to use when compiling (these are just passed straight
	 * to the compiler)
	 */
	public void setCompilerArgs( String args )
	{
		configuration.setCompilerArgs( args );
	}

	/**
	 * Set some additional arguments to use when linking (these are just passed straight
	 * to the linker)
	 */
	public void setLinkerArgs( String args )
	{
		configuration.setLinkerArgs( args );
	}

	/////////////////////////////
	///// Runtime Arguments /////
	/////////////////////////////
	/**
	 * Sets the command to execute before compilation and/or linking.
	 */
	public void setPreCommand( String preCommand )
	{
		configuration.setPreCommand( preCommand );
	}

	/**
	 * If set to <code>true</code>, the task should only try to build files that have been
	 * changed since the last build.
	 */
	public void setIncremental( boolean incremental )
	{
		configuration.setIncremental( incremental );
	}

	/**
	 * If set to <code>true</code>, the task should fail the build if there is a compile error
	 */
	public void setFailOnError( boolean failOnError )
	{
		configuration.setFailOnError( failOnError );
	}

	//////////////////////////////////////////////////////////////////////////////////
	/////////////////////////// Nested Element Set Methods ///////////////////////////
	//////////////////////////////////////////////////////////////////////////////////
	/**
	 * Add a FileSet that contains some source code to compile.
	 */
	public void addFileset( FileSet fileset )
	{
		configuration.addSourceFiles( fileset );
	}

	/**
	 * Add an path that will be used for the includes in the compile.
	 */
	public void addIncludePath( IncludePath includePath )
	{
		configuration.addIncludePaths( includePath );
	}

	/**
	 * Add a symbol definition that will be used in the compile.
	 * 
	 * Define elements are specified in the ant build with a single "name" attribute.
	 * This attribute can be the symbol to define, or it can contain multiple symbol
	 * definitions separated by a ",". If there is a "," present, this method will
	 * split all the defines up and create an individual entry for each.
	 */
	public void addDefine( Define define )
	{
		configuration.addDefines( define );
	}

	/**
	 * Add a library definition that can specify either a path to be used in
	 * library search path (-Lpath for gcc) or the name of a library that should
	 * be used during the compile/link (-llibrary for gcc)
	 */
	public void addLibrary( Library library )
	{
		configuration.addLibraries( library );
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------

	////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////// Enumerated Types ///////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * The ant enumeration for specifying the valid compiler values.
	 */
	public static class CompilerAntEnum extends EnumeratedAttribute
	{
		public String[] getValues()
		{
			//return new String[]{ "gcc", "g++", "msvc" };
			ArrayList<String> values = new ArrayList<String>();
			for( CompilerType type : CompilerType.values() )
				values.add( type.toString().toLowerCase() );
			
			return values.toArray( new String[0] );
		}
	}

	/**
	 * The ant enumeration specifying the type of program to build (shared library or
	 * standalone executable).
	 */
	public static class OutputTypeAntEnum extends EnumeratedAttribute
	{
		public String[] getValues()
		{
			ArrayList<String> values = new ArrayList<String>();
			for( OutputType type : OutputType.values() )
				values.add( type.toString().toLowerCase() );
			
			return values.toArray( new String[0] );
		}
	}

	/**
	 * Ant enumeration to specify the valid values for the output architecture type.
	 */
	public static class OutputArchAntEnum extends EnumeratedAttribute
	{
		public String[] getValues()
		{
			ArrayList<String> values = new ArrayList<String>();
			for( Arch arch : Arch.values() )
				values.add( arch.toString().toLowerCase() );
			
			return values.toArray( new String[0] );
		}
	}
}
