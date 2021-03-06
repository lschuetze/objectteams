/**********************************************************************
 * This file is part of "Object Teams Development Tooling"-Software
 * 
 * Copyright 2004, 2006 Fraunhofer Gesellschaft, Munich, Germany,
 * for its Fraunhofer Institute for Computer Architecture and Software
 * Technology (FIRST), Berlin, Germany and Technical University Berlin,
 * Germany.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * $Id$
 * 
 * Please visit http://www.eclipse.org/objectteams for updates and contact.
 * 
 * Contributors:
 * Fraunhofer FIRST - Initial API and implementation
 * Technical University Berlin - Initial API and implementation
 **********************************************************************/
package org.eclipse.objectteams.otdt.debug;

public interface IOTDTDebugPreferenceConstants
{

	/**
	 * <h3>Sort mode for teams in the Team Monitor view.</h3> 
	 * By this constant the UI advises the {@link OTDebugElementsContainer} to 
	 * sort team instances by the names of their classes.
	 */
	public static final String TEAMS_BY_NAME = "Teams.by.name"; //$NON-NLS-1$
	/**
	 * <h3>Sort mode for teams in the Team Monitor view.</h3> 
	 * By this constant the UI advises the {@link OTDebugElementsContainer} to 
	 * sort team instances by instantiation time.
	 */
	public static final String TEAMS_BY_INSTANTIATION = "Teams.by.instantiation"; //$NON-NLS-1$
	/**
	 * <h3>Sort mode for teams in the Team Monitor view.</h3> 
	 * By this constant the UI advises the {@link OTDebugElementsContainer} to 
	 * sort team instances by activation time.
	 */
	public static final String TEAMS_BY_ACTIVATION_TIME = "Teams.by.activationtime"; //$NON-NLS-1$
}
