/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.xsd.ui.internal.common.commands;

import java.util.List;
import java.util.Map;

import org.eclipse.wst.xsd.ui.internal.common.properties.sections.appinfo.SpecificationForExtensionsSchema;
import org.eclipse.wst.xsd.ui.internal.common.util.XSDCommonUIUtils;
import org.eclipse.xsd.XSDAnnotation;
import org.eclipse.xsd.XSDConcreteComponent;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDSchema;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AddExtensionElementCommand extends AddExtensionCommand
{
  XSDElementDeclaration element;
  Element appInfo;
  Element newElement;

  public AddExtensionElementCommand(String label, XSDConcreteComponent input, XSDElementDeclaration element)
  {
    super(label);
    this.component = input;
    this.element = element;
  }

  public void execute()
  {
    super.execute();
    addAnnotationSet(component.getSchema(), extensionsSchemaSpec);
  }

  public void undo()
  {
    super.undo();
    XSDAnnotation xsdAnnotation = XSDCommonUIUtils.getInputXSDAnnotation(component, false);
    xsdAnnotation.getElement().removeChild(appInfo);
    List appInfos = xsdAnnotation.getApplicationInformation();
    appInfos.remove(appInfo);
    xsdAnnotation.updateElement();

  }

  public void setSchemaProperties(SpecificationForExtensionsSchema spec)
  {
    this.extensionsSchemaSpec = spec;
  }

  public void addAnnotationSet(XSDSchema xsdSchema, SpecificationForExtensionsSchema spec)
  {
    XSDAnnotation xsdAnnotation = XSDCommonUIUtils.getInputXSDAnnotation(component, true);
    addAnnotationSet(spec, xsdAnnotation);
  }

  private void addAnnotationSet(SpecificationForExtensionsSchema spec, XSDAnnotation xsdAnnotation)
  {
    appInfo = xsdAnnotation.createApplicationInformation(spec.getNamespaceURI());

    if (appInfo != null)
    {
      Document doc = appInfo.getOwnerDocument();

      Element rootElement = doc.createElementNS(spec.getNamespaceURI(), element.getName());
      
      String prefix = createUniquePrefix(component);
      rootElement.setPrefix(prefix);
      newElement = rootElement;
      
      Attr nsURIAttribute = doc.createAttribute("xmlns:"+prefix); //$NON-NLS-1$
      nsURIAttribute.setValue(spec.getNamespaceURI());
      rootElement.setAttributeNode(nsURIAttribute);
      appInfo.appendChild(rootElement);

      xsdAnnotation.getElement().appendChild(appInfo);
      List appInfos = xsdAnnotation.getApplicationInformation();
      appInfos.add(appInfo);
      xsdAnnotation.updateElement();
    }
  }

  public Object getNewObject()
  {
    return newElement;
  }
  
  protected String createUniquePrefix(XSDConcreteComponent component)
  {
    String prefix = "p"; //$NON-NLS-1$
    Map prefMapper = component.getSchema().getQNamePrefixToNamespaceMap();
    if ( prefMapper.get(prefix) != null){
      int i = 1;
      while ( prefMapper.get(prefix + i) != null)
        i++;
      prefix += i;
    }
    return prefix;
  }  
}
