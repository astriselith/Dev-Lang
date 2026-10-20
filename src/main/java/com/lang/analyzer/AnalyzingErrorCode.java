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

    INVALID_CLASS_SUPERCLASS("Class '%s' cannot extend '%s'"),
    INVALID_CLASS_MEMBER_ACCESS("Type '%s' does not have member '%s'"),
    INVALID_CLASS_TYPE_ARGUMENT("Type argument '%s' is not valid for '%s'"),
    INVALID_CLASS_CONSTRUCTOR_ARGUMENT_TYPE("Class '%s' expects argument of type '%s', but got '%s'"),
    INVALID_CLASS_CONSTRUCTOR_ARGUMENT_COUNT("Class '%s' expects %d argument(s) but got %d"),
    INVALID_CLASS_CONSTRUCTOR_TYPE_ARGUMENT_COUNT("Class '%s' expects %d type argument(s) but got %d"),
    INVALID_CLASS_UNCHECKED_CONSTRUCTOR("Unchecked instantiation of generic class '%s': type arguments omitted, defaulting to bounds %s"),

    INVALID_FUNCTION_CALL("Type '%s' is not callable"),
    INVALID_FUNCTION_ARGUMENT_TYPE("Function '%s' expects argument of type '%s', but got '%s'"),
    INVALID_FUNCTION_ARGUMENT_COUNT("Function '%s' expects %d argument(s) but got %d"),
    INVALID_FUNCTION_RETURN_TYPE("Function '%s' has return type '%s', but returns value of type '%s'"),
    INVALID_FUNCTION_MISSING_RETURN("Function '%s' expects return type '%s' but no value returned"),
    INVALID_FUNCTION_RETURN_OUTSIDE("Return outside function"),

    INVALID_ASSIGNMENT("Cannot assign value of type '%s' to variable of type '%s'"),
    INVALID_TYPE("Type '%s' is not valid"),
    INVALID_INDEX_ACCESS("Type '%s' does not support indexing"),
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