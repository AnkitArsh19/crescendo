package com.crescendo.steps.step_condition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface StepConditionRepository extends JpaRepository<StepCondition, UUID> {
    
    List<StepCondition> findByStepId(UUID stepId);

    @Transactional(transactionManager = "commandTransactionManager")
    void deleteByStepId(UUID stepId);
}
