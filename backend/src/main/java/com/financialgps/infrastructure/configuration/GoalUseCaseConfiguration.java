package com.financialgps.infrastructure.configuration;

import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.in.CreateGoal;
import com.financialgps.application.goal.port.in.DeleteGoal;
import com.financialgps.application.goal.port.in.GetGoalCapacity;
import com.financialgps.application.goal.port.in.GetGoals;
import com.financialgps.application.goal.port.in.UpdateGoal;
import com.financialgps.application.goal.port.out.GoalBusinessDate;
import com.financialgps.application.goal.port.out.GoalStore;
import com.financialgps.application.goal.usecase.GoalUseCases;
import com.financialgps.application.profile.port.in.GetProfile;
import com.financialgps.application.goal.port.out.GoalStore;
import com.financialgps.domain.model.OwnerId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.UUID;

/** Explicit goal wiring: one input port per use case, transaction-demarcated at the boundary. */
@Configuration
class GoalUseCaseConfiguration {

    @Bean
    GoalUseCases goalUseCases(GoalStore goals, GetProfile positions, GoalBusinessDate dates) {
        return new GoalUseCases(goals, positions, dates);
    }

    @Bean
    CreateGoal createGoal(GoalUseCases useCases, UseCaseTransactions transactions) {
        return transactions.writable(new CreateGoal() {
            @Override
            public GoalModels.GoalView create(OwnerId owner, GoalModels.GoalCommand command) {
                return useCases.create(owner, command);
            }
        });
    }

    @Bean
    UpdateGoal updateGoal(GoalUseCases useCases, UseCaseTransactions transactions) {
        return transactions.writable(new UpdateGoal() {
            @Override
            public GoalModels.GoalView update(OwnerId owner, UUID id,
                                              GoalModels.GoalUpdateCommand command) {
                return useCases.update(owner, id, command);
            }
        });
    }

    @Bean
    DeleteGoal deleteGoal(GoalUseCases useCases, UseCaseTransactions transactions) {
        return transactions.writable(new DeleteGoal() {
            @Override
            public void delete(OwnerId owner, UUID id) {
                useCases.delete(owner, id);
            }
        });
    }

    @Bean
    GetGoals getGoals(GoalUseCases useCases, UseCaseTransactions transactions) {
        return transactions.readOnly(new GetGoals() {
            @Override
            public GoalModels.GoalView get(OwnerId owner, UUID id) {
                return useCases.get(owner, id);
            }

            @Override
            public List<GoalModels.GoalView> list(OwnerId owner) {
                return useCases.list(owner);
            }
        });
    }

    @Bean
    GetGoalCapacity getGoalCapacity(GoalUseCases useCases, UseCaseTransactions transactions) {
        return transactions.readOnly(new GetGoalCapacity() {
            @Override
            public GoalModels.GoalCapacityView capacity(OwnerId owner, UUID id) {
                return useCases.capacity(owner, id);
            }
        });
    }
}
