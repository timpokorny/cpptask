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
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.portico.ant.tasks.utils.Arch;
import org.portico.ant.tasks.utils.Platform;


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
	
	// Compiler and linker options
	private CompilerType compilerType;
	private String compilerArgs;
	private String linkerArgs;

	// Runtime properties
	private String preCommand;
	private boolean failOnError;
	private boolean incremental;
	private int threadCount; 

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
		
		// Compiler and linker options
		this.compilerArgs = "";
		this.linkerArgs   = "";
		this.compilerType = Platform.getOsPlatform().isWindows() ? CompilerType.VC10 :
		                                                           CompilerType.GCC;

		// Runtime properties
		this.preCommand = "";
		this.incremental = true;
		this.failOnError = true;
		this.threadCount = 1;

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
	///////////////////////////////// Validation Methods //////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Checks the build configuration to make sure all the required information has been
	 * provided. If not, a build exception is thrown.
	 */
	public void validateConfiguration() throws BuildException
	{
		// check to see if we have an output to generate
		if( this.outputName == null )
			throw new BuildException( "The attribute \"outputName\" is required" );
		
		// make sure they've told us where things are going to go
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
	 * Generates a new temp directory under a path that is specific for the output
	 * architecture of the build. During compilation, object and other build files
	 * should be placed in this directory. Separate files may be generated for the
	 * particular output arch, so using this method ensures that a separate directory
	 * is provided for each target build architecure. If this folder doesn't exist
	 * it will be created by this call. The architecture used is that which is set
	 * as the output architecture for the build.
	 * <p/>
	 * The pathing conforms to the following general scheme:
	 * <ul>
	 *   <li>workdir: ./working</li>
	 *   <li>objdir : ./working/obj/amd64 (for 64-bit)</li>
	 * </ul>
	 * For example, if the working directory is "./working" that would make the temp
	 * directory "./working/temp". From here, if the output arch was "amd64", the returned
	 * file would point to the directory "./working/temp/amd64"
	 */
	public File getObjectDirectory()
	{
		File directory = new File( this.workingDirectory, "obj/"+getOutputArch().toString() );
		directory.mkdirs();
		return directory;
	}

	public void setOutputDirectory( File file )
	{
		this.outputDirectory = file;
	}

	/**
	 * The directory where the compiled output should be put.
	 */
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
	
	/**
	 * Set the number of threads that should be used for a parallel build. If set to "auto" it
	 * will use the same number of threads as the system has cores.
	 */
	public void setThreadCount( String threadString )
	{
		threadString = threadString.toLowerCase();
		int systemProcessors = Runtime.getRuntime().availableProcessors();
		if( threadString.equals("auto") )
		{
			this.threadCount = systemProcessors;
		}
		else 
		{
			int count = Integer.parseInt( threadString );
			if( count > systemProcessors )
				count = systemProcessors;
			
			this.threadCount = count;
		}
	}

	/**
	 * Returns the number of threads to use in any parallel builds. Defaults to 1. If set to
	 * "auto", it will use the number of CPUs available to the system.
	 * @return
	 */
	public int getThreadCount()
	{
		return this.threadCount;
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

	/**
	 * Add a symbole definition that will be used for the compile
	 */
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

	////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// Private Helper Methods ////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	//private void debug( String message )
	//{
	//	task.log( message, Project.MSG_DEBUG );
	//}
	
	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
