package com.logflow.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证结果
 */
public class ValidationResult {
    
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    
    private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
    }
    
    /**
     * 创建成功的验证结果
     */
    public static ValidationResult success() {
        return new ValidationResult(true, new ArrayList<>(), new ArrayList<>());
    }
    
    /**
     * 创建失败的验证结果
     */
    public static ValidationResult failure(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new ValidationResult(false, errors, new ArrayList<>());
    }
    
    /**
     * 创建Builder
     */
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
        
        public ValidationResult build() {
            boolean valid = errors.isEmpty();
            return new ValidationResult(valid, errors, warnings);
        }
    }
}
