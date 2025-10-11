package com.technicalchallenge.service;

import java.util.ArrayList;
import java.util.List;

public class TradeValidation {
    private boolean valid = true;
    private final List<String> errorStrings = new ArrayList<>();

    public boolean isValid() {
        return valid;
    }

    public void setError(String messageString) {
        valid = false;
        errorStrings.add(messageString);
    }

    public List<String> getErrors() {
        return errorStrings;
    }

}
