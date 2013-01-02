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
package com.lbf.tasks.cpptask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import com.lbf.tasks.utils.Arch;
import com.lbf.tasks.utils.Platform;

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

	// Output properties
	private File workingDirectory;
	private File outputDirectory;
	private String outputName;
	private OutputType outputType;
	private Arch outputArch;
	private BuildType buildType;
	
	// Compiler and linker options
	private CompilerType compilerType;
	private String compilerArgs;
	private String linkerArgs;

	// Runtime properties
	private String preCommand;
	private boolean failOnError;
	private boolean incremental;

	// Collection properties
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
		// output types
		this.workingDirectory  = null; // required
		this.outputDirectory   = null; // optional - defaults to ${workingDirectory}/complete
		this.outputName        = null; // required
		this.outputType        = OutputType.EXECUTABLE;
		this.outputArch        = Arch.getOsArch();
		this.buildType         = BuildType.RELEASE;
		
		// Compiler and linker options
		this.compilerArgs = "";
		this.linkerArgs   = "";
		this.compilerType = Platform.getOsPlatform().isWindows() ? CompilerType.VC10 :
		                                                           CompilerType.GCC;

		// Runtime properties
		this.preCommand = "";
		this.incremental = true;
		this.failOnError = true;

		// child types
		this.sourceFiles  = new ArrayList<FileSet>();
		this.includePaths = new ArrayList<IncludePath>();
		this.defines      = new ArrayList<Define>();
		this.libraries    = new ArrayList<Library>();
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
	////////////////////////////// Attribute Setting Methods //////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Checks the build configuration to make sure all the required information has been
	 * provided. If not, a build exception is thrown.
	 */
	public void validateConfiguration() throws BuildException
	{
		if( this.outputName == null )
			throw new BuildException( "The attribute \"outputName\" is required" );
		
		if( this.workingDirectory == null )
			throw new BuildException( "The attribute \"workingDirectory\" is required" );
	}
	
	///////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////// Output Properties //////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	public void setWorkingDirectory( File file )
	{
		this.workingDirectory = file;
	}
	
	public File getWorkingDirectory()
	{
		return this.workingDirectory;
	}
	
	/**
	 * Gets the directory that we should put temporary build files into. Depending on whether
	 * the given debug parameter is true or false, this will be: [workingDirectory]/temp/debug
	 * or [workingDirectory]/temp/release.
	 * <p/>
	 * Note that this method will create the directory if it doesn't already exist. 
	 */
	public File getTempDirectory( boolean debug )
	{
		if( debug )
		{
			File directory = new File( this.workingDirectory, "temp/debug/" );
			directory.mkdirs();
			return directory;
		}
		else
		{
			File directory = new File( this.workingDirectory, "temp/release/" );
			directory.mkdirs();
			return directory;
		}
	}

	/**
	 * Gets the raw temp directory (workingDirectory/temp). Typically you'll want references to
	 * either the debug or release directories underneath here and should use the provided
	 * {@link #getTempDirectory(boolean)} to get this. If you just want the temp directory itself,
	 * then have at it.
	 * <p/>
	 * Note that this method will create the directory if it doesn't already exist. 
	 */
	public File getTempDirectory()
	{
		File directory = new File( this.workingDirectory, "temp/" );
		directory.mkdirs();
		return directory;
	}

	public void setOutputDirectory( File file )
	{
		this.outputDirectory = file;
	}

	public File getOutputDirectory()
	{
		// if we haven't got one, lazy load it as [workingDirectory]/complete
		if( this.outputDirectory == null )
		{
			this.outputDirectory = new File( this.workingDirectory, "complete" );
			this.outputDirectory.mkdirs();
		}
		
		return this.outputDirectory;
	}

	public void setOutputName( String name )
	{
		this.outputName = name;
	}
	
	public String getOutputName()
	{
		return this.outputName;
	}

	/**
	 * Convenience method to fetch the output file as a File
	 * @return
	 */
	public File getOutputFile()
	{
		return new File( getOutputDirectory(), this.outputName );
	}

	public void setOutputType( OutputType outputType )
	{
		this.outputType = outputType;
	}
	
	public OutputType getOutputType()
	{
		return outputType;
	}

	public void setOutputArch( Arch outputArch )
	{
		this.outputArch = outputArch;
	}

	public Arch getOutputArch()
	{
		return this.outputArch;
	}
	
	public void setBuildType( BuildType type )
	{
		this.buildType = type;
	}
	
	public BuildType getBuildType()
	{
		return this.buildType;
	}

	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////// Compiler Properties /////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	public void setCompilerType( CompilerType compilerType )
	{
		this.compilerType = compilerType;
	}

	public CompilerType getCompilerType()
	{
		return compilerType;
	}

	public void setCompilerArgs( String additionalArgs )
	{
		this.compilerArgs = additionalArgs;
	}

	public String getCompilerArgs()
	{
		return compilerArgs;
	}

	public void setLinkerArgs( String additionalArgs )
	{
		this.linkerArgs = additionalArgs;
	}

	public String getLinkerArgs()
	{
		return linkerArgs;
	}

	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////// Runtime Properties //////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	// Runtime properties
	public void setPreCommand( String preCommand )
	{
		this.preCommand = preCommand;
	}
	
	public String getPreCommand()
	{
		return this.preCommand;
	}
	
	public void setFailOnError( boolean failOnError )
	{
		this.failOnError = failOnError;
	}

	public boolean isFailOnError()
	{
		return failOnError;
	}

	public void setIncremental( boolean incremental )
	{
		this.incremental = incremental;
	}

	public boolean isIncremental()
	{
		return incremental;
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
