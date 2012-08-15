package com.lbf.cpptask;
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

import java.io.File;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;

/**
 * Example of Task usage:
 * 
 *  <cpptask failonerror=""
 *           outfile=""        // output file name
 *           compiler=""       // name of the compiler to use
 *           compilerExe=""    // location of the executable to use (only for MSVC)
 *           linkerExe=""      // location of the linker executable to use (only for MSVC)
 *           outtype=""        // shared, executable
 *           outarch=""        // x86 or amd64
 *           objdir=""         // directory to store object files in
 *           incremental=""    // should things be compiled incrementally
 *           compilerArgs=""   // additional arguments that should be used for each compile
 *           linkerArgs=""     // additional arguments that should be used for linking
 *  >
 *
 *      <fileset...>                 // fileset that contains source code
 *      <includepath path=""/>
 *      <define name=""/>
 *      
 *      <library path=""/>
 *      <library libs=""/>
 *      <library path="" libs=""/>   //
 *      
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
		
		// create the compiler class and pass ourselves to it for processing
		Compiler theCompiler = CompilerType.newInstance( configuration.getCompilerType() );
		
		// pass the information to the compiler so it can do its thing
		try
		{
			theCompiler.compile( configuration );
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
		log( "outputType    : " + configuration.getOutputType(), Project.MSG_VERBOSE );
		log( "outputArch    : " + configuration.getOutputArch(), Project.MSG_DEBUG );
		log( "objectDir     : " + configuration.getObjectDirectory(), Project.MSG_VERBOSE );
		log( "outputFile    : " + configuration.getOutputFile(), Project.MSG_VERBOSE );
		log( "failOnError   : " + configuration.isFailOnError(), Project.MSG_VERBOSE );
		log( "incremental   : " + configuration.isIncremental(), Project.MSG_VERBOSE );
		log( "compiler args : " + configuration.getCompilerArgs(), Project.MSG_VERBOSE );
		log( "linker args   : " + configuration.getLinkerArgs(), Project.MSG_VERBOSE );

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
	 * Sets the command to execute before compilation and/or linking.
	 */
	public void setPreCommand( String preCommand )
	{
		configuration.setPreCommand( preCommand );
	}

	/**
	 * Sets the output type that should be build (shared library, executable...)
	 */
	public void setOutType( OutputTypeAntEnum type )
	{
		configuration.setOutputType( OutputType.valueOf(type.getValue().toUpperCase()) );
	}

	/**
	 * Sets the output type of the library or exe we are building.
	 */
	public void setOutArch( OutputArchAntEnum arch )
	{
		configuration.setOutputArch( Arch.valueOf(arch.getValue().toLowerCase()) );
	}

	/**
	 * If set to <code>true</code>, the task should fail the build if there is a compile error
	 */
	public void setFailOnError( boolean failOnError )
	{
		configuration.setFailOnError( failOnError );
	}

	/**
	 * Sets the location of the file which should be produced.
	 */
	public void setOutfile( File outputFile )
	{
		configuration.setOutputFile( outputFile );
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
	 * Set the location at which to store the object files.
	 */
	public void setObjDir( File objectDirectory )
	{
		configuration.setObjectDirectory( objectDirectory );
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
	 * Add a symbol definition that will be used in the compile
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
	 * Ant enumeration to specify the value values for the output architecture type.
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
