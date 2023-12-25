package dev.ciprian;

import java.time.LocalDateTime;

public record Event(String userId,
                    String firstName,
                    String middleName,
                    String lastName,
                    EventType eventType,
                    LocalDateTime createdAt,
                    String message) {
}
