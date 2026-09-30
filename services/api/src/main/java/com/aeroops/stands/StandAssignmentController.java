package com.aeroops.stands;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
public class StandAssignmentController {

    private final StandAssignmentService standAssignmentService;

    public StandAssignmentController(StandAssignmentService standAssignmentService) {
        this.standAssignmentService = standAssignmentService;
    }

    @PostMapping("/v1/stand-assignments")
    @PreAuthorize("hasAnyRole('CONTROLLER', 'PLANNER', 'TENANT_ADMIN')")
    public ResponseEntity<?> assign(@RequestBody AssignStandRequest request) {
        StandAssignmentResult result = standAssignmentService.assign(request);
        if (result.conflict()) {
            List<StandAssignmentView> conflicting = result.conflicting().stream()
                    .map(StandAssignmentView::from)
                    .toList();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("conflicting", conflicting));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(StandAssignmentView.from(result.assignment()));
    }

    @GetMapping("/v1/stands/board")
    public List<StandAssignmentView> board(@RequestParam("date") LocalDate date) {
        return standAssignmentService.board(date).stream()
                .map(StandAssignmentView::from)
                .toList();
    }
}
