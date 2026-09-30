package com.aeroops.turnarounds;

import java.util.List;
import java.util.UUID;

public record TaskView(UUID id, String name, int sequence, String status, List<TaskRevisionView> revisions) {

    public static TaskView from(Task task, List<TaskRevision> revisions) {
        return new TaskView(task.getId(), task.getName(), task.getSequence(), task.getStatus(),
                revisions.stream().map(TaskRevisionView::from).toList());
    }
}
