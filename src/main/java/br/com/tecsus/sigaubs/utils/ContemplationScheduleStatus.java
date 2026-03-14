package br.com.tecsus.sigaubs.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ContemplationScheduleStatus {

    private final AtomicReference<Status> status = new AtomicReference<>();
    private volatile LocalDateTime startTime;
    private volatile LocalDateTime endTime;

    public void setRunning() {
        this.startTime = LocalDateTime.now();
        this.endTime = null;
        this.status.set(Status.RUNNING);
    }

    public void setDone() {
        this.endTime = LocalDateTime.now();
        this.status.set(Status.DONE);
    }

    public void setFailed() {
        this.endTime = LocalDateTime.now();
        this.status.set(Status.FAILED);
    }

    public Status getStatus() {
        return status.get();
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public enum Status {
        RUNNING,
        DONE,
        FAILED
    }
}
