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
package org.eclipse.jst.jsp.ui.internal.java.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jst.jsp.core.internal.java.search.JSPSearchScope;
import org.eclipse.jst.jsp.core.internal.java.search.JSPSearchSupport;
import org.eclipse.jst.jsp.ui.internal.JSPUIPlugin;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * @author pavery
 */
public class JSPPackageRenameChange extends Change {

	public static Change[] createChangesFor(IPackageFragment pkg, String newName) {
		JSPSearchSupport support = JSPSearchSupport.getInstance();
		
		// should be handled by JSPIndexManager
		// https://w3.opensource.ibm.com/bugzilla/show_bug.cgi?id=3036
		//support.indexWorkspaceAndWait();
		
		BasicRefactorSearchRequestor requestor = new JSPPackageRenameRequestor(pkg, newName);
		support.searchRunnable(pkg, new JSPSearchScope(), requestor);

		return requestor.getChanges();
	}

	public String getName() {
		return JSPUIPlugin.getResourceString("%JSP_changes"); //$NON-NLS-1$
	}

	public void initializeValidationData(IProgressMonitor pm) {
		// pa_TODO implement
		// must be implemented to decide correct value of isValid
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		// pa_TODO implement
		// This method must ensure that the change object is still valid.
		// This is in particular interesting when performing an undo change
		// since the workspace could have changed since the undo change has
		// been created.
		return new RefactoringStatus();
	}

	public Change perform(IProgressMonitor pm) throws CoreException {
		// TODO return the "undo" change here
		return null;
	}

	public Object getModifiedElement() {
		
		//return this.pkg;
		return null;
	}
}