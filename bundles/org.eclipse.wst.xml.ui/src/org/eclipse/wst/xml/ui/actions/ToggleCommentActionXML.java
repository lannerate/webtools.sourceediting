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
package org.eclipse.wst.xml.ui.actions;

import java.util.ResourceBundle;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.exceptions.SourceEditingRuntimeException;
import org.eclipse.wst.sse.ui.edit.util.StructuredTextEditorActionConstants;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

public class ToggleCommentActionXML extends UncommentActionXML {
	public ToggleCommentActionXML(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
	}

	protected void processAction() {
		fModel.beginRecording(this, SSEUIPlugin.getResourceString(StructuredTextEditorActionConstants.ACTION_NAME_COMMENT + ".tooltip")); //$NON-NLS-1$
		fModel.aboutToChangeModel();

		for (int i = fSelectionStartLine; i <= fSelectionEndLine; i++) {
			try {
				if (fDocument.getLineLength(i) > 0) {
					if (isCommentLine(i)) {
						int lineOffset = fDocument.getLineOffset(i);
						IRegion region = fDocument.getLineInformation(i);
						String string = fDocument.get(region.getOffset(), region.getLength());
						int openCommentOffset = lineOffset + string.indexOf(OPEN_COMMENT);
						int closeCommentOffset = lineOffset + string.indexOf(CLOSE_COMMENT) - OPEN_COMMENT.length();
						uncomment(openCommentOffset, closeCommentOffset);
					} else {
						int openCommentOffset = fDocument.getLineOffset(i);
						int lineDelimiterLength = fDocument.getLineDelimiter(i) == null ? 0 : fDocument.getLineDelimiter(i).length();
						int closeCommentOffset = openCommentOffset + fDocument.getLineLength(i) - lineDelimiterLength + OPEN_COMMENT.length();
						comment(openCommentOffset, closeCommentOffset);
					}
				}
			} catch (BadLocationException e) {
				throw new SourceEditingRuntimeException();
			}
		}

		fModel.changedModel();
		fModel.endRecording(this);
	}
}
