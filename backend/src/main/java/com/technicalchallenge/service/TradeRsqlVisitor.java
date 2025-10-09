package com.technicalchallenge.service;

import java.util.ArrayList;
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

/**
 * Visits the RSQL Abstract Syntax Tree (AST) and converts it into a single
 * Spring Data JPA Specification<Trade> that can be executed by the repository.
 * Implement three visit methods:
 */
// Specification<Trade> what each visit returns
public class TradeRsqlVisitor implements RSQLVisitor<Specification<Trade>, Void> {

    // visit(AndNode...), combines child specs with AND
    @Override
    public Specification<Trade> visit(AndNode node, Void param) { // empty list to hold child spec
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

        // combine all child speficications with AND
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
        // values is the right-hand of the expression(list of literal text values) e.g
        // status=in=(LIVE,NEW). values = ["LIVE", "NEW"]
        List<String> values = node.getArguments();// e.g ["ABC"]

        // for debugging to see what parsed
        System.out
                .println("[RSQL] ComparisonNode -> field=" + field + ", operation=" + operator + ", values=" + values);
        // build predicate(condition) by returning anonymous inner class( an object that
        // has a method inside and
        // implements the Specification<Trade> interface)
        return new Specification<Trade>() {
            @Override
            public Predicate toPredicate(
                    @org.springframework.lang.NonNull Root<Trade> root,
                    @org.springframework.lang.NonNull CriteriaQuery<?> query,
                    @org.springframework.lang.NonNull CriteriaBuilder criteriaBuilder) {

                // building predicate.
                String[] fieldParts = field.split("\\.");// separatethe query string by "." e.g. counterparty.name==ABC,
                                                         // field = "counterparty.name"; then fieldParts =
                                                         // ["counterparty", "name"];. The entity Trade has a nested
                                                         // object counterparty, which is the property name

                Path<?> path = root;//// path is like a pointer object that will move deeper into nested fields e.g
                                    //// in
                                    // Counterparty table, path is counterparty.name.

                for (String part : fieldParts) {
                    path = path.get(part);
                }
                if (operator.equals("==")) {
                    if (path.getJavaType() == String.class) {
                        return criteriaBuilder.equal(
                                criteriaBuilder.lower(path.as(String.class)),
                                values.get(0).toLowerCase());
                    } else {
                        return criteriaBuilder.equal(path, values.get(0));
                    }
                }
                // check for "not equal" (!=) operator
                if (operator.equals("!=")) {
                    if (path.getJavaType() == String.class) {
                        return criteriaBuilder.notEqual(
                                criteriaBuilder.lower(path.as(String.class)),
                                values.get(0).toLowerCase());
                    } else {
                        return criteriaBuilder.notEqual(path, values.get(0));
                    }
                }

                // check for "greater than" (>) in RSQL syntax, this is written as =gt=
                if (operator.equals("=gt=")) {
                    // convert path to a String expression (path.as(String.class))
                    // because CriteriaBuilder's greaterThan() expects a Comparable type like
                    // String, Number, Date, etc.
                    return criteriaBuilder.greaterThan(path.as(String.class), values.get(0));
                }

                // check for "less than" (<) in RSQL syntax, this is written as =lt=
                if (operator.equals("=lt=")) {
                    return criteriaBuilder.lessThan(path.as(String.class), values.get(0));
                }

                // check for "greater than or equal to" (>=)
                // in RSQL syntax, this is written as =ge=
                if (operator.equals("=ge=")) {
                    return criteriaBuilder.greaterThanOrEqualTo(path.as(String.class), values.get(0));
                }

                // check for "less than or equal to" (<=)
                // in RSQL syntax, this is written as =le=
                if (operator.equals("=le=")) {
                    return criteriaBuilder.lessThanOrEqualTo(path.as(String.class), values.get(0));
                }

                // check for "in" operator (=in=)
                // for example: tradeStatus.tradeStatus=in=(LIVE,NEW)
                if (operator.equals("=in=")) {
                    // path.in(values) builds a predicate like:
                    // WHERE tradeStatus IN ('LIVE', 'NEW')
                    return path.in(values);
                }

                // check for "like" operator (=like=)
                // Purpose: wildcard string matching, e.g. *bank* matches any string containing
                // 'bank'
                // Usage: /api/trades/rsql?query=counterparty.name=like=*bank*
                if (operator.equals("=like=")) {
                    if (path.getJavaType() == String.class) {
                        // Convert RSQL '*' wildcards to SQL '%' wildcards and make case-insensitive
                        String pattern = values.get(0).replace("*", "%").toLowerCase();
                        return criteriaBuilder.like(
                                criteriaBuilder.lower(path.as(String.class)),
                                pattern);
                    }
                }
                // Handle case-insensitive LIKE operator (=like=)
                if (operator.equals("=like=")) {
                    // Convert RSQL-style *wildcards* into SQL-style %wildcards%
                    String pattern = values.get(0)
                            .replace('*', '%')
                            .toLowerCase(); // Make the search case-insensitive

                    // Apply LOWER() to both sides (field + search text)
                    return criteriaBuilder.like(
                            criteriaBuilder.lower(path.as(String.class)),
                            pattern);
                }

                // check for "not in" operator (=out=)
                // for example: tradeStatus.tradeStatus=out=(CANCELLED,REJECTED)
                if (operator.equals("=out="))
                    if (operator.equals("=out=")) {
                        return criteriaBuilder.not(path.in(values));
                    }
                // If the operator is not recognised, return null. e.g
                // /api/trades/rsql?query=counterparty.name=xyz=ABC.The operator "=xyz=" is not
                // valid or supported.

                return null;

            }

            /*
             * Future Improvements: Add logic that checks the actual field type (e.g., if
             * it's a date, parse it into LocalDate and call greaterThan() on a
             * Path<LocalDate> instead).
             */
        };

    }

}
