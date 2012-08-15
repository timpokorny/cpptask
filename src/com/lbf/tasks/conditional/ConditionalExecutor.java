/*
 *   Copyright 2008 littlebluefroglabs.com
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
package com.lbf.tasks.conditional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;

/**
 * Wraps a set if tasks so that a user can define multiple properties that should or should not
 * be set before the target executes. Like allowing "if" and "unless" attributes of a target to
 * specify multiple values.
 */
public class ConditionalExecutor extends Task implements TaskContainer
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String name;
	private ArrayList<Task> tasks;
	private HashSet<String> required;
	private HashSet<String> unless;
	private boolean failOnError;
	
	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	public ConditionalExecutor()
	{
		super();
		this.failOnError = false;
		this.tasks = new ArrayList<Task>();
		this.required = new HashSet<String>();
		this.unless = new HashSet<String>();
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	public void execute()
	{
		// if failOnError is set, shouldExecute() will thrown an exception
		if( shouldExecute() == false )
			return;
			
		
		// execute the wrapped tasks now that we know we're good
		executeWrappedTasks();
	}
	
	/**
	 * Checks to see if this task should run based on the given information about set properties.
	 * If it should, <code>true</code> is returned, otherwise <code>false</code> is returned. If
	 * the <code>failOnError</code> attribute is enabled, <code>false</code> isn't returned, but
	 * rather an exception is thrown to kill the build.
	 */
	@SuppressWarnings("rawtypes")
	private boolean shouldExecute() throws BuildException
	{
		// make sure all the required properties are present
		Hashtable existingProperties = super.getProject().getProperties();
		for( String property : required )
		{
			if( existingProperties.containsKey(property) == false )
			{
				if( failOnError )
					throw new BuildException( "Required property not set: " + property );
				else
					return false;
			}
		}
		
		// make sure NONE of the excluded properties are present
		for( String property : unless )
		{
			if( existingProperties.containsKey(property) )
			{
				if( failOnError )
					throw new BuildException( "Property that should not be set is set: "+property );
				else
					return false;
			}
		}
		
		// if we get here then we are all good to execute the wrapped task
		return true;
	}
	
	/**
	 * This method is called once it has been determined that it is safe to run the tasks according
	 * to the requirements of the project properties. This will call each contained task in order.
	 */
	private void executeWrappedTasks()
	{
		for( Task task : tasks )
			task.perform();
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// Task Attributes/Nested Elements ////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////
	public void addTask( Task task )
	{
		this.tasks.add( task );
	}
	
	public void setName( String name )
	{
		this.name = name;
		this.setTaskName( this.name );
	}
	
	public void setFailOnError( boolean failOnError )
	{
		this.failOnError = failOnError;
	}

	public void addConfiguredRequired( Required value )
	{
		this.required.addAll( explode(value.property) );
	}
	
	public void addConfiguredUnless( Unless value )
	{
		this.unless.addAll( explode(value.property) );
	}
	
	public void setIfAll( String value )
	{
		this.required.addAll( explode(value) );
	}
	
	public void setUnlessAny( String value )
	{
		this.required.addAll( explode(value) );
	}
	
	private HashSet<String> explode( String given )
	{
		HashSet<String> values = new HashSet<String>();
		StringTokenizer tokenizer = new StringTokenizer( given, "," );
		while( tokenizer.hasMoreElements() )
			values.add( tokenizer.nextToken().trim() );
		
		return values;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
