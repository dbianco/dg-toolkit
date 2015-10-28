/*******************************************************************************
 * Copyright (c) 2015 Development Gateway, Inc and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/MIT
 *
 * Contributors:
 * Development Gateway - initial API and implementation
 *******************************************************************************/
package org.devgateway.toolkit.forms.wicket.page.edit;

import javax.persistence.EntityManager;

import nl.dries.wicket.hibernate.dozer.DozerModel;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Classes;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.apache.wicket.validation.ValidationError;
import org.devgateway.toolkit.forms.WebConstants;
import org.devgateway.toolkit.forms.exceptions.NullJpaRepositoryException;
import org.devgateway.toolkit.forms.exceptions.NullListPageClassException;
import org.devgateway.toolkit.forms.wicket.components.ComponentUtil;
import org.devgateway.toolkit.forms.wicket.components.form.BootstrapCancelButton;
import org.devgateway.toolkit.forms.wicket.components.form.BootstrapDeleteButton;
import org.devgateway.toolkit.forms.wicket.components.form.BootstrapSubmitButton;
import org.devgateway.toolkit.forms.wicket.components.form.GenericBootstrapFormComponent;
import org.devgateway.toolkit.forms.wicket.page.BasePage;
import org.devgateway.toolkit.persistence.dao.GenericPersistable;
import org.devgateway.toolkit.reporting.spring.util.ReportsCacheService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationMessage;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapForm;
import de.agilecoders.wicket.core.util.Attributes;

/**
 * @author mpostelnicu
 * Page used to make editing easy, extend to get easy access to one entity for editing
 */
public abstract class AbstractEditPage<T extends GenericPersistable> extends BasePage {
    protected static Logger logger = Logger.getLogger(AbstractEditPage.class);

	private static final long serialVersionUID = -5928614890244382103L;

	/**
	 * Factory method for the new instance of the entity being editing. This will be invoked only when
	 * the parameter PARAM_ID is null
	 * @return
	 */
	protected abstract T newInstance();

	
	/**
	 * The repository used to fetch and save the entity, this is initialized in subclasses
	 */
	protected JpaRepository<T, Long> jpaRepository;
	
	/**
	 * The page that is responsible for listing the entities (used here as a return reference after successful save)
	 */
	protected Class<? extends BasePage> listPageClass;
	
	/**
	 * The form used by all subclasses 
	 */
	protected EditForm editForm;
	
	/**
	 * the entity id, or null if a new entity is requested
	 */
	protected Long entityId;
	
	
	/**
	 * This is a wrapper model that ensures we can easily edit the properties of the entity
	 */
	protected CompoundPropertyModel<T> compoundModel;

	/**
	 * generic submit button for the form
	 */
	protected BootstrapSubmitButton saveButton;
	
	/**
	 * generic delete button for the form 
	 */
	protected BootstrapDeleteButton deleteButton;
	
	 @SpringBean
     protected EntityManager entityManager;
	
		
	@SpringBean
	protected ReportsCacheService reportsCacheService;
	 

	public GenericBootstrapValidationVisitor getBootstrapValidationVisitor(AjaxRequestTarget target) {
		return new GenericBootstrapValidationVisitor(target);
	}
		
	 
	/**
	 * Traverses all fields and refreshes the ones that are not valid, so that we can see the errors
	 * @author mpostelnicu
	 *
	 */
	public class GenericBootstrapValidationVisitor implements IVisitor<GenericBootstrapFormComponent<?, ?>, Void> {

		protected AjaxRequestTarget target;
		
		protected GenericBootstrapFormComponent<?, ?> lastInvalidVisitedObject;

		public GenericBootstrapValidationVisitor(AjaxRequestTarget target) {
			this.target = target;
		}
		
		@Override
		public void component(GenericBootstrapFormComponent<?, ?> object, IVisit<Void> visit) {
			visit.dontGoDeeper();
			if (object.getField().isValid())
				return;
			target.add(object.getBorder());

			//remember last invalid visited object, we used this later to trigger the visibility of its parent container, if it is folded
			lastInvalidVisitedObject=object;
			
			
			//there's no point in visiting anything else, we already have a section with error. This hugely improves speed of large forms
			//visit.stop();
		}

		public GenericBootstrapFormComponent<?, ?> getLastInvalidVisitedObject() {
			return lastInvalidVisitedObject;
		}

	}

	public class EditForm extends BootstrapForm<T> {
		private static final long serialVersionUID = -9127043819229346784L;
		

		/**
		 * wrap the model with a {@link CompoundPropertyModel} to ease editing of fields
		 * @param model
		 */
		public void setCompoundPropertyModel(final IModel<T> model) {
			compoundModel=new CompoundPropertyModel<T>(model);
			setModel(compoundModel);
		}

		public EditForm(final String id, final IModel<T> model) {
			this(id);
			setCompoundPropertyModel(model);
		}

		public EditForm(final String id) {
			super(id);
			
			setOutputMarkupId(true);
	
			saveButton = getSaveEditPageButton();
			add(saveButton);
			
			

			deleteButton = getDeleteEditPageButton();
			add(deleteButton);
			// don't display the delete button if we just create a new entity
			if (entityId == null) {
				deleteButton.setVisibilityAllowed(false);
			}

			add(new BootstrapCancelButton("cancel", new StringResourceModel("cancelButton", this, null)) {
				private static final long serialVersionUID = -249084359200507749L;

				@Override
				protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
					setResponsePage(listPageClass);
				}
			});
		}
	}

	/**
	 * Generic funcionality for the save page button, this can be extended further by subclasses
	 * @author mpostelnicu
	 *
	 */
	public class SaveEditPageButton extends BootstrapSubmitButton {
		private static final long serialVersionUID = 9075809391795974349L;
		
		public boolean redirect = true;

		protected boolean redirectToSelf = false;

		public SaveEditPageButton(String id, IModel<String> model) {
			super(id, model);
		}

		@Override
		protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
			// save the object and go back to the list page
			T saveable = editForm.getModelObject();
			
			//saves the entity and flushes the changes
			jpaRepository.saveAndFlush(saveable);
			
			//clears session and detaches all entities that are currently attached
			entityManager.clear();
			
			//we flush the mondrian cache to ensure it gets rebuilt
			reportsCacheService.flushCache();
			
			// only redirect if redirect is true
			if (redirectToSelf) {
				// we need to close the blockUI if it's opened and enable all the buttons
				target.appendJavaScript("$.unblockUI();");
				target.appendJavaScript("$('#" + editForm.getMarkupId() + " button').prop('disabled', false);");
			} else if (redirect) {
				setResponsePage(getResponsePage(), getParameterPage());
			}

			// redirect is set back to true, which is the default behavior
			redirect = true;
            redirectToSelf = false;
		}

		/**
		 * by default, submit button returns back to listPage
		 * @return
		 */
		protected Class<? extends BasePage> getResponsePage() {
			return listPageClass;
		}

		/**
		 * no params by default
		 * @return
		 */
		protected PageParameters getParameterPage() {
			return null;
		}

		@Override
		protected void onError(final AjaxRequestTarget target, Form<?> form) {
			//make all errors visible
			GenericBootstrapValidationVisitor genericBootstrapValidationVisitor = getBootstrapValidationVisitor(target);
			editForm.visitChildren(GenericBootstrapFormComponent.class, genericBootstrapValidationVisitor);
			
			ValidationError error = new ValidationError();
			error.addKey("formHasErrors");
			error(error);

			target.add(feedbackPanel);

			// autoscroll down to the feedback panel
			target.appendJavaScript("$('html, body').animate({scrollTop: $(\".feedbackPanel\").offset().top}, 500);");
		}


		/**
		 * @return the redirect
		 */
		public boolean isRedirect() {
			return redirect;
		}


		/**
		 * @param redirect the redirect to set
		 */
		public void setRedirect(boolean redirect) {
			this.redirect = redirect;
		}


		/**
		 * @param redirectToSelf the redirectToSelf to set
		 */
		public void setRedirectToSelf(boolean redirectToSelf) {
			this.redirectToSelf = redirectToSelf;
		}


		/**
		 * @return the redirectToSelf
		 */
		public boolean isRedirectToSelf() {
			return redirectToSelf;
		}
	}
	


	public class DeleteEditPageButton extends BootstrapDeleteButton {
		private static final long serialVersionUID = 865330306716770506L;

		public DeleteEditPageButton(String id, IModel<String> model) {
			super(id, model);
		}

		@Override
		protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
			T deleteable = editForm.getModelObject();
			try {
				jpaRepository.delete(deleteable);
			} catch (DataIntegrityViolationException e) {
				error(new NotificationMessage(new StringResourceModel("delete_error_message", AbstractEditPage.this, null))
						.hideAfter(Duration.NONE));
				target.add(feedbackPanel);

				return;
			}
			setResponsePage(listPageClass);
		}
		
		@Override
		protected void onError(AjaxRequestTarget target, Form<?> form) {
			target.add(feedbackPanel);
		}
	}
	
	/**
	 * Override this to create new save buttons with additional behaviors
	 * @return
	 */
	public SaveEditPageButton getSaveEditPageButton() {
		return new SaveEditPageButton("save", new StringResourceModel("saveButton", this, null));
	}
	
	
	
	
	/**
	 * Override this to create new delete buttons if you need additional behavior
	 * @return
	 */
	public DeleteEditPageButton getDeleteEditPageButton() {
		return new DeleteEditPageButton("delete", new StringResourceModel("deleteButton", this, null));
	}
	
	public AbstractEditPage(PageParameters parameters) {
		super(parameters);
		
		if (!parameters.get(WebConstants.PARAM_ID).isNull()) {
			entityId = parameters.get(WebConstants.PARAM_ID).toLongObject();
		}

		editForm = new EditForm("editForm") {
            @Override
            protected void onComponentTag(final ComponentTag tag){
                super.onComponentTag(tag);

                if (ComponentUtil.isViewMode()) {
                    Attributes.addClass(tag, "print-view");
                }
            }
        };

		// use this in order to avoid "ServletRequest does not contain multipart content" error
		// this error appears when we have a file upload component that is hidden or not present in the page when the form is created
		editForm.setMultiPart(true);

		add(editForm);

		//this fragment ensures extra buttons are added below the wicket:child section in child 
		Fragment fragment = new  Fragment ("extraButtons", "noButtons", this);
		editForm.add(fragment);

	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		//we cant do anything if we dont have a jparepository here
		if (jpaRepository == null) {
			throw new NullJpaRepositoryException();
		}
		
		//we dont like receiving null list pages
		if (listPageClass == null) {
			throw new NullListPageClassException();
		}

		IModel<T> model = null;
		
		if (entityId != null) {
			model = new DozerModel<>(jpaRepository.findOne(entityId));
		} else {
			T instance = newInstance();
			if(instance!=null) model = new Model<>(instance);
		}

		if(model!=null) editForm.setCompoundPropertyModel(model);
	}

	protected String getClassName() {
		return Classes.simpleName(getClass());
	}
}