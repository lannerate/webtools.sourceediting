/*******************************************************************************
 * Copyright (c) 2001, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jens Lukowski/Innoopract - initial renaming/restructuring
 *     
 *******************************************************************************/
package org.eclipse.wst.sse.ui.openon;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.sse.core.IModelManager;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.ui.extensions.openon.IOpenOn;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.openon.ExternalFileEditorInput;
import org.eclipse.wst.sse.ui.util.PlatformStatusLineUtil;

/**
 * This action class retrieves the link/file selected by the cursor and
 * attempts to open the link/file in the default editor or web browser
 */
abstract public class AbstractOpenOn implements IOpenOn {
	protected final String CANNOT_OPEN = SSEUIPlugin.getResourceString("%AbstractOpenOn.0"); //$NON-NLS-1$
	private IDocument fDocument; // document currention associated with open
	protected final String FILE_PROTOCOL = "file:/";//$NON-NLS-1$
	private final String HTTP_PROTOCOL = "http://";//$NON-NLS-1$

	abstract protected IRegion doGetOpenOnRegion(int offset);

	abstract protected void doOpenOn(IRegion region);

	/**
	 * Returns the current document associated with open on
	 * 
	 * @return IDocument
	 */
	public IDocument getDocument() {
		return fDocument;
	}

	/**
	 * Determines the editor associated with the given file name
	 * 
	 * @param filename
	 * @return editor id of the editor associated with the given file name
	 */
	private String getEditorId(String filename) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IEditorRegistry editorRegistry = workbench.getEditorRegistry();
		IEditorDescriptor descriptor = editorRegistry.getDefaultEditor(filename);
		if (descriptor != null)
			return descriptor.getId();
		return EditorsUI.DEFAULT_TEXT_EDITOR_ID;
	}

	/**
	 * Returns an IFile from the given uri if possible, null if cannot find
	 * file from uri.
	 * 
	 * @param fileString
	 *            file system path
	 * @return returns IFile if fileString exists in the workspace
	 */
	protected IFile getFile(String fileString) {
		if (fileString != null) {
			IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(new Path(fileString));
			for (int i = 0; i < files.length; i++)
				if (files[i].exists())
					return files[i];
		}
		return null;
	}

	protected IModelManager getModelManager() {
		return StructuredModelManager.getInstance().getModelManager();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.sse.ui.IOpenOn#getOpenOnRegion(org.eclipse.jface.text.IDocument,
	 *      int)
	 */
	public IRegion getOpenOnRegion(IDocument doc, int offset) {
		IRegion region;
		// set the document for this action
		setDocument(doc);
		region = doGetOpenOnRegion(offset);
		// reset the document back to null for this action
		setDocument(null);
		return region;
	}

	/**
	 * Try to open the external file, fileString in its default editor
	 * 
	 * @param fileString
	 * @return IEditorPart editor opened or null if editor could not be opened
	 */
	protected IEditorPart openExternalFile(String fileString) {
		// file does not exist in workspace so try to open using system editor
		File file = new File(fileString);
		// try to open existing external file if it exists
		if (file.exists()) {
			IEditorInput input = new ExternalFileEditorInput(file);
			String editorId = getEditorId(fileString);

			try {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				return page.openEditor(input, editorId, true);
			} catch (PartInitException pie) {
				Logger.log(Logger.WARNING_DEBUG, pie.getMessage(), pie);
			}
		}
		return null;
	}

	/**
	 * Notifies user that open on selection action could not successfully open
	 * the selection (writes message on status bar and beeps)
	 */
	protected void openFileFailed() {
		PlatformStatusLineUtil.displayErrorMessage(CANNOT_OPEN);
		PlatformStatusLineUtil.addOneTimeClearListener();
	}

	/**
	 * Opens the IFile, input in its default editor, if possible, and returns
	 * the editor opened. Possible reasons for failure: input cannot be found,
	 * input does not exist in workbench, editor cannot be opened.
	 * 
	 * @return IEditorPart editor opened or null if input == null or does not
	 *         exist, external editor was opened, editor could not be opened
	 */
	protected IEditorPart openFileInEditor(IFile input) {
		if (input != null && input.exists()) {
			try {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				return IDE.openEditor(page, input, true);
			} catch (PartInitException pie) {
				Logger.log(Logger.WARNING_DEBUG, pie.getMessage(), pie);
			}
		}
		return null;
	}

	// on

	/**
	 * Opens the appropriate editor for fileString
	 * 
	 * @param fileString
	 */
	protected void openFileInEditor(String fileString) {
		IEditorPart editor = null;
		if (fileString != null) {
			// open web browser if this is a web address
			String temp = fileString.toLowerCase();
			if (temp.startsWith(HTTP_PROTOCOL)) {
				Program.launch(fileString); // launches web browser/executable
				// associated with uri
				return;
			}
			// chop off the file protocol
			if (temp.startsWith(FILE_PROTOCOL)) {
				fileString = fileString.substring(FILE_PROTOCOL.length());
			}

			// try to locate the file in the workspace and return an IFile if
			// found
			IFile file = getFile(fileString);
			if (file != null) {
				// file exists in workspace
				editor = openFileInEditor(file);
			} else {
				// file does not exist in workspace
				editor = openExternalFile(fileString);
			}
		}
		// no editor was opened
		if (editor == null) {
			openFileFailed();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.sse.ui.IOpenOn#openOn(org.eclipse.jface.text.IDocument,
	 *      org.eclipse.jface.text.IRegion)
	 */
	public void openOn(IDocument doc, IRegion region) {
		// set the document for this action
		setDocument(doc);
		// if no region was given this action fails
		if (region == null)
			openFileFailed();
		else
			doOpenOn(region);
		// reset the document back to null for this action
		setDocument(null);
	}

	/**
	 * Sets current document associated with open on
	 * 
	 * @param document
	 */
	public void setDocument(IDocument document) {
		fDocument = document;
	}
}
