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
package org.devgateway.toolkit.forms.wicket.components;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;

/**
 * A behavior that adds a required attribute support for an input components.
 */
public class RequiredBehavior extends Behavior {

	private static final long serialVersionUID = -2205308796433011875L;

	@Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("required", "required");
    }
}