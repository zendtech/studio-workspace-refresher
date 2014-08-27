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
import java.lang.reflect.Field;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.refresh.IRefreshMonitor;
import org.eclipse.core.resources.refresh.IRefreshResult;
import org.eclipse.core.runtime.IPath;

public class WorkspaceRefreshMonitor implements IRefreshMonitor, Runnable {

	private IRefreshResult refreshResult;
	private WatchService watchService;
	private boolean closed = false;

	public WorkspaceRefreshMonitor(IRefreshResult result) throws IOException {
		this.refreshResult = result;
		this.watchService = FileSystems.getDefault().newWatchService();

		Thread monitoringLoop = new Thread(this, "Workspace Refresh Monitor"); //$NON-NLS-1$
		monitoringLoop.start();
	}

	@Override
	public void run() {
		WatchKey watchKey = null;
		while (true) {
			try {
				watchKey = watchService.take();
			} catch (ClosedWatchServiceException e) {
				this.closed = true;
				return;
			} catch (InterruptedException e) {
				WorkspaceRefreshPlugin.log(e);
				continue;
			}

			if (watchKey == null || !watchKey.isValid()
					|| !(watchKey.watchable() instanceof Path)) {
				continue;
			}

			Path directory = (Path) watchKey.watchable();
			for (WatchEvent<?> event : watchKey.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				if (kind == StandardWatchEventKinds.OVERFLOW) {
					continue;
				}

				Path triggeredResource = (Path) event.context();
				Path newResource = directory.resolve(triggeredResource);

				if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
					if (Files.isDirectory(newResource)) {
						try {
							registerRecursively(newResource);
						} catch (IOException e) {
							WorkspaceRefreshPlugin.log(e);
						}
					}
				}

				refreshResource(newResource, kind);
			}

			if (!watchKey.reset()) {
				watchKey.cancel();
			}
		}
	}

	public boolean monitor(IResource resource) throws IOException {
		IPath resourcePath = resource.getLocation();
		if (resourcePath == null) {
			return false;
		}

		registerRecursively(Paths.get(resourcePath.toFile().toURI()));
		return true;
	}

	@Override
	public void unmonitor(IResource resource) {
		if (resource == null) {
			// disable monitoring
			try {
				watchService.close();
			} catch (IOException e) {
				WorkspaceRefreshPlugin.log(e);
			}
		} else {
			final IPath resourcePath = resource.getLocation();
			if (resourcePath == null) {
				return;
			}
			try {
				WatchKey watchKey = register(Paths.get(resourcePath.toFile()
						.toURI()));
				watchKey.cancel();
			} catch (IOException e) {
				WorkspaceRefreshPlugin.log(e);
			}
		}
	}

	private void refreshResource(Path elementPath, WatchEvent.Kind<?> kind) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IContainer[] containers = root.findContainersForLocationURI(elementPath
				.toUri());
		IFile[] files = root.findFilesForLocationURI(elementPath.toUri());

		if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
			refresh(containers);
			refresh(files);
		} else {
			if (Files.isDirectory(elementPath)) {
				refresh(containers);
			} else {
				refresh(files);
			}
		}
	}

	private void registerRecursively(final Path startPath) throws IOException {
		Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir,
					BasicFileAttributes attrs) throws IOException {
				register(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private WatchKey register(Path path) throws IOException {
		return path.register(watchService, new WatchEvent.Kind[] {
				StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY },
				get_com_sun_nio_file_SensitivityWatchEventModifier_HIGH());
	}

	private void refresh(IResource[] resources) {
		for (IResource resource : resources) {
			refresh(resource);
		}
	}

	private void refresh(IResource resource) {
		if (refreshResult != null
				&& !resource.isSynchronized(IResource.DEPTH_INFINITE)) {
			refreshResult.refresh(resource);
		}
	}

	/**
	 * "HIGH" modifier should improve the file detection speed.
	 * 
	 * @return SensitivityWatchEventModifier.HIGH
	 */
	private Modifier get_com_sun_nio_file_SensitivityWatchEventModifier_HIGH() {
		try {
			Class<?> c = Class
					.forName("com.sun.nio.file.SensitivityWatchEventModifier"); //$NON-NLS-1$
			Field f = c.getField("HIGH"); //$NON-NLS-1$
			return (Modifier) f.get(c);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Returns state of the monitor.
	 * 
	 * @return true if watch service was closed and monitoring thread was
	 *         stopped
	 */
	public boolean isClosed() {
		return closed;
	}

}