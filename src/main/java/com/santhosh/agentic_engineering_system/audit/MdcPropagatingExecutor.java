package com.santhosh.agentic_engineering_system.audit;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class MdcPropagatingExecutor implements Executor, AutoCloseable {
    private final ExecutorService delegate;

    public MdcPropagatingExecutor(ExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable command) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        delegate.execute(() -> {
            if (context != null) MDC.setContextMap(context);
            try {
                command.run();
            } finally {
                MDC.clear();
            }
        });
    }

    @Override public void close() { delegate.close(); }
}
