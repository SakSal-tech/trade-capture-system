package com.technicalchallenge.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.technicalchallenge.model.Trade;

import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import jakarta.persistence.criteria.Path;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Visits the RSQL Abstract Syntax Tree (AST) and converts it into a single
 * Spring Data JPA Specification<Trade> that can be executed by the repository.
 * Implement three visit methods:
 */
// Specification<Trade> what each visit returns
public class TradeRsqlVisitor implements RSQLVisitor<Specification<Trade>, Void> {

    // visit(AndNode...), combines child specs with AND
    @Override
    public Specification<Trade> visit(AndNode node, Void param) {
        // empty list to hold child spec
        List<Specification<Trade>> childSpecs = new ArrayList<>();
        // get all children each could be ComparisonNode or another And/Or node
        List<Node> children = node.getChildren();

        for (Node child : children) {
            // this output is for debugging/testing reasons before building the spec
            // System.out.println("Visiting child of AND node: " + child.toString());
            // recursively visit each child node
            Specification<Trade> childSpec = child.accept(this);// tells child node, to call the right visit method on
                                                                // itself using this same visitor class.”
            if (childSpec != null) {
                childSpecs.add(childSpec);
            }
        }

        Specification<Trade> result = null;

        // combine all child specifications with AND
        for (Specification<Trade> spec : childSpecs) {
            if (result == null) {
                result = spec; // first one becomes base(result=spec1)
            } else {
                result = result.and(spec);// chain others with AND(Add the next spec using AND)
            }
        }

        return result;// Final combined Specification
    }

    // visit(OrNode...), combines child specs with OR
    @Override
    public Specification<Trade> visit(OrNode node, Void param) {
        List<Specification<Trade>> childSpecs = new ArrayList<>();
        List<Node> children = node.getChildren();
        for (Node child : children) {
            Specification<Trade> childSpec = child.accept(this);
            if (childSpec != null) {
                childSpecs.add(childSpec);
            }
        }
        Specification<Trade> result = null;

        for (Specification<Trade> spec : childSpecs) {
            if (result == null) {
                result = spec;
            } else {
                result = result.or(spec);
            }
        }

        return result;
    }

    @Override
    public Specification<Trade> visit(ComparisonNode node, Void param) {

        // pull parts Abstract Syntax Tree
        // field is the left-hand side of a condition, the field path e.g.
        // counterparty.name==ABC
        String field = node.getSelector();
        // operator symbol e.g == or =ge=
        String operator = node.getOperator().getSymbol();
        // FIX: to fix the failing test. List all supported operators
        List<String> supportedOperators = Arrays.asList("==", "!=", "=gt=", "=lt=", "=ge=", "=le=", "=in=", "=like=",
                "=out=");
        // Throw an exception if the operator is not recognised
        if (!supportedOperators.contains(operator)) {
            throw new IllegalArgumentException("Unsupported operator: " + operator);
        }

        // values is the right-hand of the expression(list of literal text values) e.g
        // status=in=(LIVE,NEW). values = ["LIVE", "NEW"]
        List<String> values = node.getArguments();// e.g ["ABC"]

        // for debugging to see what parsed
        System.out
                .println("[RSQL] ComparisonNode -> field=" + field + ", operation=" + operator + ", values=" + values);

        // Strict reflection-based field existence check for nested fields (run
        // immediately)
        String[] fieldParts = field.split("\\.");
        Class<?> currentClass = Trade.class;
        for (int i = 0; i < fieldParts.length; i++) {
            String part = fieldParts[i];
            java.lang.reflect.Field f = null;
            Class<?> searchClass = currentClass;// find the field part in the class examining e.g. Trade.
            while (searchClass != null) {
                try {
                    f = searchClass.getDeclaredField(part);
                    break;
                } catch (NoSuchFieldException ex) {
                    searchClass = searchClass.getSuperclass();// If not found the field in the class, check its
                                                              // superclass.
                }
            }
            if (f == null) {
                throw new IllegalArgumentException("Invalid field in query: " + field);
            }
            // For nested fields, ensure the type is not a JPA proxy or collection
            if (i < fieldParts.length - 1) {
                Class<?> type = f.getType();// Retrieve field type. e.g. Counterparty.class
                if (type.isPrimitive() || type == String.class || Number.class.isAssignableFrom(type)
                        || type.isEnum()) {
                    // Reject traversing into primitives or simple fields
                    throw new IllegalArgumentException(
                            "Cannot traverse into non-entity field: " + part + " in query: " + field);
                }
                currentClass = type;
            } else {
                currentClass = f.getType();
            }
        }

        // build predicate(condition) by returning anonymous inner class
        return new Specification<Trade>() {
            @Override
            public Predicate toPredicate(
                    @org.springframework.lang.NonNull Root<Trade> root,
                    @org.springframework.lang.NonNull CriteriaQuery<?> query,
                    @org.springframework.lang.NonNull CriteriaBuilder criteriaBuilder) {

                // Now traverse the path as before
                Path<?> path = root;// start at the root
                for (String part : fieldParts) {
                    path = path.get(part);// e.g. path = path.get("tradeDate")tradeDate==2023-01-01
                }
                Class<?> fieldType;
                try {
                    fieldType = path.getJavaType();
                } catch (NullPointerException e) {
                    throw new IllegalArgumentException("Invalid field in query: " + field, e);
                }
                Object typedValue;
                try {
                    typedValue = convertValue(fieldType, values.get(0));
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Invalid value '" + values.get(0) + "' for type " + fieldType.getSimpleName(), e);
                }

                // Fixed: Use typedValue for type safety and cast to String for string
                // operations
                if (operator.equals("==")) {
                    if (fieldType == String.class) {
                        return criteriaBuilder.equal(
                                criteriaBuilder.lower(path.as(String.class)),
                                ((String) typedValue).toLowerCase());
                    } else {
                        return criteriaBuilder.equal(path, typedValue);
                    }
                }

                if (operator.equals("!=")) {
                    if (fieldType == String.class) {
                        return criteriaBuilder.notEqual(
                                criteriaBuilder.lower(path.as(String.class)),
                                ((String) typedValue).toLowerCase());
                    } else {
                        return criteriaBuilder.notEqual(path, typedValue);
                    }
                }

                if (operator.equals("=gt=")) {
                    if (fieldType == Integer.class || fieldType == int.class) {
                        return criteriaBuilder.greaterThan(path.as(Integer.class), (Integer) typedValue);
                    } else if (fieldType == Long.class || fieldType == long.class) {
                        return criteriaBuilder.greaterThan(path.as(Long.class), (Long) typedValue);
                    } else if (fieldType == Double.class || fieldType == double.class) {
                        return criteriaBuilder.greaterThan(path.as(Double.class), (Double) typedValue);
                    } else if (fieldType == LocalDate.class) {
                        // I've added this to support LocalDate comparisons correctly
                        return criteriaBuilder.greaterThan(path.as(LocalDate.class), (LocalDate) typedValue);
                    } else {
                        return criteriaBuilder.greaterThan(path.as(String.class), ((String) typedValue));
                    }
                }

                if (operator.equals("=lt=")) {
                    if (fieldType == Integer.class || fieldType == int.class) {
                        return criteriaBuilder.lessThan(path.as(Integer.class), (Integer) typedValue);
                    } else if (fieldType == Long.class || fieldType == long.class) {
                        return criteriaBuilder.lessThan(path.as(Long.class), (Long) typedValue);
                    } else if (fieldType == Double.class || fieldType == double.class) {
                        return criteriaBuilder.lessThan(path.as(Double.class), (Double) typedValue);
                    } else if (fieldType == LocalDate.class) {
                        // I've added this for proper LocalDate less-than comparisons
                        return criteriaBuilder.lessThan(path.as(LocalDate.class), (LocalDate) typedValue);
                    } else {
                        return criteriaBuilder.lessThan(path.as(String.class), ((String) typedValue));
                    }
                }

                if (operator.equals("=ge=")) {
                    if (fieldType == Integer.class || fieldType == int.class) {
                        return criteriaBuilder.greaterThanOrEqualTo(path.as(Integer.class), (Integer) typedValue);
                    } else if (fieldType == Long.class || fieldType == long.class) {
                        return criteriaBuilder.greaterThanOrEqualTo(path.as(Long.class), (Long) typedValue);
                    } else if (fieldType == Double.class || fieldType == double.class) {
                        return criteriaBuilder.greaterThanOrEqualTo(path.as(Double.class), (Double) typedValue);
                    } else if (fieldType == LocalDate.class) {
                        // I've added this to allow LocalDate >= comparisons
                        return criteriaBuilder.greaterThanOrEqualTo(path.as(LocalDate.class), (LocalDate) typedValue);
                    } else {
                        return criteriaBuilder.greaterThanOrEqualTo(path.as(String.class), ((String) typedValue));
                    }
                }

                if (operator.equals("=le=")) {
                    if (fieldType == Integer.class || fieldType == int.class) {
                        return criteriaBuilder.lessThanOrEqualTo(path.as(Integer.class), (Integer) typedValue);
                    } else if (fieldType == Long.class || fieldType == long.class) {
                        return criteriaBuilder.lessThanOrEqualTo(path.as(Long.class), (Long) typedValue);
                    } else if (fieldType == Double.class || fieldType == double.class) {
                        return criteriaBuilder.lessThanOrEqualTo(path.as(Double.class), (Double) typedValue);
                    } else if (fieldType == LocalDate.class) {
                        // I've added this for proper LocalDate <= comparisons
                        return criteriaBuilder.lessThanOrEqualTo(path.as(LocalDate.class), (LocalDate) typedValue);
                    } else {
                        return criteriaBuilder.lessThanOrEqualTo(path.as(String.class), ((String) typedValue));
                    }
                }

                if (operator.equals("=in=")) {
                    List<Object> typedValues = new ArrayList<>();
                    for (String v : values) {
                        typedValues.add(convertValue(fieldType, v));
                    }
                    return path.in(typedValues);
                }

                if (operator.equals("=like=")) {
                    if (fieldType == String.class) {
                        String pattern = ((String) typedValue).replace("*", "%").toLowerCase();
                        return criteriaBuilder.like(
                                criteriaBuilder.lower(path.as(String.class)),
                                pattern);
                    }
                }

                if (operator.equals("=out=")) {
                    List<Object> typedValues = new ArrayList<>();
                    for (String v : values) {
                        typedValues.add(convertValue(fieldType, v));
                    }
                    return criteriaBuilder.not(path.in(typedValues));
                }

                throw new IllegalStateException("No predicate could be built for operator: " + operator);
            }

            /*
             * Future Improvements: Add logic that checks the actual field type (e.g., if
             * it's a date, parse it into LocalDate and call greaterThan() on a
             * Path<LocalDate> instead).
             */
        };
    }

    /*
     * Fix for failing test Type IllegalArgument exceptions
     * Uses reflection and JPA Criteria API, to determine the actual Java type of
     * the field in the entity/model (e.g., Long for tradeId).
     * 
     * Uses a utility method to convert the string to the expected type (e.g.,
     * parses "123" to Long).
     * Catchs conversion errors and throw an exception.
     */
    public static Object convertValue(Class<?> type, String value) {
        // I refactored this method to handle LocalDate and LocalDateTime conversions
        // properly, while preserving existing logic and comments.
        try {
            if (type == String.class)
                return value;
            if (type == Integer.class || type == int.class)
                return Integer.parseInt(value);
            if (type == Long.class || type == long.class)
                return Long.parseLong(value);
            if (type == Double.class || type == double.class)
                return Double.parseDouble(value);
            if (type == Boolean.class || type == boolean.class)
                return Boolean.parseBoolean(value);

            // I've added this section for LocalDate so that date-only fields parse
            // correctly.
            if (type == LocalDate.class) {
                String clean = value.replace("'", "").replace("\"", "");
                return LocalDate.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE);
            }

            // I've added this section for LocalDateTime in case timestamp fields are
            // queried.
            if (type == LocalDateTime.class) {
                String clean = value.replace("'", "").replace("\"", "");
                return LocalDateTime.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            // Add more types as needed
            throw new IllegalArgumentException("Unsupported type: " + type.getSimpleName());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid value '" + value + "' for type " + type.getSimpleName());
        }
    }

}
