/**
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.console.client.user.task.view.more;

import static org.bonitasoft.web.toolkit.client.common.i18n.AbstractI18n.*;
import static org.bonitasoft.web.toolkit.client.ui.utils.DateFormat.FORMAT.*;

import org.bonitasoft.console.client.common.metadata.MetadataTaskBuilder;
import org.bonitasoft.console.client.uib.formatter.Formatter;
import org.bonitasoft.console.client.user.cases.view.ArchivedCaseMoreDetailsPage;
import org.bonitasoft.console.client.user.cases.view.CaseMoreDetailsPage;
import org.bonitasoft.web.rest.model.bpm.flownode.IFlowNodeItem;
import org.bonitasoft.web.rest.model.bpm.flownode.IHumanTaskItem;
import org.bonitasoft.web.toolkit.client.common.util.StringUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class HumanTaskMetadataView extends Composite {

    @UiField(provided = true)
    MetadataMessages messages = new MetadataMessages();

    @UiField(provided = true)
    IHumanTaskItem task;

    @UiField
    AnchorElement caseId;

    @UiField
    SpanElement priority;

    @UiField
    SpanElement assignedTo;

    @UiField
    DivElement doneByContainer;
    
    @UiField
    SpanElement doneBy;

    @UiField
    SpanElement dueDate;

    @UiField
    SpanElement lastUpdateDate;

    @UiField
    LabelElement labelDoneOn;

    @UiField
    SpanElement assignedDate;

    @UiField
    ParagraphElement description;

    interface Binder extends UiBinder<HTMLPanel, HumanTaskMetadataView> {
    }

    protected static Binder binder = GWT.create(Binder.class);

    public HumanTaskMetadataView(final IHumanTaskItem task) {
        this.task = task;
        initWidget(binder.createAndBindUi(this));

        priority.setInnerText(Formatter.formatPriority(task.getPriority()));
        assignedTo.setInnerText(Formatter.formatUser(task.getAssignedUser()));
        dueDate.setInnerText(Formatter.formatDate(task.getDueDate(), DISPLAY_RELATIVE));
        if (!IFlowNodeItem.VALUE_STATE_READY.equals(task.getState())) {
        	if (task.getExecutedByUserId().toLong().equals(task.getExecutedBySubstituteUserId().toLong())) {
        		doneBy.setInnerText(Formatter.formatUser(task.getExecutedByUser()));
        	} else {
        		doneBy.setInnerText(Formatter.formatUser(task.getExecutedBySubstituteUser()) + _(" for ") + Formatter.formatUser(task.getExecutedByUser())); 
        	}
        } else {
        	doneByContainer.removeFromParent();
        }

        lastUpdateDate.setInnerText(Formatter.formatDate(task.getLastUpdateDate(), DISPLAY));
        if (IFlowNodeItem.VALUE_STATE_COMPLETED.equals(task.getState())) {
            labelDoneOn.setInnerText(messages.done_on_label() + ": ");
        } else {
            labelDoneOn.setTitle(messages.last_update_date_title());
            labelDoneOn.setInnerText(messages.last_update_date_label() + ": ");
        }
        assignedDate.setInnerText(Formatter.formatDate(task.getAssignedDate(), DISPLAY));

        if(!StringUtil.isBlank(task.ensureDescription())) {
            description.setInnerText(task.ensureDescription());
        }
        
        MetadataTaskBuilder.setCaseHref(caseId, task, CaseMoreDetailsPage.TOKEN, ArchivedCaseMoreDetailsPage.TOKEN);
    }
}
