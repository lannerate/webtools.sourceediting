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
package org.eclipse.jst.jsp.ui.style;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.wst.common.encoding.content.IContentTypeIdentifier;
import org.eclipse.wst.html.ui.style.IStyleConstantsHTML;
import org.eclipse.wst.sse.core.text.ITextRegion;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.preferences.PreferenceKeyGenerator;
import org.eclipse.wst.sse.ui.style.AbstractLineStyleProvider;
import org.eclipse.wst.sse.ui.style.LineStyleProvider;
import org.eclipse.wst.xml.core.jsp.model.parser.temp.XMLJSPRegionContexts;
import org.eclipse.wst.xml.core.parser.XMLRegionContext;
import org.eclipse.wst.xml.ui.style.IStyleConstantsXML;

public class LineStyleProviderForJSP extends AbstractLineStyleProvider implements LineStyleProvider{

	private String language = null;

	//    private static final String JAVA = "java"; //$NON-NLS-1$
	//    private static final String[] JAVASCRIPT_LANGUAGE_KEYS = new String[] {
	// "javascript", "javascript1.0", "javascript1.1_3", "javascript1.2",
	// "javascript1.3", "javascript1.4", "javascript1.5", "javascript1.6",
	// "jscript", "sashscript" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	// //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
	// //$NON-NLS-9$ //$NON-NLS-10$

	public LineStyleProviderForJSP() {
		super();
		loadColorsForJSPTags();
	}

	protected void clearColors() {
		getTextAttributes().clear();
	}

	protected TextAttribute getAttributeFor(ITextRegion region) {
		/**
		 * a method to centralize all the "sytle rules" for regions
		 */
		TextAttribute result = null;
		// not sure why this is coming through null, but just to catch it
		if (region == null) {
			result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.CDATA_TEXT);
		}
		else {

			if (result == null) {
				String type = region.getType();
				if ((type == XMLJSPRegionContexts.JSP_SCRIPTLET_OPEN) || (type == XMLJSPRegionContexts.JSP_DECLARATION_OPEN) || (type == XMLJSPRegionContexts.JSP_EXPRESSION_OPEN) || (type == XMLJSPRegionContexts.JSP_DIRECTIVE_OPEN) || (type == XMLJSPRegionContexts.JSP_DIRECTIVE_CLOSE) || (type == XMLJSPRegionContexts.JSP_CLOSE)) {
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsHTML.SCRIPT_AREA_BORDER);
				}
				else if (type == XMLJSPRegionContexts.JSP_DIRECTIVE_NAME || type == XMLJSPRegionContexts.JSP_ROOT_TAG_NAME) {
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.TAG_NAME);
				}
				else if ((type == XMLJSPRegionContexts.JSP_COMMENT_OPEN) || (type == XMLJSPRegionContexts.JSP_COMMENT_CLOSE)) {
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.COMMENT_BORDER);
				}
				else if (type == XMLJSPRegionContexts.JSP_COMMENT_TEXT) {
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.COMMENT_TEXT);
				}
				// ============ These are in common with XML --- (for XML form
				// of tags)
				//              Note: this assume's this provider is only called for
				//              true JSP Nodes. If its called for others, then this will
				//              cause their tag names to be highlighted too!
				//              Further checks could be done to prevent that, but doesn't
				//              seem worth it, since if adpaters factories are working
				// right,
				//              then wouldn't be needed.
				else if (type == XMLRegionContext.XML_TAG_NAME) {
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.TAG_NAME);
				}
				else if ((type == XMLRegionContext.XML_TAG_OPEN) || (type == XMLRegionContext.XML_END_TAG_OPEN) || (type == XMLRegionContext.XML_TAG_CLOSE) || (type == XMLRegionContext.XML_EMPTY_TAG_CLOSE)) {
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.TAG_BORDER);
				}
				else if (type == XMLRegionContext.XML_TAG_ATTRIBUTE_NAME) {
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.TAG_ATTRIBUTE_NAME);
				}
				else if ((type == XMLRegionContext.XML_TAG_ATTRIBUTE_VALUE) || (type == XMLJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE) || (type == XMLJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_SQUOTE)) {
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.TAG_ATTRIBUTE_VALUE);
				}
				else if (type == XMLRegionContext.XML_TAG_ATTRIBUTE_EQUALS) {
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.TAG_ATTRIBUTE_EQUALS);
				}

				// DMW: added 9/1/2002 Undefined color may need addjustment :)
				else if (type == XMLRegionContext.UNDEFINED)
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.XML_CONTENT);

				else if (type == XMLRegionContext.WHITE_SPACE)
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.XML_CONTENT);
				// DMW added 8/30/2002 -- should provide JSP specific
				// preference for "custom tag content" (both tag dependent,
				// BLOCKED_TEXT, and not, XML CONTENT)
				else if (type == XMLRegionContext.XML_CONTENT)
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.XML_CONTENT);
				else if (type == XMLRegionContext.BLOCK_TEXT)
					result = (TextAttribute) getTextAttributes().get(IStyleConstantsXML.CDATA_TEXT);
			}
		}
		// default, return null to signal "not handled"
		// in which case, other factories should be tried
		return result;
	}


	protected IPreferenceStore getColorPreferences() {
		return SSEUIPlugin.getDefault().getPreferenceStore();
	}

	protected String getPreferenceKey(String key) {
		String contentTypeId = IContentTypeIdentifier.ContentTypeID_JSP;
		return PreferenceKeyGenerator.generateKey(key, contentTypeId);
	}

	protected void loadColorsForJSPTags() {
		clearColors();

		addTextAttribute(IStyleConstantsXML.TAG_NAME);
		addTextAttribute(IStyleConstantsXML.TAG_BORDER);
		addTextAttribute(IStyleConstantsXML.TAG_ATTRIBUTE_NAME);
		addTextAttribute(IStyleConstantsXML.TAG_ATTRIBUTE_VALUE);
		addTextAttribute(IStyleConstantsXML.TAG_ATTRIBUTE_EQUALS);
		addTextAttribute(IStyleConstantsXML.COMMENT_BORDER);
		addTextAttribute(IStyleConstantsXML.COMMENT_TEXT);
		addTextAttribute(IStyleConstantsXML.CDATA_BORDER);
		addTextAttribute(IStyleConstantsXML.CDATA_TEXT);
		addTextAttribute(IStyleConstantsXML.DECL_BORDER);
		addTextAttribute(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID);
		addTextAttribute(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_PUBREF);
		addTextAttribute(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_SYSREF);
		addTextAttribute(IStyleConstantsXML.DOCTYPE_NAME);
		addTextAttribute(IStyleConstantsXML.PI_CONTENT);
		addTextAttribute(IStyleConstantsXML.PI_BORDER);
		addTextAttribute(IStyleConstantsXML.XML_CONTENT);
		addTextAttribute(IStyleConstantsHTML.SCRIPT_AREA_BORDER);
	}

	protected void handlePropertyChange(PropertyChangeEvent event) {
		String styleKey = null;

		if (event != null) {
			String prefKey = event.getProperty();
			// check if preference changed is a style preference
			if (getPreferenceKey(IStyleConstantsXML.TAG_NAME).equals(prefKey)) {
				styleKey = IStyleConstantsXML.TAG_NAME;
			}
			else if (getPreferenceKey(IStyleConstantsXML.TAG_BORDER).equals(prefKey)) {
				styleKey = IStyleConstantsXML.TAG_BORDER;
			}
			else if (getPreferenceKey(IStyleConstantsXML.TAG_ATTRIBUTE_NAME).equals(prefKey)) {
				styleKey = IStyleConstantsXML.TAG_ATTRIBUTE_NAME;
			}
			else if (getPreferenceKey(IStyleConstantsXML.TAG_ATTRIBUTE_VALUE).equals(prefKey)) {
				styleKey = IStyleConstantsXML.TAG_ATTRIBUTE_VALUE;
			}
			else if (getPreferenceKey(IStyleConstantsXML.TAG_ATTRIBUTE_EQUALS).equals(prefKey)) {
				styleKey = IStyleConstantsXML.TAG_ATTRIBUTE_EQUALS;
			}
			else if (getPreferenceKey(IStyleConstantsXML.COMMENT_BORDER).equals(prefKey)) {
				styleKey = IStyleConstantsXML.COMMENT_BORDER;
			}
			else if (getPreferenceKey(IStyleConstantsXML.COMMENT_TEXT).equals(prefKey)) {
				styleKey = IStyleConstantsXML.COMMENT_TEXT;
			}
			else if (getPreferenceKey(IStyleConstantsXML.CDATA_BORDER).equals(prefKey)) {
				styleKey = IStyleConstantsXML.CDATA_BORDER;
			}
			else if (getPreferenceKey(IStyleConstantsXML.CDATA_TEXT).equals(prefKey)) {
				styleKey = IStyleConstantsXML.CDATA_TEXT;
			}
			else if (getPreferenceKey(IStyleConstantsXML.DECL_BORDER).equals(prefKey)) {
				styleKey = IStyleConstantsXML.DECL_BORDER;
			}
			else if (getPreferenceKey(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID).equals(prefKey)) {
				styleKey = IStyleConstantsXML.DOCTYPE_EXTERNAL_ID;
			}
			else if (getPreferenceKey(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_PUBREF).equals(prefKey)) {
				styleKey = IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_PUBREF;
			}
			else if (getPreferenceKey(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_SYSREF).equals(prefKey)) {
				styleKey = IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_SYSREF;
			}
			else if (getPreferenceKey(IStyleConstantsXML.DOCTYPE_NAME).equals(prefKey)) {
				styleKey = IStyleConstantsXML.DOCTYPE_NAME;
			}
			else if (getPreferenceKey(IStyleConstantsXML.PI_CONTENT).equals(prefKey)) {
				styleKey = IStyleConstantsXML.PI_CONTENT;
			}
			else if (getPreferenceKey(IStyleConstantsXML.PI_BORDER).equals(prefKey)) {
				styleKey = IStyleConstantsXML.PI_BORDER;
			}
			else if (getPreferenceKey(IStyleConstantsXML.XML_CONTENT).equals(prefKey)) {
				styleKey = IStyleConstantsXML.XML_CONTENT;
			}
			else if (getPreferenceKey(IStyleConstantsHTML.SCRIPT_AREA_BORDER).equals(prefKey)) {
				styleKey = IStyleConstantsHTML.SCRIPT_AREA_BORDER;
			}
		}

		if (styleKey != null) {
			// overwrite style preference with new value
			addTextAttribute(styleKey);
			super.handlePropertyChange(event);
		}
	}

	/**
	 * Returns the language.
	 * 
	 * @return String
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Sets the language.
	 * 
	 * @param language
	 *            The language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
	}
}