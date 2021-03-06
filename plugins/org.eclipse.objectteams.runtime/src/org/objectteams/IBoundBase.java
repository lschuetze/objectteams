/**********************************************************************
 * This file is part of the "Object Teams Runtime Environment"
 *
 * Copyright 2007-2009 Berlin Institute of Technology, Germany.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Please visit http://www.eclipse.org/objectteams for updates and contact.
 *
 * Contributors:
 *		Berlin Institute of Technology - Initial API and implementation
 **********************************************************************/
package org.objectteams;

/**
 * Super type for all bound base classes when using the traditional weaver (OTRE).
 * Purely internal class, not intended for client use.
 * @author Stephan Herrmann
 */
public interface IBoundBase {
	/** Method to be used by generated code, only (lifting constructor). */
	void _OT$addRole(Object aRole);
	/** Method to be used by generated code, only (unregisterRole()). */
	void _OT$removeRole(Object aRole);
}