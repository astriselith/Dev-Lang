package com.lang.analyzer;

public enum AnalyzingErrorCode {

    REDEFINED_CLASS("Class '%s' is already defined"),
    REDEFINED_FUNCTION("Function '%s' is already defined"),
    REDEFINED_VARIABLE("Variable '%s' is already defined"),
    REDEFINED_PARAMETER("Parameter '%s' is already defined"),
    REDEFINED_TYPE_PARAMETER("Type parameter '%s' is already defined"),

    UNDEFINED_CLASS("Class '%s' is not defined"),
    UNDEFINED_FUNCTION("Function '%s' is not defined"),
    UNDEFINED_VARIABLE("Variable '%s' is not defined"),
    UNDEFINED_PARAMETER("Parameter '%s' is not defined"),

    UNRESOLVED_TYPE("Type '%s' could not be resolved"),
    UNRESOLVED_SUPERCLASS("Superclass '%s' could not be resolved for class '%s'"),
    UNRESOLVED_MEMBER("Member '%s' could not be resolved for type '%s'"),
    UNRESOLVED_FUNCTION("Function '%s' could not be resolved for type '%s'"),
    UNRESOLVED_VARIABLE("Variable '%s' could not be resolved"),
    UNRESOLVED_TYPE_ARGUMENT("Type argument '%s' could not be resolved for type '%s'"),
    UNRESOLVED_TYPE_PARAMETER("Type parameter '%s' could not be resolved for type '%s'"),
    UNRESOLVED_GENERIC_ARGUMENT("Generic argument '%s' could not be resolved for type '%s'"),
    UNRESOLVED_GENERIC_PARAMETER("Generic parameter '%s' could not be resolved for type '%s'"),

    DUPLICATE_SUPERCLASS("Superclass '%s' is specified more than once"),

    INVALID_SUPERCLASS("Class '%s' cannot extend '%s'"),
    INVALID_TYPE("Type '%s' is not valid"),
    INVALID_TYPE_ARGUMENT("Type argument '%s' is not valid for '%s'"),
    INVALID_ASSIGNMENT("Cannot assign value of type '%s' to variable of type '%s'"),
    INVALID_RETURN_TYPE("Function '%s' has return type '%s', but returns value of type '%s'"),
    INVALID_ARGUMENT_TYPE("Function '%s' expects argument of type '%s', but got '%s'"),
    INVALID_MEMBER_ACCESS("Type '%s' does not have member '%s'"),
    INVALID_INDEX_ACCESS("Type '%s' does not support indexing"),
    INVALID_CALL("Type '%s' is not callable"),
    INVALID_OPERATOR("Operator '%s' is not valid for type '%s'"),

    SELF_REFERENTIAL_TYPE_PARAMETER("Type parameter '%s' cannot reference itself"),
    CIRCULAR_TYPE_PARAMETER("Circular type parameter detected for '%s'"),
    CIRCULAR_INHERITANCE("Circular inheritance detected for class '%s'");

    public static final String TAG = "ANALYZER";

    private final String message;

    AnalyzingErrorCode(String message) {
        this.message = message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }

    public String getMessage() {
        return message;
    }
}