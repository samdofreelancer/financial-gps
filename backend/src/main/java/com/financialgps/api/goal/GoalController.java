package com.financialgps.api.goal;

import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.in.CreateGoal;
import com.financialgps.application.goal.port.in.DeleteGoal;
import com.financialgps.application.goal.port.in.GetGoalCapacity;
import com.financialgps.application.goal.port.in.GetGoals;
import com.financialgps.application.goal.port.in.UpdateGoal;
import com.financialgps.application.goal.usecase.GoalValidationException;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.account.port.out.CurrentCaller;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP adapter only: binds/validates DTOs, resolves the actor, invokes input ports. */
@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final CreateGoal createGoal;
    private final UpdateGoal updateGoal;
    private final DeleteGoal deleteGoal;
    private final GetGoals getGoals;
    private final GetGoalCapacity getGoalCapacity;
    private final CurrentCaller owners;

    public GoalController(CreateGoal createGoal, UpdateGoal updateGoal, DeleteGoal deleteGoal,
                          GetGoals getGoals, GetGoalCapacity getGoalCapacity,
                          CurrentCaller owners) {
        this.createGoal = createGoal;
        this.updateGoal = updateGoal;
        this.deleteGoal = deleteGoal;
        this.getGoals = getGoals;
        this.getGoalCapacity = getGoalCapacity;
        this.owners = owners;
    }

    @GetMapping
    public List<GoalModels.GoalView> list(@RequestParam(required = false) String status) {
        OwnerId owner = owners.requireCurrentOwner();
        List<GoalModels.GoalView> views = getGoals.list(owner);
        if (status == null || status.isBlank()) {
            return views;
        }
        if (!"ACTIVE".equals(status) && !"COMPLETED".equals(status)) {
            throw new GoalValidationException("GOAL_STATUS_INVALID",
                    "Status filter must be ACTIVE or COMPLETED");
        }
        return views.stream().filter(v -> status.equals(v.status())).toList();
    }

    @PostMapping
    public ResponseEntity<GoalModels.GoalView> create(
            @Valid @RequestBody GoalDtos.GoalRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        GoalModels.GoalView view = createGoal.create(owner, new GoalModels.GoalCommand(
                request.name(), request.goalType(), request.targetAmount(), request.currentAmount(),
                request.targetDate(), request.priority()));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping("/{id}")
    public GoalModels.GoalView get(@PathVariable UUID id) {
        return getGoals.get(owners.requireCurrentOwner(), id);
    }

    @GetMapping("/{id}/capacity")
    public GoalModels.GoalCapacityView capacity(@PathVariable UUID id) {
        return getGoalCapacity.capacity(owners.requireCurrentOwner(), id);
    }

    @PutMapping("/{id}")
    public GoalModels.GoalView update(@PathVariable UUID id,
                                      @Valid @RequestBody GoalDtos.GoalRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        return updateGoal.update(owner, id, new GoalModels.GoalUpdateCommand(
                request.name(), request.goalType(), request.targetAmount(), request.currentAmount(),
                request.targetDate(), request.priority()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteGoal.delete(owners.requireCurrentOwner(), id);
        return ResponseEntity.noContent().build();
    }
}
