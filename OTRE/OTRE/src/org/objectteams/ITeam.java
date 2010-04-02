/**********************************************************************
 * This file is part of the "Object Teams Runtime Environment"
 * 
 * Copyright 2010 Stephan Herrmann.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * $Id: ITeam.java 23408 2010-02-03 18:07:35Z stephan $
 * 
 * Please visit http://www.objectteams.org for updates and contact.
 * 
 * Contributors:
 * 		Stephan Herrmann - Initial API and implementation
 **********************************************************************/
package org.objectteams;

/**
 * Public interface of all team classes.
 */
public interface ITeam {
	
    /**
     *  Interface for all role classes that should allow explicit lowering.
     *  The interface provides a phantom method <pre>&lt;B&gt; B lower()</pre>
     *  where B is the bound base class of the implementing role class.
     *  There is no need to implement the method lower, since this is done by the compiler.
     */
    public interface ILowerable {
		// internal method needed for cast and instanceof
    	ITeam _OT$getTeam();
    }

	/**
	 *  This role interface has no properties not even those of java.lang.Object.
	 */
	public interface IConfined extends org.objectteams.IConfined {
		// internal method needed for cast and instanceof
		ITeam _OT$getTeam(); 
	}


	/**
	 * Activates the team and therefore all of its callin bindings. 
	 * This activation applies to the current thread only.
	 */
	public abstract void activate();

	/**
	 * Deactivates the team and therefore all of its callin bindings. 
	 * This deactivation applies to the current thread only.
	 */
	public abstract void deactivate();

	/**
	 * Activates the team and therefore all of its callin bindings for passed thread. 
	 * If the constant 'Team.ALL_THREADS' is passed, this activation globally applies to all threads.
	 */
	public abstract void activate(Thread thread);

	/**
	 * Deactivates the team and therefore all of its callin bindings for passed thread. 
	 * If the constant 'Team.ALL_THREADS' is passed, this deactivation globally applies to all threads.
	 */
	public abstract void deactivate(Thread thread);

	/**
	 * Checks, if the team instance is active for the current thread.
	 * @return true, if the team is active, false else.
	 */
	public abstract boolean isActive();

	/**
	 * Checks, if the team instance is active for the 'thread'.
	 * @param thread	The thread for which to check activity.
	 * @return	true, if the team is active for 'thread', false else.
	 */
	public abstract boolean isActive(Thread thread);

	/**
	 * Does given base object have a role in this team?
	 * This method will consider roles of any type.
	 * 
	 * @param aBase any object, i.e., no checks are performed whether the base object's
	 * 		class is bound by any role class in this team. 	
	 * @return
	 */
	public abstract boolean hasRole(Object aBase);

	/**
	 * Does given base object have a role in this team?
	 * The role must be an instance of the specified role type.
	 * 
	 * @param aBase any object, i.e., no checks are performed whether the base object's
	 * 		class is bound by any role class in this team.
	 * @param roleClass Class instance specifying the required role type.
	 *      If this does not specify an existing role class an IllegalArgumentException will be thrown.
	 *      TODO (SH): is it legal to pass an unbound role class? 	
	 * @return
	 */
	public abstract boolean hasRole(Object aBase, Class<?> roleType);

	/**
	 * Retrieve a role for a given base object.
	 * If more than one role exists, a DuplicateRoleException is thrown.
	 * 
	 * @param aBase
	 * @return
	 */
	public abstract Object getRole(Object aBase);

	/**
	 * Retrieve a role for a given base object.
	 * The role must be an instance of the specified role type.
	 * 
	 * @param aBase any object, i.e., no checks are performed whether the base object's
	 * 		class is bound by any role class in this team.
	 * @param roleClass Class instance specifying the required role type.
	 *      If this does not specify an existing role class an IllegalArgumentException will be thrown.
	 * @return
	 */
	public abstract <T> T getRole(Object aBase, Class<T> roleType);

	/**
	 * Retrieve all bound roles registered in the current team.
	 * 
	 * This method uses internal structures of weak references. 
	 * For that reason it may return role instances which were about to be reclaimed 
	 * by the garbage collector. 
	 * If performance permits, it is thus advisable to always call System.gc() 
	 * prior to calling getAllRoles() in order to achieve deterministic results
	 * 
	 * @return a non-null array.
	 */
	public abstract Object[] getAllRoles();

	/**
	 * Retrieve all bound roles registered in the current team that
	 * are instance of roleType or a subtype thereof.
	 * 
	 * This method uses internal structures of weak references. 
	 * For that reason it may return role instances which were about to be reclaimed 
	 * by the garbage collector. 
	 * If performance permits, it is thus advisable to always call System.gc() 
	 * prior to calling getAllRoles() in order to achieve deterministic results
	 * 
	 * @param roleType must be a top-most bound role of this team. 
	 * @return a non-null array.
	 */
	public abstract <T> T[] getAllRoles(Class<T> roleType);

	/**
	 * Query whether any role instance of this team instance is currently executing a
	 * method due to a callin binding.
	 * @return
	 */
	public abstract boolean isExecutingCallin();

	/**
	 * Remove a role from the internal registry, which means that the role will no longer be
	 * considered during lifting.
	 *  
	 * @param aRole
	 */
	public abstract void unregisterRole(Object aRole);

	/**
	 * Remove a role from the internal registry, which means that the role will no longer be
	 * considered during lifting.
	 *  
	 * @param aRole
	 * @param roleType
	 */
	public abstract void unregisterRole(Object aRole, Class<?> roleType);

	/**
	 * Not API.
	 * This method saves the activation state of the team  for the current thread.
	 * If active, it also saves, if the activation was explicit or implicit.
	 * This method has to be called by the generated code when entering a within block, 
	 * before the activation.
	 */
	public int _OT$saveActivationState();
	
	/**
	 * Not API.
	 * This method restores the former saved activation state of the team  for the current thread.
	 * If active, it also restores, if the activation was explicit or implicit.
	 * This method has to be called by the generated code when leaving a within block 
	 * (in the finally block).
	 */
	public void _OT$restoreActivationState(int old_state);
	
	/**
	 * Not API, for use by TeamThreadManager, only.
	 */
	public boolean internalIsActiveSpecificallyFor(Thread t);
	
	/**
	 * Not API, for use by TeamThreadManager, only.
	 */
	public void deactivateForEndedThread(Thread thread);
}