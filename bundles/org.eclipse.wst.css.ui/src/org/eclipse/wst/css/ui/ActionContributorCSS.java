/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui;

import java.util.ResourceBundle;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.wst.sse.ui.edit.util.ActionContributor;
import org.eclipse.wst.sse.ui.edit.util.ActionDefinitionIds;
import org.eclipse.wst.sse.ui.edit.util.StructuredTextEditorActionConstants;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

/**
 * ActionContributorCSS
 * 
 * This class should not be used inside multi page editor's
 * ActionBarContributor, since cascaded init() call from the
 * ActionBarContributor will causes exception and it leads to lose whole
 * toolbars.
 * 
 * Instead, use SourcePageActionContributor for source page contributor of
 * multi page editor.
 * 
 * Note that this class is still valid for single page editor.
 */
public class ActionContributorCSS extends ActionContributor {
	private static final String[] EDITOR_IDS = {"org.eclipse.wst.css.ui.StructuredTextEditorCSS", "org.eclipse.wst.sse.ui.StructuredTextEditor"}; //$NON-NLS-1$ //$NON-NLS-2$

	protected RetargetTextEditorAction fContentAssist = null;
	protected RetargetTextEditorAction fCleanupDocument = null;
	protected MenuManager fFormatMenu = null;
	protected RetargetTextEditorAction fFormatDocument = null;
	protected RetargetTextEditorAction fFormatActiveElements = null;

	public ActionContributorCSS() {
		super();

		ResourceBundle resourceBundle = SSEUIPlugin.getDefault().getResourceBundle();

		// edit commands
		fContentAssist = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
		fContentAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);

		// source commands
		fCleanupDocument = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
		fCleanupDocument.setActionDefinitionId(ActionDefinitionIds.CLEANUP_DOCUMENT);

		fFormatDocument = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
		fFormatDocument.setActionDefinitionId(ActionDefinitionIds.FORMAT_DOCUMENT);

		fFormatActiveElements = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
		fFormatActiveElements.setActionDefinitionId(ActionDefinitionIds.FORMAT_ACTIVE_ELEMENTS);

		fFormatMenu = new MenuManager(SSEUIPlugin.getResourceString("%FormatMenu.label")); //$NON-NLS-1$
		fFormatMenu.add(fFormatDocument);
		fFormatMenu.add(fFormatActiveElements);
	}

	protected String[] getExtensionIDs() {
		return EDITOR_IDS;
	}

	protected void addToMenu(IMenuManager menu) {
		// edit commands
		IMenuManager editMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			editMenu.add(fCommandsSeparator);
			editMenu.add(fToggleInsertModeAction);
			editMenu.add(fCommandsSeparator);
			editMenu.add(fExpandSelectionToMenu);
			editMenu.add(fCommandsSeparator);
			editMenu.add(fContentAssist);
			editMenu.add(fMenuAdditionsGroupMarker);
		}

		// source commands
		String sourceMenuLabel = SSEUIPlugin.getResourceString("%SourceMenu.label"); //$NON-NLS-1$
		String sourceMenuId = "sourceMenuId"; // This is just a menu id. No
											  // need to translate.
											  // //$NON-NLS-1$
		IMenuManager sourceMenu = new MenuManager(sourceMenuLabel, sourceMenuId);
		menu.insertAfter(IWorkbenchActionConstants.M_EDIT, sourceMenu);
		if (sourceMenu != null) {
			sourceMenu.add(fCommandsSeparator);
			sourceMenu.add(fShiftRight);
			sourceMenu.add(fShiftLeft);
			sourceMenu.add(fCleanupDocument);
			sourceMenu.add(fFormatMenu);
			sourceMenu.add(fCommandsSeparator);
		}
	}

	public void setActiveEditor(IEditorPart activeEditor) {
		super.setActiveEditor(activeEditor);

		ITextEditor textEditor = getTextEditor(activeEditor);

		fContentAssist.setAction(getAction(textEditor, StructuredTextEditorActionConstants.ACTION_NAME_CONTENTASSIST_PROPOSALS));

		fCleanupDocument.setAction(getAction(textEditor, StructuredTextEditorActionConstants.ACTION_NAME_CLEANUP_DOCUMENT));
		fFormatDocument.setAction(getAction(textEditor, StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_DOCUMENT));
		fFormatActiveElements.setAction(getAction(textEditor, StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_ACTIVE_ELEMENTS));
	}
}