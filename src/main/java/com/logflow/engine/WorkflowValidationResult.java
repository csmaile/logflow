package com.logflow.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流验证结果
 */
public class WorkflowValidationResult {
    
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    
    private WorkflowValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * Builder模式
     */
    public static class Builder {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        
        public Builder error(String error) {
            this.errors.add(error);
            return this;
        }
        
        public Builder warning(String warning) {
            this.warnings.add(warning);
            return this;
        }
        
        public WorkflowValidationResult build() {
            boolean valid = errors.isEmpty();
            return new WorkflowValidationResult(valid, errors, warnings);
        }
    }
}
