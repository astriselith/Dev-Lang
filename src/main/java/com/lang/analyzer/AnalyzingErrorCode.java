package com.lang.analyzer;

public enum AnalyzingErrorCode {
    REDEFINED_FUNCTION("Function '%s' is already defined"),
    REDEFINED_VARIABLE("Variable '%s' is already defined"),
    REDEFINED_OPERATOR("Operator '%s' is already defined for these operand types"),
    UNDEFINED_VARIABLE("Variable '%s' is not defined"),
    UNRESOLVED_TYPE("Type '%s' could not be resolved"),
    UNRESOLVED_MEMBER("Member '%s' could not be resolved for type '%s'"),
    UNIREFERED_LOCAL("Cannot infer type for local variable '%s': without an initializer"),
    INVALID_FUNCTION_CALL("Type '%s' is not callable"),
    INVALID_FUNCTION_ARGUMENT_TYPE("Function '%s' argument %d expects type '%s', but got '%s'"),
    INVALID_FUNCTION_ARGUMENT_COUNT("Function '%s' expects %d argument(s) but got %d"),
    INVALID_FUNCTION_TYPE_ARGUMENT_COUNT("Function '%s' expects %d type argument(s) but got %d"),
    UNINFERRED_TYPE_ARGUMENT("Cannot infer type argument for '%s' in call to '%s'"),
    INVALID_TYPE_ARGUMENT_BOUND("Type argument '%s' does not satisfy bound '%s' for type parameter '%s'"),
    INVALID_FUNCTION_RETURN_TYPE("Function '%s' has return type '%s', but returns value of type '%s'"),
    INVALID_FUNCTION_MISSING_RETURN("Function '%s' expects return type '%s' but no value returned"),
    INVALID_FUNCTION_RETURN_OUTSIDE("Return outside function"),
    FUNCTION_WITHOUT_BODY("Function '%s' has no body; its implementation must be provided externally"),
    OPERATOR_WITHOUT_BODY("Operator '%s' has no body; its implementation must be provided externally"),
    BREAK_OUTSIDE_LOOP("Break statement outside loop"),
    CONTINUE_OUTSIDE_LOOP("Continue statement outside loop"),
    EXPECTED_TYPE("Expected type '%s' but got '%s'"),
    INVALID_ASSIGNMENT("Cannot assign value of type '%s' to variable of type '%s'"),
    INVALID_TYPE("Type '%s' is not valid"),
    INVALID_OPERATOR("Operator '%s' is not valid for type '%s'"),
    INVALID_OPERATOR_ARGUMENT_TYPE("Operator '%s' expects an operand of type '%s', but got '%s'"),
    INVALID_CLASS_CONSTRUCTOR_ARGUMENT_TYPE("Class '%s' constructor argument %d expects type '%s', but got '%s'"),
    INVALID_CLASS_CONSTRUCTOR_ARGUMENT_COUNT("Class '%s' expects %d argument(s) but got %d"),
    INVALID_CLASS_CONSTRUCTOR_TYPE_ARGUMENT_COUNT("Class '%s' expects %d type argument(s) but got %d"),
    STATIC_ACCESS_ON_INSTANCE("Static member '%s' must be accessed through the class"),
    INSTANCE_ACCESS_ON_STATIC("Instance member '%s' must be accessed through an instance"),
    INSTANCE_ACCESS_ON_CLASS("Instance member '%s' cannot be accessed through the class"),
    DUPLICATE_MODIFIER("Duplicate modifier: %s"),
    CONFLICTING_MODIFIERS("Conflicting modifiers: %s"),
    INVALID_MODIFIER_IN_CONTEXT("Modifier '%s' is not valid in this context"),
    INACCESSIBLE_MEMBER("Member '%s' is not accessible from here"),
    INVALID_TYPE_PARAMETER_BOUND("Bound '%s' of type parameter '%s' must be a class or type parameter"),
    CIRCULAR_TYPE_PARAMETER_BOUND("Type parameter '%s' has a circular bound dependency on '%s'"),
    INVALID_ANNOTATION_BASE("Annotation '%s' must extend 'Annotation', but extends '%s'"),
    SELF_REFERENTIAL_TYPE_PARAMETER_BOUND("Type parameter '%s' cannot be bounded by itself"),
    UNRESOLVED_TYPE_PARAMETER_BOUND("Bound '%s' of type parameter '%s' could not be resolved"),
    INVALID_OPERATOR_RECEIVER_TYPE(
            "Operator '%s' must have its first operand of type '%s' (the declaring class), but got '%s'"),
    FINAL_ASSIGNMENT("Cannot assign to final variable '%s'"),
    FINAL_WITHOUT_INIT("Final variable '%s' requires an initializer"),
    FINAL_SUPER("Class '%s' is final and cannot be extended"),
    INVALID_ANNOTATION_TYPE("Annotation '%s' must be a class type"),
    INVALID_ANNOTATION_ARGUMENT_TYPE("Annotation '%s' argument %d expects type '%s', but got '%s'"),
    INVALID_ANNOTATION_ARGUMENT_COUNT("Annotation '%s' expects %d argument(s) but got %d"),
    ANNOTATION_REQUIRES_ARGUMENT("Placeholder '_' requires annotation '%s' parameter '%s' to have a default value"),
    INVALID_CLASS_UNCHECKED_CONSTRUCTOR(
            "Unchecked instantiation of generic class '%s': type arguments omitted, defaulting to bounds %s");

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
