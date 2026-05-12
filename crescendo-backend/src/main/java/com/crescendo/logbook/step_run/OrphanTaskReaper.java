package com.crescendo.logbook.step_run;

import com.crescendo.enums.StepRunStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class OrphanTaskReaper {

    private static final Logger logger = LoggerFactory.getLogger(OrphanTaskReaper.class);
    
    private final StepRunRepository stepRunRepository;

    public OrphanTaskReaper(StepRunRepository stepRunRepository) {
        this.stepRunRepository = stepRunRepository;
    }

    @Scheduled(fixedRate = 60_000) // Every minute
    @Transactional
    public void reapOrphanTasks() {
        Instant threshold = Instant.now().minus(15, ChronoUnit.MINUTES);
        List<StepRun> orphans = stepRunRepository.findOrphanTasks(threshold);
        
        if (!orphans.isEmpty()) {
            logger.info("Found {} orphan RUNNING tasks older than 15 minutes. Resetting to PENDING.", orphans.size());
            for (StepRun run : orphans) {
                run.setStatus(StepRunStatus.PENDING);
                run.setUpdatedAt(Instant.now());
                stepRunRepository.save(run);
            }
        }
    }
}
