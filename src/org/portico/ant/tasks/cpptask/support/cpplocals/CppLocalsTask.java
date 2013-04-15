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
package org.portico.ant.tasks.cpptask.support.cpplocals;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.property.LocalProperties;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.portico.ant.tasks.cpptask.CppTask.CompilerAntEnum;
import org.portico.ant.tasks.cpptask.CppTask.OutputArchAntEnum;
import org.portico.ant.tasks.utils.Arch;
import org.portico.ant.tasks.utils.Platform;

/**
 * This task takes the desired compiler, architecture and build type, and then sets a number
 * of *local* variables that can be used in a cpptask compile. The basic purpose of this task
 * is to set a bunch of locally-scoped ant properties that can be used when compiling a cpptask
 * call that can serve many purposes (debug builds, multiple architectures, etc...).
 * <p/>
 * In addition to setting some basic properties, the user can also provide additional settings
 * that will be applied depending on whether this is a debug or release build.
 * <p/>
 * The properties that are set are all locally scopped and are prefixed with an "_" character
 * to remind you of this, as it is very much against the Ant-norm. The properties set are as
 * follows:
 * <ul>
 *   <li>_bitness: "" or "64" depending on arch. Can use in library/exe names (defaults to "")</li>
 *   <li>_d: "d" is this is a debug build, an empty string otherwise (defaults to "")</li>
 *   <li>_platform: Common name of the platform*
 *   <li>_cargs: Compiler args. Value depends on nested debug/release element (defaults to "")</li>
 *   <li>_largs: Linker args. Value depends on nested debug/release element (defaults to "")</li>
 *   <li>_buildsymbol: "DEBUG" or "RELEASE". Optionally use this in symbol definitions (defaults to "RELEASE")</li>
 *   <li>_symbols: Set of symbol definitions. Value depends on  nested debug/release element (defaults to "")</li>
 * </ul> 
 * 
 * *The common name for the platform is one of: "win32, win64, linux32, linux64, macosx" and often
 * used in paths to specify the correct location for libraries. 
 */
public class CppLocalsTask extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	//private CompilerType compiler; -- just not used yet :(
	private Arch arch;
	private boolean debugBuild;
	private Configuration debugConfiguration;
	private Configuration releaseConfiguration;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public CppLocalsTask()
	{
		//this.compiler = null; --not used yet
		this.arch = null;
		this.debugBuild = false;
		this.debugConfiguration = null;
		this.releaseConfiguration = null;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	public void execute()
	{
		// set _d and _buildsymbol
		if( this.debugBuild )
		{
			setLocal( "_d", "d" );
			setLocal( "_buildsymbol", "DEBUG" );
		}
		else
		{
			setLocal( "_d", "" );
			setLocal( "_buildsymbol", "RELEASE" );
		}
		
		// set _bitness
		if( arch == null || arch == Arch.x86 )
			setLocal( "_bitness", "" );
		else if( arch == Arch.amd64 )
			setLocal( "_bitness", "64" );
		
		// set _cargs, _largs and _symbols
		Configuration winner = debugBuild ? debugConfiguration : releaseConfiguration;
		setLocal( "_cargs", winner.compilerArgs );
		setLocal( "_largs", winner.linkerArgs );
		setLocal( "_symbols", winner.symbols );
		
		// set _platform
		Platform platform = Platform.getOsPlatform();
		if( platform.isWindows() )
		{
			if( arch == null || arch == Arch.x86 )
				setLocal( "_platform", "win32" );
			else
				setLocal( "_platform", "win64" );
		}
		else if( platform.isLinux() )
		{
			if( arch == null || arch == Arch.x86 )
				setLocal( "_platform", "linux32" );
			else
				setLocal( "_platform", "linux64" );
		}
		else if( platform.isMac() )
		{
			setLocal( "_platform", "macosx" );
		}
	}
	
	private void setLocal( String name, String value )
	{
		// declare the local
		LocalProperties.get(getProject()).addLocal( name );
		
		// set the value
		Property property = new Property();
		property.setProject( getProject() );
		property.setName( name );
		property.setValue( value );
		property.execute();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Sets the compiler being used.
	 */
	public void setCompiler( CompilerAntEnum compiler )
	{
		//this.compiler = CompilerType.fromString( compiler.getValue() ); --not used yet
	}

	/**
	 * Sets the target architecture (x86/amd64)
	 */
	public void setArch( OutputArchAntEnum arch )
	{
		this.arch = Arch.valueOf( arch.getValue().toLowerCase() );
	}

	/**
	 * Set the desired build type (debug/release)
	 */
	public void setBuild( BuildTypeAntEnum build )
	{
		this.debugBuild = build.getValue().equals( "debug" );
	}

	/**
	 * Add in the values to set for various properties if this is a debug build
	 */
	public void addDebug( Debug configuration )
	{
		if( debugConfiguration != null )
			throw new BuildException( "You can't have more than one <debug> configuration" );
		
		this.debugConfiguration = configuration;
	}
	
	/**
	 * Add in the values to set for various properties if this is a debug build
	 */
	public void addRelease( Release configuration )
	{
		if( releaseConfiguration != null )
			throw new BuildException( "You can't have more than one <release> configuration" );
		
		this.releaseConfiguration = configuration;
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
	public static class BuildTypeAntEnum extends EnumeratedAttribute
	{
		public String[] getValues()
		{
			return new String[] { "debug", "release" };
		}
	}
}
