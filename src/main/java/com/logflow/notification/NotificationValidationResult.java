package com.logflow.notification;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知配置验证结果类
 */
public class NotificationValidationResult {

    private final boolean valid;
    private final List<ValidationError> errors;
    private final List<ValidationWarning> warnings;

    public NotificationValidationResult(boolean valid, List<ValidationError> errors, List<ValidationWarning> warnings) {
        this.valid = valid;
        this.errors = new ArrayList<>(errors != null ? errors : new ArrayList<>());
        this.warnings = new ArrayList<>(warnings != null ? warnings : new ArrayList<>());
    }

    // 静态工厂方法

    public static NotificationValidationResult valid() {
        return new NotificationValidationResult(true, null, null);
    }

    public static NotificationValidationResult invalid(List<ValidationError> errors) {
        return new NotificationValidationResult(false, errors, null);
    }

    public static NotificationValidationResult invalid(String error) {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError("config", error));
        return new NotificationValidationResult(false, errors, null);
    }

    public static NotificationValidationResult validWithWarnings(List<ValidationWarning> warnings) {
        return new NotificationValidationResult(true, null, warnings);
    }

    // Getters

    public boolean isValid() {
        return valid;
    }

    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<ValidationWarning> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    // Builder类

    public static class Builder {
        private final List<ValidationError> errors = new ArrayList<>();
        private final List<ValidationWarning> warnings = new ArrayList<>();

        public Builder error(String parameter, String message) {
            errors.add(new ValidationError(parameter, message));
            return this;
        }

        public Builder warning(String parameter, String message) {
            warnings.add(new ValidationWarning(parameter, message));
            return this;
        }

        public NotificationValidationResult build() {
            boolean valid = errors.isEmpty();
            return new NotificationValidationResult(valid, errors, warnings);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // 内部类 - 验证错误

    public static class ValidationError {
        private final String parameter;
        private final String message;

        public ValidationError(String parameter, String message) {
            this.parameter = parameter;
            this.message = message;
        }

        public String getParameter() {
            return parameter;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("ValidationError{parameter='%s', message='%s'}", parameter, message);
        }
    }

    // 内部类 - 验证警告

    public static class ValidationWarning {
        private final String parameter;
        private final String message;

        public ValidationWarning(String parameter, String message) {
            this.parameter = parameter;
            this.message = message;
        }

        public String getParameter() {
            return parameter;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("ValidationWarning{parameter='%s', message='%s'}", parameter, message);
        }
    }

    @Override
    public String toString() {
        return String.format("NotificationValidationResult{valid=%s, errors=%d, warnings=%d}",
                valid, errors.size(), warnings.size());
    }
}
