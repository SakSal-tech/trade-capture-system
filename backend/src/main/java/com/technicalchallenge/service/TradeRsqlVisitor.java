package com.technicalchallenge.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.technicalchallenge.model.Trade;

import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;

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
                result = spec; // first one becomes base
            } else {
                result.and(spec);// chain others with AND
            }
        }

        return result;
    }

    // visit(OrNode...), combines child specs with OR
    @Override
    public Specification<Trade> visit(OrNode node, Void param) {
        return null;
    }

    @Override
    public Specification<Trade> visit(ComparisonNode node, Void param) {
        return null;
    }

}
