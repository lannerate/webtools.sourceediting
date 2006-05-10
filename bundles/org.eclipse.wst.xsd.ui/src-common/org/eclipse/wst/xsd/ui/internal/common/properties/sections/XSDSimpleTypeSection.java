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
package org.eclipse.wst.xsd.ui.internal.common.properties.sections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.ibm.icu.util.StringTokenizer;

import org.apache.xerces.util.XMLChar;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.xsd.ui.internal.actions.CreateElementAction;
import org.eclipse.wst.xsd.ui.internal.actions.DOMAttribute;
import org.eclipse.wst.xsd.ui.internal.common.commands.UpdateNameCommand;
import org.eclipse.wst.xsd.ui.internal.common.util.Messages;
import org.eclipse.wst.xsd.ui.internal.editor.XSDEditorPlugin;
import org.eclipse.wst.xsd.ui.internal.util.XSDDOMHelper;
import org.eclipse.xsd.XSDNamedComponent;
import org.eclipse.xsd.XSDSimpleTypeDefinition;
import org.eclipse.xsd.XSDVariety;
import org.eclipse.xsd.util.XSDConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XSDSimpleTypeSection extends AbstractSection
{
  protected Text nameText;
  CCombo varietyCombo;
  Text typesText;
  CLabel typesLabel;
  Button button;
  XSDSimpleTypeDefinition memberTypeDefinition, itemTypeDefinition, baseTypeDefinition;

  public XSDSimpleTypeSection()
  {
    super();
  }

  protected void createContents(Composite parent)
  {
    TabbedPropertySheetWidgetFactory factory = getWidgetFactory();
    composite = factory.createFlatFormComposite(parent);

    GridData data = new GridData();

    GridLayout gridLayout = new GridLayout();
    gridLayout.marginTop = 0;
    gridLayout.marginBottom = 0;
    gridLayout.numColumns = 3;
    composite.setLayout(gridLayout);

    // ------------------------------------------------------------------
    // NameLabel
    // ------------------------------------------------------------------

    data.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
    data.grabExcessHorizontalSpace = false;
    CLabel nameLabel = factory.createCLabel(composite, Messages._UI_LABEL_NAME);
    nameLabel.setLayoutData(data);

    // ------------------------------------------------------------------
    // NameText
    // ------------------------------------------------------------------
    data = new GridData();
    data.grabExcessHorizontalSpace = true;
    data.horizontalAlignment = GridData.FILL;
    nameText = getWidgetFactory().createText(composite, ""); //$NON-NLS-1$
    nameText.setLayoutData(data);
    applyAllListeners(nameText);

    // ------------------------------------------------------------------
    // DummyLabel
    // ------------------------------------------------------------------
    getWidgetFactory().createCLabel(composite, ""); //$NON-NLS-1$

    // Variety Label
    CLabel label = getWidgetFactory().createCLabel(composite, XSDEditorPlugin.getXSDString("_UI_LABEL_VARIETY")); //$NON-NLS-1$

    // Variety Combo
    data = new GridData();
    data.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
    data.grabExcessHorizontalSpace = false;
    label.setLayoutData(data);

    varietyCombo = getWidgetFactory().createCCombo(composite, SWT.FLAT);
    data = new GridData();
    data.grabExcessHorizontalSpace = true;
    data.horizontalAlignment = GridData.FILL;

    List list = XSDVariety.VALUES;
    Iterator iter = list.iterator();
    while (iter.hasNext())
    {
      varietyCombo.add(((XSDVariety) iter.next()).getName());
    }
    varietyCombo.addSelectionListener(this);
    varietyCombo.setLayoutData(data);

    // ------------------------------------------------------------------
    // DummyLabel
    // ------------------------------------------------------------------
    getWidgetFactory().createCLabel(composite, ""); //$NON-NLS-1$

    // ------------------------------------------------------------------
    // Types Label
    // ------------------------------------------------------------------
    typesLabel = getWidgetFactory().createCLabel(composite, XSDEditorPlugin.getXSDString("_UI_LABEL_MEMBERTYPES")); //$NON-NLS-1$

    // ------------------------------------------------------------------
    // Types Text
    // ------------------------------------------------------------------
    typesText = getWidgetFactory().createText(composite, ""); //$NON-NLS-1$
    typesText.addListener(SWT.Modify, this);
    data = new GridData();
    data.grabExcessHorizontalSpace = true;
    data.horizontalAlignment = GridData.FILL;
    typesText.setLayoutData(data);

    button = getWidgetFactory().createButton(composite, "", SWT.PUSH); //$NON-NLS-1$
    button.setImage(XSDEditorPlugin.getXSDImage("icons/browsebutton.gif")); //$NON-NLS-1$
    button.addSelectionListener(this);
  }
  
  public void setInput(IWorkbenchPart part, ISelection selection)
  {
    super.setInput(part, selection);
    relayout();
  }

  protected void relayout()
  {
    Composite parentComposite = composite.getParent();
    parentComposite.getParent().setRedraw(false);

    if (parentComposite != null && !parentComposite.isDisposed())
    {
      Control[] children = parentComposite.getChildren();
      for (int i = 0; i < children.length; i++)
      {
        children[i].dispose();
      }
    }

    // Now initialize the new handler
    createContents(parentComposite);
    parentComposite.getParent().layout(true, true);

    // Now turn painting back on
    parentComposite.getParent().setRedraw(true);
    refresh();
  }

  public void refresh()
  {
    super.refresh();

    setListenerEnabled(false);
    if (isReadOnly)
    {
      composite.setEnabled(false);
    }
    else
    {
      composite.setEnabled(true);
    }

    nameText.setText(""); //$NON-NLS-1$
    varietyCombo.setText(""); //$NON-NLS-1$
    typesText.setText(""); //$NON-NLS-1$
    typesLabel.setText(XSDEditorPlugin.getXSDString("_UI_LABEL_BASE_TYPE_WITH_COLON")); //$NON-NLS-1$

    if (input instanceof XSDSimpleTypeDefinition)
    {
      XSDSimpleTypeDefinition st = (XSDSimpleTypeDefinition) input;
      String simpleTypeName = st.getName();
      if (simpleTypeName != null)
      {
        nameText.setText(simpleTypeName);
      }
      else
      {
        nameText.setText("**anonymous**"); //$NON-NLS-1$
      }
      
      String variety = st.getVariety().getName();
      int intVariety = st.getVariety().getValue();

      if (variety != null)
      {
        varietyCombo.setText(variety);
        if (intVariety == XSDVariety.ATOMIC)
        {
          baseTypeDefinition = st.getBaseTypeDefinition();
          String name = ""; //$NON-NLS-1$
          if (baseTypeDefinition != null)
          {
            name = baseTypeDefinition.getName();
          }
          typesText.setText(name);
          typesLabel.setText(XSDEditorPlugin.getXSDString("_UI_LABEL_BASE_TYPE_WITH_COLON")); //$NON-NLS-1$
        }
        else if (intVariety == XSDVariety.LIST)
        {
          itemTypeDefinition = st.getItemTypeDefinition();
          String name = ""; //$NON-NLS-1$
          if (itemTypeDefinition != null)
          {
            name = itemTypeDefinition.getName();
          }
          typesText.setText(name);
          typesLabel.setText(XSDEditorPlugin.getXSDString("_UI_LABEL_ITEM_TYPE")); //$NON-NLS-1$
        }
        else if (intVariety == XSDVariety.UNION)
        {
          List memberTypesList = st.getMemberTypeDefinitions();
          StringBuffer sb = new StringBuffer();
          for (Iterator i = memberTypesList.iterator(); i.hasNext();)
          {
            XSDSimpleTypeDefinition typeObject = (XSDSimpleTypeDefinition) i.next();
            String name = typeObject.getQName();
            if (name != null)
            {
              sb.append(name);
              if (i.hasNext())
              {
                sb.append(" "); //$NON-NLS-1$
              }
            }
          }
          String memberTypes = sb.toString();
          typesText.setText(memberTypes);
          typesLabel.setText(XSDEditorPlugin.getXSDString("_UI_LABEL_MEMBERTYPES")); //$NON-NLS-1$
        }
      }
    }
    setListenerEnabled(true);

  }

  public void widgetSelected(SelectionEvent e)
  {
    if (e.widget == varietyCombo)
    {
      if (input != null)
      {
        if (input instanceof XSDSimpleTypeDefinition)
        {
          XSDSimpleTypeDefinition st = (XSDSimpleTypeDefinition) input;
          Element parent = st.getElement();

          String variety = varietyCombo.getText();
          if (variety.equals(XSDVariety.ATOMIC_LITERAL.getName()))
          {
            typesLabel.setText(XSDEditorPlugin.getXSDString("_UI_LABEL_BASE_TYPE_WITH_COLON")); //$NON-NLS-1$
            st.setVariety(XSDVariety.ATOMIC_LITERAL);
            addCreateElementActionIfNotExist(XSDConstants.RESTRICTION_ELEMENT_TAG, XSDEditorPlugin.getXSDString("_UI_ACTION_ADD_RESTRICTION"), parent, null); //$NON-NLS-1$
          }
          else if (variety.equals(XSDVariety.UNION_LITERAL.getName()))
          {
            typesLabel.setText(XSDEditorPlugin.getXSDString("_UI_LABEL_MEMBERTYPES")); //$NON-NLS-1$
            st.setVariety(XSDVariety.UNION_LITERAL);
            addCreateElementActionIfNotExist(XSDConstants.UNION_ELEMENT_TAG, XSDEditorPlugin.getXSDString("_UI_ACTION_ADD_UNION"), parent, null); //$NON-NLS-1$
          }
          else if (variety.equals(XSDVariety.LIST_LITERAL.getName()))
          {
            typesLabel.setText(XSDEditorPlugin.getXSDString("_UI_LABEL_ITEM_TYPE")); //$NON-NLS-1$
            st.setVariety(XSDVariety.LIST_LITERAL);
            addCreateElementActionIfNotExist(XSDConstants.LIST_ELEMENT_TAG, XSDEditorPlugin.getXSDString("_UI_ACTION_ADD_LIST"), parent, null); //$NON-NLS-1$
          }
        }
      }
    }
    else if (e.widget == button)
    {
//      Shell shell = Display.getCurrent().getActiveShell();
//      Element element = ((XSDConcreteComponent) input).getElement();
//      Dialog dialog = null;
//      String property = "";
//      Element secondaryElement = null;

//      IFile currentIFile = ((IFileEditorInput) getActiveEditor().getEditorInput()).getFile();
      
      // issue (cs) need to move to common.ui's selection dialog
      /*
      XSDComponentSelectionProvider provider = new XSDComponentSelectionProvider(currentIFile, xsdSchema);
      dialog = new XSDComponentSelectionDialog(shell, XSDEditorPlugin.getXSDString("_UI_LABEL_SET_TYPE"), provider);
      provider.setDialog((XSDComponentSelectionDialog) dialog);

      if (input instanceof XSDSimpleTypeDefinition)
      {
        XSDSimpleTypeDefinition st = (XSDSimpleTypeDefinition) input;
        Element simpleTypeElement = st.getElement();
        if (st.getVariety() == XSDVariety.LIST_LITERAL)
        {
          Element listElement = (Element) itemTypeDefinition.getElement();
          // dialog = new TypesDialog(shell, listElement,
          // XSDConstants.ITEMTYPE_ATTRIBUTE, xsdSchema);
          // dialog.showComplexTypes = false;
          provider.showComplexTypes(false);

          secondaryElement = listElement;
          property = XSDConstants.ITEMTYPE_ATTRIBUTE;
        }
        else if (st.getVariety() == XSDVariety.ATOMIC_LITERAL)
        {
          Element derivedByElement = (Element) baseTypeDefinition.getElement();
          if (derivedByElement != null)
          {
            // dialog = new TypesDialog(shell, derivedByElement,
            // XSDConstants.BASE_ATTRIBUTE, xsdSchema);
            // dialog.showComplexTypes = false;
            provider.showComplexTypes(false);

            secondaryElement = derivedByElement;
            property = XSDConstants.BASE_ATTRIBUTE;
          }
          else
          {
            return;
          }
        }
        else if (st.getVariety() == XSDVariety.UNION_LITERAL)
        {
          SimpleContentUnionMemberTypesDialog unionDialog = new SimpleContentUnionMemberTypesDialog(shell, st);
          unionDialog.setBlockOnOpen(true);
          unionDialog.create();

          int result = unionDialog.open();
          if (result == Window.OK)
          {
            String newValue = unionDialog.getResult();
            // beginRecording(XSDEditorPlugin.getXSDString("_UI_LABEL_MEMBERTYPES_CHANGE"),
            // element); //$NON-NLS-1$
            Element unionElement = (Element) memberTypeDefinition.getElement();
            unionElement.setAttribute(XSDConstants.MEMBERTYPES_ATTRIBUTE, newValue);

            if (newValue.length() > 0)
            {
              unionElement.setAttribute(XSDConstants.MEMBERTYPES_ATTRIBUTE, newValue);
            }
            else
            {
              unionElement.removeAttribute(XSDConstants.MEMBERTYPES_ATTRIBUTE);
            }
            // endRecording(unionElement);
            refresh();
          }
          return;
        }
        else
        {
          property = "type";
        }
      }
      else
      {
        property = "type";
      }
      // beginRecording(XSDEditorPlugin.getXSDString("_UI_TYPE_CHANGE"),
      // element); //$NON-NLS-1$
      dialog.setBlockOnOpen(true);
      dialog.create();
      int result = dialog.open();

      if (result == Window.OK)
      {
        if (secondaryElement == null)
        {
          secondaryElement = element;
        }
        XSDSetTypeHelper helper = new XSDSetTypeHelper(currentIFile, xsdSchema);
        helper.setType(secondaryElement, property, ((XSDComponentSelectionDialog) dialog).getSelection());

        XSDSimpleTypeDefinition st = (XSDSimpleTypeDefinition) input;
        st.setElement(element);
        updateSimpleTypeFacets();*/
      }
      // endRecording(element);
    
    refresh();
  }

  public boolean shouldUseExtraSpace()
  {
    return false;
  }

  // issue (cs) this method seems to be utilizing 'old' classes, can we reimplement?
  // (e.g. ChangeElementAction, XSDDOMHelper, etc)
  protected boolean addCreateElementActionIfNotExist(String elementTag, String label, Element parent, Node relativeNode)
  {
    XSDSimpleTypeDefinition st = (XSDSimpleTypeDefinition) input;
    List attributes = new ArrayList();
    String reuseType = null;

    // beginRecording(XSDEditorPlugin.getXSDString("_UI_LABEL_VARIETY_CHANGE"),
    // parent); //$NON-NLS-1$
    if (elementTag.equals(XSDConstants.RESTRICTION_ELEMENT_TAG))
    {
      Element listNode = getFirstChildNodeIfExists(parent, XSDConstants.LIST_ELEMENT_TAG, false);
      if (listNode != null)
      {
        reuseType = listNode.getAttribute(XSDConstants.ITEMTYPE_ATTRIBUTE);
        XSDDOMHelper.removeNodeAndWhitespace(listNode);
      }

      Element unionNode = getFirstChildNodeIfExists(parent, XSDConstants.UNION_ELEMENT_TAG, false);
      if (unionNode != null)
      {
        String memberAttr = unionNode.getAttribute(XSDConstants.MEMBERTYPES_ATTRIBUTE);
        if (memberAttr != null)
        {
          StringTokenizer stringTokenizer = new StringTokenizer(memberAttr);
          reuseType = stringTokenizer.nextToken();
        }
        XSDDOMHelper.removeNodeAndWhitespace(unionNode);
      }

      if (reuseType == null)
      {
        reuseType = getBuiltInStringQName();
      }
      attributes.add(new DOMAttribute(XSDConstants.BASE_ATTRIBUTE, reuseType));
      st.setItemTypeDefinition(null);
    }
    else if (elementTag.equals(XSDConstants.LIST_ELEMENT_TAG))
    {
      Element restrictionNode = getFirstChildNodeIfExists(parent, XSDConstants.RESTRICTION_ELEMENT_TAG, false);
      if (restrictionNode != null)
      {
        reuseType = restrictionNode.getAttribute(XSDConstants.BASE_ATTRIBUTE);
        XSDDOMHelper.removeNodeAndWhitespace(restrictionNode);
      }
      Element unionNode = getFirstChildNodeIfExists(parent, XSDConstants.UNION_ELEMENT_TAG, false);
      if (unionNode != null)
      {
        String memberAttr = unionNode.getAttribute(XSDConstants.MEMBERTYPES_ATTRIBUTE);
        if (memberAttr != null)
        {
          StringTokenizer stringTokenizer = new StringTokenizer(memberAttr);
          reuseType = stringTokenizer.nextToken();
        }
        XSDDOMHelper.removeNodeAndWhitespace(unionNode);
      }
      attributes.add(new DOMAttribute(XSDConstants.ITEMTYPE_ATTRIBUTE, reuseType));
    }
    else if (elementTag.equals(XSDConstants.UNION_ELEMENT_TAG))
    {
      Element listNode = getFirstChildNodeIfExists(parent, XSDConstants.LIST_ELEMENT_TAG, false);
      if (listNode != null)
      {
        reuseType = listNode.getAttribute(XSDConstants.ITEMTYPE_ATTRIBUTE);
        XSDDOMHelper.removeNodeAndWhitespace(listNode);
      }
      Element restrictionNode = getFirstChildNodeIfExists(parent, XSDConstants.RESTRICTION_ELEMENT_TAG, false);
      if (restrictionNode != null)
      {
        reuseType = restrictionNode.getAttribute(XSDConstants.BASE_ATTRIBUTE);
        XSDDOMHelper.removeNodeAndWhitespace(restrictionNode);
      }
      attributes.add(new DOMAttribute(XSDConstants.MEMBERTYPES_ATTRIBUTE, reuseType));
      st.setItemTypeDefinition(null);
    }

    if (getFirstChildNodeIfExists(parent, elementTag, false) == null)
    {
      Action action = addCreateElementAction(elementTag, label, attributes, parent, relativeNode);
      action.run();
    }

    st.setElement(parent);
    st.updateElement();
    // endRecording(parent);
    return true;
  }

  protected Action addCreateElementAction(String elementTag, String label, List attributes, Element parent, Node relativeNode)
  {
    CreateElementAction action = new CreateElementAction(label);
    action.setElementTag(elementTag);
    action.setAttributes(attributes);
    action.setParentNode(parent);
    action.setRelativeNode(relativeNode);
    return action;
  }

  protected Element getFirstChildNodeIfExists(Node parent, String elementTag, boolean isRef)
  {
    NodeList children = parent.getChildNodes();
    Element targetNode = null;
    for (int i = 0; i < children.getLength(); i++)
    {
      Node child = children.item(i);
      if (child != null && child instanceof Element)
      {
        if (XSDDOMHelper.inputEquals(child, elementTag, isRef))
        {
          targetNode = (Element) child;
          break;
        }
      }
    }
    return targetNode;
  }

  protected String getBuiltInStringQName()
  {
    String stringName = "string"; //$NON-NLS-1$

    if (xsdSchema != null)
    {
      String schemaForSchemaPrefix = xsdSchema.getSchemaForSchemaQNamePrefix();
      if (schemaForSchemaPrefix != null && schemaForSchemaPrefix.length() > 0)
      {
        String prefix = xsdSchema.getSchemaForSchemaQNamePrefix();
        if (prefix != null && prefix.length() > 0)
        {
          stringName = prefix + ":" + stringName; //$NON-NLS-1$
        }
      }
    }
    return stringName;
  }

//  private void updateSimpleTypeFacets()
//  {
//    XSDSimpleTypeDefinition st = (XSDSimpleTypeDefinition) input;
//    Element simpleTypeElement = st.getElement();
//    Element derivedByElement = baseTypeDefinition.getElement();
//    if (derivedByElement != null)
//    {
//      List nodesToRemove = new ArrayList();
//      NodeList childList = derivedByElement.getChildNodes();
//      int length = childList.getLength();
//      for (int i = 0; i < length; i++)
//      {
//        Node child = childList.item(i);
//        if (child instanceof Element)
//        {
//          Element elementChild = (Element) child;
//          if (!(elementChild.getLocalName().equals("pattern") || elementChild.getLocalName().equals("enumeration") || //$NON-NLS-1$ //$NON-NLS-2$
//              XSDDOMHelper.inputEquals(elementChild, XSDConstants.SIMPLETYPE_ELEMENT_TAG, false) || XSDDOMHelper.inputEquals(elementChild, XSDConstants.ANNOTATION_ELEMENT_TAG, false)
//              || XSDDOMHelper.inputEquals(elementChild, XSDConstants.ATTRIBUTE_ELEMENT_TAG, false) || XSDDOMHelper.inputEquals(elementChild, XSDConstants.ATTRIBUTE_ELEMENT_TAG, true)
//              || XSDDOMHelper.inputEquals(elementChild, XSDConstants.ATTRIBUTEGROUP_ELEMENT_TAG, false) || XSDDOMHelper.inputEquals(elementChild, XSDConstants.ATTRIBUTEGROUP_ELEMENT_TAG, true) || XSDDOMHelper.inputEquals(elementChild,
//              XSDConstants.ANYATTRIBUTE_ELEMENT_TAG, false)))
//          {
//            nodesToRemove.add(child);
//          }
//        }
//      }
//      Iterator iter = nodesToRemove.iterator();
//      while (iter.hasNext())
//      {
//        Element facetToRemove = (Element) iter.next();
//        String facetName = facetToRemove.getLocalName();
//        Iterator it = st.getValidFacets().iterator();
//        boolean doRemove = true;
//        while (it.hasNext())
//        {
//          String aValidFacet = (String) it.next();
//          if (aValidFacet.equals(facetName))
//          {
//            doRemove = false;
//            break;
//          }
//        }
//        if (doRemove)
//        {
//          XSDDOMHelper.removeNodeAndWhitespace(facetToRemove);
//        }
//      }
//    }
//  }

  // TODO: Common this up with element declaration
  public void doHandleEvent(Event event) 
  {
    if (event.widget == nameText)
    {
      String newValue = nameText.getText().trim();
      if (input instanceof XSDNamedComponent)
      {
        XSDNamedComponent namedComponent = (XSDNamedComponent)input;
        if (!validateSection())
          return;

        Command command = null;

        // Make sure an actual name change has taken place
        String oldName = namedComponent.getName();
        if (!newValue.equals(oldName))
        {
          command = new UpdateNameCommand(Messages._UI_ACTION_RENAME, namedComponent, newValue);
        }

        if (command != null && getCommandStack() != null)
        {
          getCommandStack().execute(command);
        }

      }
    }
  }
  
  protected boolean validateSection()
  {
    if (nameText == null || nameText.isDisposed())
      return true;

    setErrorMessage(null);

    String name = nameText.getText().trim();

    // validate against NCName
    if (name.length() < 1 || !XMLChar.isValidNCName(name))
    {
      setErrorMessage(Messages._UI_ERROR_INVALID_NAME);
      return false;
    }

    return true;
  }

}
