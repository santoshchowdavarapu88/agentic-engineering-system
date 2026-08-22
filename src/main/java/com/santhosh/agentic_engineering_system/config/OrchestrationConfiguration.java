package com.santhosh.agentic_engineering_system.config;

import com.santhosh.agentic_engineering_system.orchestration.adapter.InMemoryWorkflowRepository;
import com.santhosh.agentic_engineering_system.audit.MdcPropagatingExecutor;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowEngine;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowGateEvaluator;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowGraphValidator;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowTaskHandlerRegistry;
import com.santhosh.agentic_engineering_system.orchestration.port.DecisionLedger;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowRepository;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowTaskHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class OrchestrationConfiguration {
    @Bean Clock workflowClock() { return Clock.systemUTC(); }
    @Bean WorkflowRepository workflowRepository() { return new InMemoryWorkflowRepository(); }
    @Bean Executor workflowExecutor() {
        return new MdcPropagatingExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
    @Bean WorkflowEngine workflowEngine(List<WorkflowTaskHandler> handlers,
                                        DecisionLedger ledger,
                                        @Qualifier("workflowExecutor") Executor workflowExecutor) {
        return new WorkflowEngine(new WorkflowGraphValidator(), new WorkflowGateEvaluator(),
                new WorkflowTaskHandlerRegistry(handlers), ledger, workflowExecutor);
    }
}
