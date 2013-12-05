/*
 * Copyright 2012 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.services.task.commands;

import javax.enterprise.util.AnnotationLiteral;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.jboss.seam.transaction.Transactional;
import org.jbpm.services.task.events.AfterTaskExitedEvent;
import org.jbpm.services.task.events.BeforeTaskExitedEvent;
import org.jbpm.services.task.exception.PermissionDeniedException;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.internal.command.Context;
import org.kie.internal.task.api.model.InternalTaskData;

/**
 * Operation.Exit
        : [ new OperationCommand().{
                status = [ Status.Created, Status.Ready, Status.Reserved, Status.InProgress, Status.Suspended ],
                allowed = [ Allowed.BusinessAdministrator ],
                newStatus = Status.Exited
            } ]
 */
@Transactional
@XmlRootElement(name="exit-task-command")
@XmlAccessorType(XmlAccessType.NONE)
public class ExitTaskCommand extends TaskCommand<Void> {

	public ExitTaskCommand() {
	}

    public ExitTaskCommand(long taskId, String userId) {
        this.taskId = taskId;
        this.userId = userId;
    }

    public Void execute(Context cntxt) {
        TaskContext context = (TaskContext) cntxt;
        if (context.getTaskService() != null) {
        	context.getTaskService().exit(taskId, userId);
        	return null;
        }
        Task task = context.getTaskQueryService().getTaskInstanceById(taskId);
        User user = context.getTaskIdentityService().getUserById(userId);
        context.getTaskEvents().select(new AnnotationLiteral<BeforeTaskExitedEvent>() {
        }).fire(task);
        boolean adminAllowed = CommandsUtil.isAllowed(user, getGroupsIds(), task.getPeopleAssignments().getBusinessAdministrators());
      
        if (!adminAllowed ) {
            String errorMessage = "The user" + user + "is not allowed to Start the task " + task.getId();
            throw new PermissionDeniedException(errorMessage);
        }

        
        
            if (task.getTaskData().getStatus().equals(Status.Created) ||
                    task.getTaskData().getStatus().equals(Status.Ready) ||
                    task.getTaskData().getStatus().equals(Status.Reserved) ||
                    task.getTaskData().getStatus().equals(Status.InProgress) ||
                    task.getTaskData().getStatus().equals(Status.Suspended)) {
                
            	((InternalTaskData) task.getTaskData()).setStatus(Status.Exited);

            }
       

        context.getTaskEvents().select(new AnnotationLiteral<AfterTaskExitedEvent>() {
        }).fire(task);

        return null;
    }
}