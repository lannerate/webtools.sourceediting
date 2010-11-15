/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others.
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
package org.eclipse.wst.sse.ui.internal.reconcile.validator;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcileResult;
import org.eclipse.jface.text.reconciler.IReconcileStep;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.sse.ui.internal.IReleasable;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.reconcile.DocumentAdapter;
import org.eclipse.wst.sse.ui.internal.reconcile.ReconcileAnnotationKey;
import org.eclipse.wst.sse.ui.internal.reconcile.StructuredTextReconcilingStrategy;
import org.eclipse.wst.sse.ui.internal.reconcile.TemporaryAnnotation;
import org.eclipse.wst.validation.MutableProjectSettings;
import org.eclipse.wst.validation.MutableWorkspaceSettings;
import org.eclipse.wst.validation.ValidationFramework;
import org.eclipse.wst.validation.Validator;
import org.eclipse.wst.validation.internal.IValChangedListener;
import org.eclipse.wst.validation.internal.ValPrefManagerGlobal;
import org.eclipse.wst.validation.internal.ValPrefManagerProject;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;


/**
 * Special validator strategy. Runs validator steps contributed via the
 * <code>org.eclipse.wst.sse.ui.extensions.sourcevalidation</code> extension
 * point
 * 
 * @author pavery
 */
public class ValidatorStrategy extends StructuredTextReconcilingStrategy {

	private String[] fContentTypeIds = null;
	private List fMetaData = null;
	/** validator id (as declared in ext point) -> ReconcileStepForValidator * */
	private HashMap fVidToVStepMap = null;

	/*
	 * List of ValidatorMetaDatas of total scope validators that have been run
	 * since beginProcessing() was called.
	 */
	private List fTotalScopeValidatorsAlreadyRun;
	
	/** Listens to validator preference changes in order to clear out annotations */
	private IValChangedListener fValChangedListener;

	public ValidatorStrategy(ISourceViewer sourceViewer, String contentType) {
		super(sourceViewer);
		fMetaData = new ArrayList();
		fContentTypeIds = calculateParentContentTypeIds(contentType);
		fVidToVStepMap = new HashMap();
		fValChangedListener = new ValChangedListener();
		
		//add listeners
		ValPrefManagerGlobal.getDefault().addListener(this.fValChangedListener);
		ValPrefManagerProject.addListener(this.fValChangedListener);
	}

	public void addValidatorMetaData(ValidatorMetaData vmd) {
		fMetaData.add(vmd);
	}

	public void beginProcessing() {
		if (fTotalScopeValidatorsAlreadyRun == null)
			fTotalScopeValidatorsAlreadyRun = new ArrayList();
		else
			fTotalScopeValidatorsAlreadyRun.clear();
	}

	/**
	 * The content type passed in should be the most specific one. TODO: This
	 * exact method is also in ValidatorMetaData. Should be in a common place.
	 * 
	 * @param contentType
	 * @return
	 */
	private String[] calculateParentContentTypeIds(String contentTypeId) {

		Set parentTypes = new HashSet();

		IContentTypeManager ctManager = Platform.getContentTypeManager();
		IContentType ct = ctManager.getContentType(contentTypeId);
		String id = contentTypeId;

		while (ct != null && id != null) {

			parentTypes.add(id);
			ct = ctManager.getContentType(id);
			if (ct != null) {
				IContentType baseType = ct.getBaseType();
				id = (baseType != null) ? baseType.getId() : null;
			}
		}
		return (String[]) parentTypes.toArray(new String[parentTypes.size()]);
	}

	protected boolean canHandlePartition(String partitionType) {
		ValidatorMetaData vmd = null;
		for (int i = 0; i < fMetaData.size(); i++) {
			vmd = (ValidatorMetaData) fMetaData.get(i);
			if (vmd.canHandlePartitionType(getContentTypeIds(), partitionType))
				return true;
		}
		return false;
	}

	protected boolean containsStep(IReconcileStep step) {
		return fVidToVStepMap.containsValue(step);
	}

	/**
	 * @see org.eclipse.wst.sse.ui.internal.provisional.reconcile.AbstractStructuredTextReconcilingStrategy#createReconcileSteps()
	 */
	public void createReconcileSteps() {
		// do nothing, steps are created
	}

	public void endProcessing() {
		fTotalScopeValidatorsAlreadyRun.clear();
	}

	/**
	 * All content types on which this ValidatorStrategy can run
	 * 
	 * @return
	 */
	public String[] getContentTypeIds() {
		return fContentTypeIds;
	}

	/**
	 * @param tr
	 *            Partition of the region to reconcile.
	 * @param dr
	 *            Dirty region representation of the typed region
	 */
	public void reconcile(ITypedRegion tr, DirtyRegion dr) {
		/*
		 * Abort if no workspace file is known (new validation framework does
		 * not support that scenario) or no validators have been specified
		 * or validation has been disabled
		 */
		IFile file = getFile();
		if (isCanceled() || fMetaData.isEmpty() || areAllValidatorsSuspended(file)) {
			return;
		}

		IDocument doc = getDocument();
		// for external files, this can be null
		if (doc == null)
			return;

		String partitionType = tr.getType();

		ValidatorMetaData vmd = null;
		List annotationsToAdd = new ArrayList();
		List stepsRanOnThisDirtyRegion = new ArrayList(1);
		
		/*
		 * Keep track of the disabled validators by source id for the V2
		 * validators.
		 */
		Set disabledValsBySourceId = new HashSet(20);
		
		/*
		 * Keep track of the disabled validators by class id for the v1
		 * validators.
		 */
		Set disabledValsByClass = new HashSet(20);
		getDisabledValidators(file, disabledValsBySourceId, disabledValsByClass);
				
		/*
		 * Loop through all of the relevant validator meta data to find
		 * supporting validators for this partition type. Don't check
		 * this.canHandlePartition() before-hand since it just loops through
		 * and calls vmd.canHandlePartitionType()...which we're already doing
		 * here anyway to find the right vmd.
		 */
		for (int i = 0; i < fMetaData.size() && !isCanceled(); i++) {
			vmd = (ValidatorMetaData) fMetaData.get(i);
			if (vmd.canHandlePartitionType(getContentTypeIds(), partitionType)) {
				/*
				 * Check if validator is enabled according to validation
				 * preferences before attempting to create/use it
				 */
				if (!disabledValsBySourceId.contains(vmd.getValidatorId()) && !disabledValsByClass.contains(vmd.getValidatorClass())) {
					int validatorScope = vmd.getValidatorScope();
					ReconcileStepForValidator validatorStep = null;
					// get step for partition type
					Object o = fVidToVStepMap.get(vmd.getValidatorId());
					if (o != null) {
						validatorStep = (ReconcileStepForValidator) o;
					}
					else {
						// if doesn't exist, create one
						IValidator validator = vmd.createValidator();

						validatorStep = new ReconcileStepForValidator(validator, validatorScope);
						validatorStep.setInputModel(new DocumentAdapter(doc));

						fVidToVStepMap.put(vmd.getValidatorId(), validatorStep);
					}

					if (!fTotalScopeValidatorsAlreadyRun.contains(vmd)) {
						annotationsToAdd.addAll(Arrays.asList(validatorStep.reconcile(dr, dr)));
						stepsRanOnThisDirtyRegion.add(validatorStep);

						if (validatorScope == ReconcileAnnotationKey.TOTAL) {
							// mark this validator as "run"
							fTotalScopeValidatorsAlreadyRun.add(vmd);
						}
					}
				}
			}
		}
		
		TemporaryAnnotation[] annotationsToRemove = getAnnotationsToRemove(dr, stepsRanOnThisDirtyRegion);
		if (annotationsToRemove.length + annotationsToAdd.size() > 0)
			smartProcess(annotationsToRemove, (IReconcileResult[]) annotationsToAdd.toArray(new IReconcileResult[annotationsToAdd.size()]));
	}

	public void release() {
		super.release();
		
		//remove listeners
		ValPrefManagerGlobal.getDefault().removeListener(this.fValChangedListener);
		ValPrefManagerProject.removeListener(this.fValChangedListener);
		
		Iterator it = fVidToVStepMap.values().iterator();
		IReconcileStep step = null;
		while (it.hasNext()) {
			step = (IReconcileStep) it.next();
			if (step instanceof IReleasable)
				((IReleasable) step).release();
		}
	}

	/**
	 * @see org.eclipse.wst.sse.ui.internal.reconcile.AbstractStructuredTextReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
	 */
	public void setDocument(IDocument document) {

		super.setDocument(document);

		// validator steps are in "fVIdToVStepMap" (as opposed to fFirstStep >
		// next step etc...)
		Iterator it = fVidToVStepMap.values().iterator();
		IReconcileStep step = null;
		while (it.hasNext()) {
			step = (IReconcileStep) it.next();
			step.setInputModel(new DocumentAdapter(document));
		}
	}

	/**
	 * Gets IFile from current document
	 * 
	 * @return IFile the IFile, null if no such file exists
	 */
	private IFile getFile() {
		IStructuredModel model = null;
		IFile file = null;
		try {
			model = StructuredModelManager.getModelManager().getExistingModelForRead(getDocument());
			if (model != null) {
				String baseLocation = model.getBaseLocation();
				// The baseLocation may be a path on disk or relative to the
				// workspace root. Don't translate on-disk paths to
				// in-workspace resources.
				IPath basePath = new Path(baseLocation);
				if (basePath.segmentCount() > 1) {
					file = ResourcesPlugin.getWorkspace().getRoot().getFile(basePath);
					/*
					 * If the IFile doesn't  exist, make sure it's not
					 * returned
					 */
					if (!file.exists())
						file = null;
				}
			}
		}
		finally {
			if (model != null) {
				model.releaseFromRead();
			}
		}
		return file;
	}
	
	/**
	 * <p>Determines the disabled validators by source id and class for v2 and v1 validators respectively</p>
	 * 
	 * @param file get the disabled validators for this {@link IFile}
	 * @param disabledValsBySourceId return the disabled validators by source ID here
	 * @param disabledValsByClass return the disabled validators by class here
	 */
	private static void getDisabledValidators(IFile file, Set disabledValsBySourceId, Set disabledValsByClass) {
		if (file != null) {
			for (Iterator it = ValidationFramework.getDefault().getDisabledValidatorsFor(file).iterator(); it.hasNext();) {
				Validator v = (Validator) it.next();
				Validator.V1 v1 = v.asV1Validator();
				if (v1 != null)
					disabledValsByClass.add(v1.getId());
				// not a V1 validator
				else if (v.getSourceId() != null) {
					//could be more then one sourceid per batch validator
					String[] sourceIDs = StringUtils.unpack(v.getSourceId());
					disabledValsBySourceId.addAll(Arrays.asList(sourceIDs));
				}
			}
		}
	}
	
	/**
	 * <p>Determines if all validators are suspended for a given file.</p>
	 * 
	 * @param file Determine if all validators are suspended for this {@link IFile}
	 * @return <code>true</code> if all validators are suspended for this file,
	 * <code>false</code> otherwise
	 */
	private static boolean areAllValidatorsSuspended(IFile file) {
		MutableWorkspaceSettings workspaceSettings = null;
		try {
			workspaceSettings = ValidationFramework.getDefault().getWorkspaceSettings();
		} catch (InvocationTargetException e) {
			Logger.logException("Could not get global validation settings", e); //$NON-NLS-1$
		}
		MutableProjectSettings projSettings = ValidationFramework.getDefault().getProjectSettings(file.getProject());
		
		return (workspaceSettings != null && workspaceSettings.getSuspend()) ||
			ValidationFramework.getDefault().isSuspended() ||
			ValidationFramework.getDefault().isSuspended(file.getProject()) ||
			((workspaceSettings == null || workspaceSettings.getOverride()) && projSettings.getOverride() && projSettings.getSuspend());
	}
	
	/**
	 * <p>Used to listen to validator preference changes in order to
	 * clear out annotations when needed</p>
	 */
	private class ValChangedListener implements IValChangedListener {

		/**
		 * @see org.eclipse.wst.validation.internal.IValChangedListener#validatorsForProjectChanged(org.eclipse.core.resources.IProject, boolean)
		 */
		public void validatorsForProjectChanged(IProject project,
				boolean configSettingChanged) {
			
			//remove all the annotations if the val preferences changed
			if(configSettingChanged) {
				IFile file = ValidatorStrategy.this.getFile();
				if(project == null || file.getProject() == project) {
					
					//determine the disabled steps
					Set disabledValsBySourceId = new HashSet(20);
					Set disabledValsByClass = new HashSet(20);
					getDisabledValidators(file, disabledValsBySourceId, disabledValsByClass);
					ValidatorMetaData vmd = null;
					Set disabledSteps = new HashSet();
					for (int i = 0; i < fMetaData.size() && !isCanceled(); i++) {
						vmd = (ValidatorMetaData) fMetaData.get(i);
						if (areAllValidatorsSuspended(file) ||
								disabledValsBySourceId.contains(vmd.getValidatorId()) ||
								disabledValsByClass.contains(vmd.getValidatorClass())) {
							
							Object step = fVidToVStepMap.get(vmd.getValidatorId());
							if(step != null) {
								disabledSteps.add(step);
							}
						}
					}
					
					//clear annotations for the disabled steps
					IAnnotationModel annoModel = ValidatorStrategy.this.getAnnotationModel();
					Iterator iter = annoModel.getAnnotationIterator();
					while(iter.hasNext()) {
						Annotation anno = (Annotation)iter.next();
						if(anno instanceof TemporaryAnnotation) {
							TemporaryAnnotation tempAnno = (TemporaryAnnotation)anno;
							if(tempAnno.getKey() instanceof ReconcileAnnotationKey) {
								ReconcileAnnotationKey key = (ReconcileAnnotationKey)tempAnno.getKey();
								IReconcileStep step = key.getStep();
								if(disabledSteps.contains(step)) {
									annoModel.removeAnnotation(anno);
								}
							}
						}
					}
				}
			}
		}
	}
}
