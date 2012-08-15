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
package com.lbf.cpptask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/**
 * The {@link BuildConfiguration} class holds all the information that was extracted from the
 * task by ant. It is used as a basic unit of configuration data that can be passed around.
 * <p/>
 * <b>NOTE:</b> All setXxx() methods will <b>REPLACE THE EXISTING VALUES WITHOUT QUESTION</b>.
 * If you are working on a collection of things (such as {@link Define}s, you can append to the
 * current colleciton with the addXxx() methods.
 */
public class BuildConfiguration
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private CppTask task;
	
	private CompilerType compilerType;
	private String preCommand;
	private OutputType outputType;
	private Arch outputArch;
	private File objectDirectory;
	private File outputFile;
	private boolean failOnError;
	private boolean incremental;
	private String compilerArgs;
	private String linkerArgs;

	private List<FileSet> sourceFiles;
	private List<IncludePath> includePaths;
	private List<Define> defines;
	private List<Library> libraries;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	public BuildConfiguration( CppTask theTask )
	{
		this.task = theTask;
		
		// initialize the values
		// set defaults
		if( System.getProperty("os.name").toUpperCase().contains("WINDOWS") )
			this.compilerType = CompilerType.VC10;
		else
			this.compilerType = CompilerType.GCC;
		
		this.outputType      = OutputType.EXECUTABLE;
		this.outputArch      = Arch.getOsArch();
		this.objectDirectory = getProject().getBaseDir();
		// this.outputFile   = null;
		this.failOnError     = true;
		this.incremental     = true;
		this.compilerArgs    = "";
		this.linkerArgs      = "";
		
		this.sourceFiles     = new ArrayList<FileSet>();
		this.includePaths    = new ArrayList<IncludePath>();
		this.defines         = new ArrayList<Define>();
		this.libraries       = new ArrayList<Library>();
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	/**
	 * Get the {@link CppTask} this {@link BuildConfiguration} is associated with.
	 */
	public CppTask getTask()
	{
		return this.task;
	}

	/**
	 * Get the Ant Project instance this build configuration is associated with
	 */
	public Project getProject()
	{
		return this.task.getProject();
	}
	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// Basic Get/Set ////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	public CompilerType getCompilerType()
	{
		return compilerType;
	}

	public void setCompilerType( CompilerType compilerType )
	{
		this.compilerType = compilerType;
	}

	public String getPreCommand()
	{
		return this.preCommand;
	}
	
	public void setPreCommand( String preCommand )
	{
		this.preCommand = preCommand;
	}
	
	public OutputType getOutputType()
	{
		return outputType;
	}

	public void setOutputType( OutputType outputType )
	{
		this.outputType = outputType;
	}
	
	public Arch getOutputArch()
	{
		return this.outputArch;
	}
	
	public void setOutputArch( Arch outputArch )
	{
		this.outputArch = outputArch;
	}

	public File getObjectDirectory()
	{
		return objectDirectory;
	}

	public void setObjectDirectory( File objectDirectory )
	{
		this.objectDirectory = objectDirectory;
	}

	public File getOutputFile()
	{
		return outputFile;
	}

	public void setOutputFile( File outputFile )
	{
		this.outputFile = outputFile;
	}

	public boolean isFailOnError()
	{
		return failOnError;
	}

	public void setFailOnError( boolean failOnError )
	{
		this.failOnError = failOnError;
	}

	public boolean isIncremental()
	{
		return incremental;
	}

	public void setIncremental( boolean incremental )
	{
		this.incremental = incremental;
	}

	public String getCompilerArgs()
	{
		return compilerArgs;
	}

	public void setCompilerArgs( String additionalArgs )
	{
		this.compilerArgs = additionalArgs;
	}

	public String getLinkerArgs()
	{
		return linkerArgs;
	}

	public void setLinkerArgs( String additionalArgs )
	{
		this.linkerArgs = additionalArgs;
	}

	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Collections /////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////

	////////// Source Files //////////
	public List<FileSet> getSourceFiles()
	{
		return sourceFiles;
	}

	/**
	 * Replaces the existing collection of filesets.
	 */
	public void setSourceFiles( List<FileSet> sourceFiles )
	{
		this.sourceFiles = sourceFiles;
	}

	/**
	 * Appends all the values in the contained collection to the existing collection
	 * 
	 * @param additional The items to append to the existing collection
	 */
	public void addSourceFiles( List<FileSet> additional )
	{
		this.sourceFiles.addAll( additional );
	}
	
	public void addSourceFiles( FileSet additional )
	{
		this.sourceFiles.add( additional );
	}

	////////// Include Paths //////////
	public List<IncludePath> getIncludePaths()
	{
		return includePaths;
	}

	/**
	 * Replaces the existing collection of {@link IncludePath}s.
	 */
	public void setIncludePaths( List<IncludePath> includePaths )
	{
		this.includePaths = includePaths;
	}

	/**
	 * Appends all the values in the contained collection to the existing collection
	 * 
	 * @param additional The items to append to the existing collection
	 */
	public void addIncludePaths( List<IncludePath> additional )
	{
		this.includePaths.addAll( additional );
	}
	
	public void addIncludePaths( IncludePath additional )
	{
		this.includePaths.add( additional );
	}

	////////// Symbol Defines //////////
	public List<Define> getDefines()
	{
		return defines;
	}

	/**
	 * Replaces the existing collection of {@link Define}s.
	 */
	public void setDefines( List<Define> defines )
	{
		this.defines = defines;
	}

	/**
	 * Appends all the values in the contained collection to the existing collection
	 * 
	 * @param additional The items to append to the existing collection
	 */
	public void addDefines( List<Define> additional )
	{
		this.defines.addAll( additional );
	}
	
	public void addDefines( Define additional )
	{
		this.defines.add( additional );
	}

	////////// Library Definitions //////////
	public List<Library> getLibraries()
	{
		return libraries;
	}

	/**
	 * Replaces the existing collection of {@link Library} objects.
	 */
	public void setLibraries( List<Library> libraries )
	{
		this.libraries = libraries;
	}

	/**
	 * Appends all the values in the contained collection to the existing collection
	 * 
	 * @param additional The items to append to the existing collection
	 */
	public void addLibraries( List<Library> additional )
	{
		this.libraries.addAll( additional );
	}
	
	public void addLibraries( Library additional )
	{
		this.libraries.add( additional );
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
