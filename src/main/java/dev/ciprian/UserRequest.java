package dev.ciprian;

public record UserRequest(int numberOfObjects) {

    public UserRequest {
        if (numberOfObjects <= 0) {
            numberOfObjects = 1;
        }
    }

}
