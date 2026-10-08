package com.financialgps.infrastructure.configuration;

import com.financialgps.application.goal.port.in.CreateGoal;
import com.financialgps.application.goal.port.in.DeleteGoal;
import com.financialgps.application.goal.port.in.GetGoalCapacity;
import com.financialgps.application.goal.port.in.GetGoals;
import com.financialgps.application.goal.port.in.UpdateGoal;
import com.financialgps.application.goal.port.out.GoalBusinessDate;
import com.financialgps.application.goal.port.out.GoalPositionReader;
import com.financialgps.application.goal.usecase.CreateGoalUseCase;
import com.financialgps.application.goal.usecase.DeleteGoalUseCase;
import com.financialgps.application.goal.usecase.GetGoalCapacityUseCase;
import com.financialgps.application.goal.usecase.GetGoalsUseCase;
import com.financialgps.application.goal.usecase.UpdateGoalUseCase;
import com.financialgps.domain.goal.GoalStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit goal wiring: one input port per use case, each transaction-demarcated at the
 * boundary. Read use cases are read-only, mutations are read-write.
 */
@Configuration
class GoalUseCaseConfiguration {

    @Bean
    CreateGoal createGoal(GoalStore goals, GoalPositionReader positions, GoalBusinessDate dates,
                          UseCaseTransactions transactions) {
        return transactions.writable(new CreateGoalUseCase(goals, positions, dates));
    }

    @Bean
    UpdateGoal updateGoal(GoalStore goals, GoalPositionReader positions, GoalBusinessDate dates,
                          UseCaseTransactions transactions) {
        return transactions.writable(new UpdateGoalUseCase(goals, positions, dates));
    }

    @Bean
    DeleteGoal deleteGoal(GoalStore goals, UseCaseTransactions transactions) {
        return transactions.writable(new DeleteGoalUseCase(goals));
    }

    @Bean
    GetGoals getGoals(GoalStore goals, GoalPositionReader positions, GoalBusinessDate dates,
                      UseCaseTransactions transactions) {
        return transactions.readOnly(new GetGoalsUseCase(goals, positions, dates));
    }

    @Bean
    GetGoalCapacity getGoalCapacity(GoalStore goals, GoalPositionReader positions,
                                    UseCaseTransactions transactions) {
        return transactions.readOnly(new GetGoalCapacityUseCase(goals, positions));
    }
}
