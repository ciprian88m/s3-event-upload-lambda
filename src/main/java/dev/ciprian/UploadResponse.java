package dev.ciprian;

import java.util.List;

public record UploadResponse(List<String> objectKeys, String status, boolean partialKeys) {
}
