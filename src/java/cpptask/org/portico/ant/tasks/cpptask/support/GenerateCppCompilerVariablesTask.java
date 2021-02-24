/*
 *   Copyright 2016 The Portico Project
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
package org.portico.ant.tasks.cpptask.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * This task takes an Ant property that details the specifics of the compiler combinations
 * we want to build our C++ app with. This profile is a comma-separated string containing
 * symbolic names for compilers, architectures and build types (release/debug).
 * 
 * From this profile, the information is processed and the particular combinations of compiler,
 * release type and arch are inferred. Based on this information, certain Ant properties are
 * set. For each combination we believe is valid, a property in the form: `compiler.arch.type`
 * is created (for example, <code>vc8.x86.debug</code>).
 * 
 * Later on, these properties can be used with the "if" attribute to conditionally execute
 * tasks or not related to each of the specific compiler flag combinations.
 * 
 * The process of inferring which combinations are valid based on the input works by assuming
 * that for each of `compilers`, `architectures` and `builds`, if no values are specified then
 * properties are generated for all valid values. For example, specifying <code>vc10</code>
 * will cause ONLY the vc10 combinations to have attributes set. Omitting any mention will cause
 * all the specified compiler combinations to have attributes generated for them.
 * <p/>
 * <h3>Usage:</h3>
 * <p/>
 * <pre>
 * &lt;generateCppCompilerVariables compilers="vc8,vc9,vc10,vc11,vc12,vc14" <-- defaults
 *                                  architectures="x86,amd64"     <-- defaults
 *                                  builds="debug,release"        <-- defaults
 *                                  property="propName"&gt;       <-- optional, default is "compilers"
 * </pre>
 * 
 * To specify, you must create a variable at some point before execution with the name specified
 * in <code>property</code>. If not, <code>compilers</code> will be used. The typical way to
 * specify this information is via system variable at Ant startup (<code>./ant -Dcompilers=...</code>).
 * 
 * On the task itself, you can specify the range of valid values via the compilers, architecutres
 * and builds properties.
 * 
 * <ul>
 *   <li><code>./ant -Dcompilers=vc8 (all VC8)</code></li>
 *   <li><code>./ant -Dcompilers=vc8,debug (all VC8 debug tasks)</code></li>
 *   <li><code>./ant -Dcompilers=vc8,amd64 (all VC8 amd64 tasks)</code></li>
 *   <li><code>./ant -Dcompilers=vc8,debug,amd64 (the VC8 debug build for amd64)</code></li>
 * </ul>
 * 
 * <p/>
 * <h3>Properties that are set from the profile</h3>
 * <p/>
 * The properties that are set follow a consistent scheme: [compiler].[arch].[build].
 * 
 * <p/>
 * So, for <code>-Dcompilers="vc8,vc9,debug"</code> the properties that are set will be:
 * <ul>
 *   <li><code>vc8.x86.debug</code></li>
 *   <li><code>vc8.amd64.debug</code></li>
 *   <li><code>vc9.amd64.debug</code></li>
 *   <li><code>vc9.amd64.debug</code></li>
 * </ul>
 * 
 * When declaring your targets that will perform the actual compilation for these combinations,
 * if you declare the <code>if</code> parameter to reference these properties, they should
 * allow or prevent the task from running appropriately.
 * <pre>
 *   &lt;target name="cpp.compile.vc8.x86.debug" if="vc8.x86.debug"/&gt; ...
 * </pre> 
 * 
 * <p/>
 * <h3>Default values for Supported Compilers, Architectures and Builds</h3>
 * <p/>
 * By default, the task is set up to provide a sensible set of defaults for each of the
 * supported compiler, architecture and build sets. You can override these values by setting
 * them directly on the target when you invoke it. The default settings are:
 * <ul>
 *   <li>Compilers: vc8, vc9, vc10, vc11, vc12, vc13</li>
 *   <li>Architectures: x86, amd64</li>
 *   <li>Builds: debug, release</li>
 * </ul>
 * 
 */
public class GenerateCppCompilerVariablesTask extends Task
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private HashMap<String,Compiler> settings; // where we put the build up profile
	
	// values set on the task by the user
	private String compilersProperty; // set to "compilers" by default - not modifiable currently 
	private Set<String> supportedCompilers;
	private Set<String> supportedArchitectures;
	private Set<String> supportedBuilds;
	
	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public GenerateCppCompilerVariablesTask()
	{
		this.settings = new HashMap<String,Compiler>();
		this.compilersProperty = "compilers";

		this.supportedCompilers = new HashSet<String>();
		this.supportedCompilers.add( "vc8" );
		this.supportedCompilers.add( "vc9" );
		this.supportedCompilers.add( "vc10" );
		this.supportedCompilers.add( "vc11" );
		this.supportedCompilers.add( "vc12" );
		this.supportedCompilers.add( "vc14" );
		this.supportedCompilers.add( "vc14.1" );
		this.supportedCompilers.add( "vc14.2" );
		
		this.supportedArchitectures = new HashSet<String>();
		this.supportedArchitectures.add( "x86" );
		this.supportedArchitectures.add( "amd64" );
		
		this.supportedBuilds = new HashSet<String>();
		this.supportedBuilds.add( "debug" );
		this.supportedBuilds.add( "release" );
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	public void execute()
	{
		// get the profile from the system properties
		String profile = getProject().getProperty( compilersProperty );
		
		// if there is no profile, turn everything on
		if( profile == null || profile.trim().equals("") )
		{
			logVerbose( "No build profile provided, enabling everything" );
			turnEverythingOn();
		}
		else
		{
			HashSet<String> profileValues = explode( profile );
			logVerbose( "Processing build profile: "+profile );

			processForCompilers( profileValues );
			processForArchitectures( profileValues );
			processForBuilds( profileValues );
		}

		// set the project properties from the profile
		for( String compilerName : this.settings.keySet() )
			this.settings.get(compilerName).applyProperties( compilerName );
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////// Profile Building Methods /////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void processForCompilers( Set<String> profile )
	{
		////////////////////////////
		// find all the compilers //
		////////////////////////////
		// note down if we don't find any, as if that is the case we use them all
		boolean foundOne = false;
		for( String supportedCompiler : supportedCompilers )
		{
			if( profile.contains(supportedCompiler) )
			{
				foundOne = true;
				this.settings.put( supportedCompiler, new Compiler() );
			}
		}
		
		// if we didn't find a single compiler setting, add all the supported compilers
		if( foundOne == false )
		{
			for( String supportedCompiler : supportedCompilers )
				this.settings.put( supportedCompiler, new Compiler() );
		}
	}
	
	private void processForArchitectures( Set<String> profile )
	{
		// find all the specified architectures and enable them on all registered compilers
		boolean foundOne = false;
		for( String supportedArchitecture : supportedArchitectures )
		{
			if( profile.contains(supportedArchitecture) )
			{
				foundOne = true;
				// set this flag on every compiler
				for( Compiler compiler : this.settings.values() )
					compiler.arch.add(supportedArchitecture);
			}
		}

		// if no architecture was specified, turn all supported ones on
		if( foundOne == false )
		{
			for( Compiler compiler : this.settings.values() )
			{
				for( String supportedArchitecture : supportedArchitectures )
					compiler.arch.add( supportedArchitecture );
			}
		}
	}
	
	private void processForBuilds( Set<String> profile )
	{
		// find all the specified builds and enable them for the various architectures
		// that have already been turned on
		boolean foundOne = false;
		for( String supportedBuild : supportedBuilds )
		{
			if( profile.contains(supportedBuild) )
			{
				foundOne = true;
				// set the flag on each specified architecture for each specified compiler
				for( Compiler compiler : this.settings.values() )
					compiler.build.add(supportedBuild);
			}
		}
		
		// if no build type was specified, turn all supported ones on
		if( foundOne == false )
		{
			for( Compiler compiler : this.settings.values() )
			{
				for( String supportedBuild : supportedBuilds )
					compiler.build.add( supportedBuild );
			}
		}
	}
	
	private void turnEverythingOn()
	{
		for( String compilerName : supportedCompilers )
		{
			Compiler compiler = new Compiler();
			this.settings.put( compilerName, compiler );
			for( String supportedArch : supportedArchitectures )
				compiler.arch.add( supportedArch );

			for( String supportedBuild : supportedBuilds )
				compiler.build.add( supportedBuild );
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Utility Methods //////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void logVerbose( String message )
	{
		log( message, Project.MSG_VERBOSE );
	}
	
	private void setBuildProperty( String name )
	{
		logVerbose( " [enabled] "+name );
		getProject().setProperty( name, "true" );
	}
	
	private HashSet<String> explode( String given )
	{
		HashSet<String> values = new HashSet<String>();
		StringTokenizer tokenizer = new StringTokenizer( given, "," );
		while( tokenizer.hasMoreElements() )
			values.add( tokenizer.nextToken().trim() );
		
		return values;
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	public void setCompilers( String compilers )
	{
		this.supportedCompilers.clear(); // remove the defaults
		this.supportedCompilers.addAll( explode(compilers) );
	}
	
	public void setArchitectures( String architectures )
	{
		this.supportedArchitectures.clear(); // remove the defaults
		this.supportedArchitectures.addAll( explode(architectures) );
	}
	
	public void setBuilds( String builds )
	{
		this.supportedBuilds.clear(); // remove the defaults
		this.supportedBuilds.addAll( explode(builds) );
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	/**
	 * Testing.
	 */
	public static void main( String[] args )
	{
		GenerateCppCompilerVariablesTask task = new GenerateCppCompilerVariablesTask();
		task.setCompilers( "vc8,vc9,vc10,vc11,vc12,vc14,vc14.1,vc14.2" );
		task.setArchitectures( "x86,amd64" );
		task.setBuilds( "debug,release" );

		String profile = "";
		//String profile = "vc8";
		//String profile = "debug";
		//String profile = "amd64";
		//String profile = "vc8,debug";
		//String profile = "vc8,amd64";
		//String profile = "vc8,amd64,release";
		//String profile = "vc8,debug,x86,amd64";
		//String profile = "vc8,vc10,amd64,release";
		//task.execute( profile );
		System.out.println( "Compelte for profile: "+profile );
	}
	
	/////////////////////////////////////////////////////////////////
	///////////////// Private Inner Class: Compiler /////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * This class represents a compiler against which we can execute builds targeting
	 * various combinations of architectures and build types. When the build profile
	 * is being processed, instances of this class are created and have their properties
	 * set. At the conclusion, all created compilers are iterated through and properties
	 * are set for their recorded architecture and build types.
	 */
	private class Compiler
	{
		public Set<String> arch = new HashSet<String>();
		public Set<String> build = new HashSet<String>();
		
		/**
		 * Set the actual properties in the project. Depending on the compiler name
		 * this will set the appropriate project properties to true.
		 */
		public void applyProperties( String compilerName )
		{
			logVerbose( "Apply properties for: "+compilerName );
			for( String setArch : arch )
			{
				for( String setBuild : build )
				{
					setBuildProperty( compilerName+"."+setArch+"."+setBuild );
				}
			}
		}
	}
}
