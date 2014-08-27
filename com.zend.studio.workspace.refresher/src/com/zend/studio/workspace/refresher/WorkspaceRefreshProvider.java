/*******************************************************************************
 * Copyright (c) 2014 Zend Techologies Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Zend Technologies Ltd. 
 *******************************************************************************/
package com.zend.studio.workspace.refresher;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.refresh.IRefreshMonitor;
import org.eclipse.core.resources.refresh.IRefreshResult;
import org.eclipse.core.resources.refresh.RefreshProvider;

public class WorkspaceRefreshProvider extends RefreshProvider {

	private WorkspaceRefreshMonitor refreshMonitor;

	@Override
	public IRefreshMonitor installMonitor(IResource resource,
			IRefreshResult result) {
		if (resource.getLocation() == null || !resource.exists()
				|| resource.getType() == IResource.FILE) {
			return null;
		}
		try {
			if (refreshMonitor == null || refreshMonitor.isClosed()) {
				refreshMonitor = new WorkspaceRefreshMonitor(result);
			}
			if (refreshMonitor.monitor(resource)) {
				return refreshMonitor;
			}
		} catch (IOException e) {
			WorkspaceRefreshPlugin.log(e);
		}

		return refreshMonitor;
	}
}
