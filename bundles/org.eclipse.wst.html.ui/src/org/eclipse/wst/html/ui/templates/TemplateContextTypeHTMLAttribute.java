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
package org.eclipse.wst.html.ui.templates;

import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;
import org.eclipse.wst.xml.ui.templates.TemplateContextTypeIds;

/**
 * Templates of this context type apply to any attributes within HTML content type.
 */
public class TemplateContextTypeHTMLAttribute extends TemplateContextTypeHTML {

	public TemplateContextTypeHTMLAttribute() {
		super(generateContextTypeId(TemplateContextTypeIds.ATTRIBUTE), HTMLUIPlugin.getResourceString("%TemplateContextTypeHTMLAttribute.0")); //$NON-NLS-1$
	}
}
