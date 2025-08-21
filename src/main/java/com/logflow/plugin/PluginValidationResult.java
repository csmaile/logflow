package com.logflow.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件配置验证结果
 */
public class PluginValidationResult {

    private boolean valid;
    private List<ValidationError> errors;
    private List<ValidationWarning> warnings;

    public PluginValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.valid = true;
    }

    public static PluginValidationResult success() {
        return new PluginValidationResult();
    }

    public static PluginValidationResult failure(String errorMessage) {
        PluginValidationResult result = new PluginValidationResult();
        result.addError("general", errorMessage);
        return result;
    }

    public static PluginValidationResult failure(String parameter, String errorMessage) {
        PluginValidationResult result = new PluginValidationResult();
        result.addError(parameter, errorMessage);
        return result;
    }

    public void addError(String parameter, String message) {
        this.errors.add(new ValidationError(parameter, message));
        this.valid = false;
    }

    public void addWarning(String parameter, String message) {
        this.warnings.add(new ValidationWarning(parameter, message));
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    public List<ValidationWarning> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<ValidationWarning> warnings) {
        this.warnings = warnings;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public String getErrorSummary() {
        if (errors.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ValidationError error : errors) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(error.getParameter()).append(": ").append(error.getMessage());
        }
        return sb.toString();
    }

    public static class ValidationError {
        private String parameter;
        private String message;

        public ValidationError(String parameter, String message) {
            this.parameter = parameter;
            this.message = message;
        }

        public String getParameter() {
            return parameter;
        }

        public void setParameter(String parameter) {
            this.parameter = parameter;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class ValidationWarning {
        private String parameter;
        private String message;

        public ValidationWarning(String parameter, String message) {
            this.parameter = parameter;
            this.message = message;
        }

        public String getParameter() {
            return parameter;
        }

        public void setParameter(String parameter) {
            this.parameter = parameter;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
