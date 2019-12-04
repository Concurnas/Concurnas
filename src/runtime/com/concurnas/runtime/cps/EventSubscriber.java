package com.concurnas.runtime.cps;

public interface EventSubscriber {
    void onEvent(EventPublisher ep, Event e);
}
