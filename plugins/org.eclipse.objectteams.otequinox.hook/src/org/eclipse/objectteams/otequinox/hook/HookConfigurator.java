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
 * 
 * Please visit http://www.eclipse.org/objectteams for updates and contact.
 * 
 * Contributors:
 * Fraunhofer FIRST - Initial API and implementation
 * Technical University Berlin - Initial API and implementation
 **********************************************************************/
package org.eclipse.objectteams.otequinox.hook;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.internal.adaptor.EclipseStorageHook;
import org.eclipse.objectteams.otequinox.internal.hook.Logger;
import org.eclipse.objectteams.otequinox.internal.hook.OTEquinoxServiceWatcher;
import org.eclipse.objectteams.otequinox.internal.hook.OTStorageHook;
import org.eclipse.objectteams.otequinox.internal.hook.TransformerHook;
import org.eclipse.osgi.baseadaptor.hooks.StorageHook;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

/**
 * This class installs the necessary hooks for using OT/J in eclipse plugins.
 * As the first OTDT class in the load process it also updates the eclipse buildId
 * to contain the OTDT version number, too (for logging).
 * 
 * @author stephan
 */
public class HookConfigurator implements org.eclipse.osgi.baseadaptor.HookConfigurator {
	
	/** Has OT/Equinox been enabled (using system property ot.equinox set to anything but "false")? */
	public static final boolean OT_EQUINOX_ENABLED;

	static {
		String otequinoxProperty = System.getProperty("ot.equinox"); //$NON-NLS-1$
		if (otequinoxProperty == null)
			OT_EQUINOX_ENABLED = false;
		else
			OT_EQUINOX_ENABLED = !"false".equals(otequinoxProperty); //$NON-NLS-1$
	}
	
	// singleton:
	static HookConfigurator instance;
	// logging (console or via FrameworkLog):
	private Logger logger;
	public HookConfigurator() {
		instance = this;
	}
	public static ILogger getLogger() {
		if (instance == null)
			throw new RuntimeException("TransformerHook not initialized, perhaps OT/Equinox is not enabled?"); //$NON-NLS-1$
		return instance.logger;
	}
	
	public void addHooks(org.eclipse.osgi.baseadaptor.HookRegistry hookRegistry) {
		if (!OT_EQUINOX_ENABLED)
			return;
		this.logger = new Logger(hookRegistry.getAdaptor().getFrameworkLog());
		TransformerHook transformerHook = new TransformerHook(hookRegistry.getAdaptor());
		// this instance observes
		hookRegistry.addClassLoadingHook(transformerHook);
		hookRegistry.addClassLoaderDelegateHook(transformerHook);
		hookRegistry.addWatcher(transformerHook);
		hookRegistry.addClassLoadingStatsHook(transformerHook);
		hookRegistry.addAdaptorHook(new OTEquinoxServiceWatcher(transformerHook));
		synchronized (hookRegistry) {
			insertStorageHook(hookRegistry, transformerHook);
		}
	}
	/* Insert an OTStorageHook before EclipseStorageHook as to intercept getManifest(): */
	private void insertStorageHook(org.eclipse.osgi.baseadaptor.HookRegistry hookRegistry, TransformerHook transformerHook) 
	{
		hookRegistry.addStorageHook(null); // grow array, fill in below:
		StorageHook[] allStorageHooks= hookRegistry.getStorageHooks();

		OTStorageHook storageHook= null;
		for (int i=allStorageHooks.length-1; i>0; i--) {
			allStorageHooks[i]= allStorageHooks[i-1];
			if (allStorageHooks[i] instanceof EclipseStorageHook) 
				storageHook= new OTStorageHook((EclipseStorageHook)allStorageHooks[i], transformerHook);
		}
		if (storageHook != null) // only install if validly wired
			allStorageHooks[0]= storageHook; 
		else
			hookRegistry.getAdaptor().getFrameworkLog().log(new FrameworkLogEntry(
					HookConfigurator.class.getName(),
					"EclipseStorageHook not found", //$NON-NLS-1$
					FrameworkLogEntry.ERROR, null, null));
	}
	/** Bridge to the OTStorageHook. */
	public static void parseForcedExportsFile(File configFile, AspectPermission perm) {
		OTStorageHook.parseForcedExportsFile(configFile, perm);
	}
	/** Bridge to the OTStorageHook. */
	public static ArrayList<String[]> getForcedExportsByAspect(String aspectBundleId, AspectPermission perm) {
		return OTStorageHook.getForcedExportsByAspect(aspectBundleId, perm);
	}
}
