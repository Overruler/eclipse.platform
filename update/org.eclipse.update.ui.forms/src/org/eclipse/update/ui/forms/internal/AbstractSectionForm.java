/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.ui.forms.internal;
import java.util.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.*;

/**
 * This form class understands form sections.
 * It assumes that they are part of the form
 * and it offers life cycle handling of sections
 * once they are registered.
 */

public abstract class AbstractSectionForm extends AbstractForm {
	public static final int H_SCROLL_INCREMENT = 5;
	public static final int V_SCROLL_INCREMENT = 64;
	protected Vector sections = null;

	public void registerSection(FormSection section) {
		if (sections == null)
			sections = new Vector();
		if (!sections.contains(section))
			sections.add(section);
	}

	public void unregisterSection(FormSection section) {
		if (sections != null && sections.contains(section))
			sections.remove(section);
	}

	public void initialize(Object model) {
		if (sections != null) {
			for (Iterator iter = sections.iterator(); iter.hasNext();) {
				FormSection section = (FormSection) iter.next();
				section.initialize(model);
			}
		}
	}

	public void setFocus() {
		if (sections != null && sections.size() > 0) {
			FormSection firstSection = (FormSection) sections.firstElement();
			firstSection.setFocus();
		}
	}

	public void update() {
		if (sections != null) {
			for (Iterator iter = sections.iterator(); iter.hasNext();) {
				FormSection section = (FormSection) iter.next();
				section.update();
			}
		}
	}

	public void commitChanges(boolean onSave) {
		if (sections != null) {
			for (Iterator iter = sections.iterator(); iter.hasNext();) {
				FormSection section = (FormSection) iter.next();
				if (section.isDirty())
					section.commitChanges(onSave);
			}
		}
	}

	public boolean doGlobalAction(String actionId) {
		Control focusControl = getFocusControl();
		if (focusControl == null)
			return false;

		if (canPerformDirectly(actionId, focusControl))
			return true;
		Composite parent = focusControl.getParent();
		FormSection targetSection = null;
		while (parent != null) {
			Object data = parent.getData();
			if (data != null && data instanceof FormSection) {
				targetSection = (FormSection) data;
				break;
			}
			parent = parent.getParent();
		}
		if (targetSection != null) {
			return targetSection.doGlobalAction(actionId);
		}
		return false;
	}

	protected Control getFocusControl() {
		Control control = getControl();
		if (control == null || control.isDisposed())
			return null;
		Display display = control.getDisplay();
		Control focusControl = display.getFocusControl();
		if (focusControl == null || focusControl.isDisposed())
			return null;
		return focusControl;
	}

	public boolean canPaste(Clipboard clipboard) {
		Control focusControl = getFocusControl();
		if (focusControl == null)
			return false;
		Composite parent = focusControl.getParent();
		FormSection targetSection = null;
		while (parent != null) {
			Object data = parent.getData();
			if (data != null && data instanceof FormSection) {
				targetSection = (FormSection) data;
				break;
			}
			parent = parent.getParent();
		}
		if (targetSection != null) {
			return targetSection.canPaste(clipboard);
		}
		return false;
	}

	public void dispose() {
		if (sections != null) {
			for (Iterator iter = sections.iterator(); iter.hasNext();) {
				FormSection section = (FormSection) iter.next();
				section.dispose();
			}
		}
		super.dispose();
	}

	public static void ensureVisible(ScrolledComposite scomp, Control control) {
		Point controlSize = control.getSize();
		Point controlOrigin = getControlLocation(scomp, control);
		ensureVisible(scomp, controlOrigin, controlSize);
	}

	public static void ensureVisible(ScrolledComposite scomp, Point controlOrigin, Point controlSize) {
		Point area = scomp.getContent().getSize();
		Point scompOrigin = scomp.getOrigin();

		int x = scompOrigin.x;
		int y = scompOrigin.y;

		if (controlOrigin.x + controlSize.x > scompOrigin.x + area.x) {
			x = controlOrigin.x + controlSize.x - area.x;
		}		
		if (controlOrigin.x < x ) {
			 x = controlOrigin.x;
		 }         
		
		if (controlOrigin.y + controlSize.y > scompOrigin.y + area.y) {
			y = controlOrigin.y + controlSize.y - area.y;
		}
		if (controlOrigin.y < y ) {
			 y = controlOrigin.y;
		 }         
		scomp.setOrigin(x, y);
	}
	
	public static Point getControlLocation(ScrolledComposite scomp, Control control) {
		int x = 0;
		int y = 0;
		Control currentControl = control;
		for (;;) {
			if (currentControl == scomp)
				break;
			if (currentControl.getLocation().x > 0)
				x += currentControl.getLocation().x;
			if (currentControl.getLocation().y > 0)
				y += currentControl.getLocation().y;
			currentControl = currentControl.getParent();
		}
		return new Point(x, y);
	}
	
	public static void scrollVertical(ScrolledComposite scomp, boolean up) {
		scroll(scomp, 0, up ? -V_SCROLL_INCREMENT : V_SCROLL_INCREMENT);
	}
	public static void scrollHorizontal(ScrolledComposite scomp, boolean left) {
		scroll(scomp, left ? -H_SCROLL_INCREMENT : H_SCROLL_INCREMENT, 0);
	}
	public static void scrollPage(ScrolledComposite scomp, boolean up) {
		Rectangle clientArea = scomp.getClientArea();
		int increment = up ? -clientArea.height : clientArea.height;
		scroll(scomp, 0, increment);
	}
	private static void scroll(ScrolledComposite scomp, int xoffset, int yoffset) {
		Point origin = scomp.getOrigin();
		Point contentSize = scomp.getContent().getSize();
		int xorigin = origin.x + xoffset;
		int yorigin = origin.y + yoffset;
		xorigin = Math.max(xorigin, 0);
		xorigin = Math.min(xorigin, contentSize.x - 1);
		yorigin = Math.max(yorigin, 0);
		yorigin = Math.min(yorigin, contentSize.y - 1);
		scomp.setOrigin(xorigin, yorigin);
	}

	public static void updatePageIncrement(ScrolledComposite scomp) {
		ScrollBar vbar = scomp.getVerticalBar();
		if (vbar != null) {
			Rectangle clientArea = scomp.getClientArea();
			int increment = clientArea.height - 5;
			vbar.setPageIncrement(increment);
		}
	}
}