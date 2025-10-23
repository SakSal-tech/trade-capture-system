package com.technicalchallenge.validation;

import java.util.ArrayList;
import java.util.List;

//Central place for all validation feedback. Will be shared by all validators (date, privilege, etc.). Holds validation results, will the container that collects all validation errors. Stores multiple validation messages and a success flag to easily test multiple failure conditions in one go. 
public class TradeValidationResult {
    private boolean valid = true;
    private final List<String> errorStrings = new ArrayList<>();

    public boolean isValid() {
        return valid;
    }

    // Record a new error in the validation result. Whenever a validation rule
    // fails, the validator calls this method
    public void setError(String messageString) {
        valid = false;// Marks the overall result as invalid (because at least one problem is found)
        errorStrings.add(messageString);
    }

    // returns the full list of error messages
    public List<String> getErrors() {
        return errorStrings;
    }
    // Adding this line to force commit

}
