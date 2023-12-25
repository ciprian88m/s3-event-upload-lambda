package dev.ciprian;

import java.util.Random;

public enum EventType {
    USER_SIGN_UP,
    USER_LOGIN,
    USER_STARTED_PROCESS,
    USER_SELECTED_PRODUCT,
    USER_UPDATED_PRODUCT,
    USER_PASSED_CHECK,
    USER_FINISHED_PROCESS;

    private static final EventType[] EVENT_TYPES = values();
    private static final int SIZE = EVENT_TYPES.length;
    private static final Random RANDOM = new Random();

    public static EventType randomEventType() {
        return EVENT_TYPES[RANDOM.nextInt(SIZE)];
    }

}
