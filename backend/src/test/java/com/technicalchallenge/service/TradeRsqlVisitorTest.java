package com.technicalchallenge.service;

import org.junit.jupiter.api.Test; // JUnit test annotation
import org.springframework.data.jpa.domain.Specification;

import com.technicalchallenge.model.Trade;

import static org.junit.jupiter.api.Assertions.*; // For assertions like assertNotNull
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays; // For creating lists easily
/*This class tests the logic of TradeRSQLVIsitor class method Visit(And), visit(Or) one method each and visit(comparison) will be broken down into methods as it handles many operators */

public class TradeRsqlVisitorTest {

    @Test
    // verifies that the visit(AndNode) function in TradeRsqlVisitor class correctly
    // processes an RSQL "AND" node and returns a valid (non-null)
    // Specification<Trade> object.
    public void testVisitAndNode() {
        // GIVEN
        // Creating tow child nodes
        // First child node
        ComparisonNode childOne = new ComparisonNode(new ComparisonOperator("==", false), // operator not a multi value
                "counterparty.name", // field name
                Arrays.asList("BigBank"));// Creates a list with one value. The RSQL syntax allows for multiple values,
                                          // like field=in=(A,B,C). The ComparisonNode constructor expects a list of
                                          // arguments, not just a single value.

        // 2nd child node
        ComparisonNode childTwo = new ComparisonNode(new ComparisonOperator("=="), "tradeStatus.tradeStatus",
                Arrays.asList("Live"));

        // AndNode with 2 child nodes to simulate a query e.g
        // counterparty.name==BigBank;tradeStatus.tradeStatus==Live
        AndNode andNode = new AndNode(Arrays.asList(childOne, childTwo));

        // Creating an object of TradeRsqlVisitor
        TradeRsqlVisitor visitor = new TradeRsqlVisitor();

        // WHEN
        // Calling the visit method with the AndNode.The result is a Specification
        // object of type Trade class, which represents the combined query logic.
        Specification<Trade> spec = visitor.visit(andNode, null);

        // THEN
        // check spec is not null to make sure your visitor actually returns something
        // and that visitor successfully built a Specification object from the RSQL AST
        assertNotNull(spec, "Specification should not be null");

        // To test if test fails to check if test will fail if the visitor does not
        // return a valid Specification<Trade> object
        // assertNull(spec, "Specification should not be null");

    }

    @Test
    public void testVisitOrNode() {

        ComparisonNode childOne = new ComparisonNode(new ComparisonOperator("==", false),
                "counterparty.name",
                Arrays.asList("BigBank"));
        ComparisonNode childTwo = new ComparisonNode(new ComparisonOperator("=="), "tradeStatus.tradeStatus",
                Arrays.asList("Live"));

        OrNode orNode = new OrNode(Arrays.asList(childOne, childTwo));

        TradeRsqlVisitor visitor = new TradeRsqlVisitor();

        Specification<Trade> spec = visitor.visit(orNode, null);
        assertNotNull(spec, "Specification should not be null");

        // assertNull(spec, "Specification should not be null");

    }

    @ParameterizedTest
    // parameterised to test many combinations of operators, field names, and
    // values, making sure visitor logic works for all scenarios
    @CsvSource({
            "counterparty.name,==,BigBank",
            "tradeStatus.tradeStatus,!=,Live",
            "tradeType,=in=,SWAP",
            "counterparty.name,=like=,*Bank*",
            "tradeStatus.tradeStatus,=out=,CANCELLED",
            "tradeStatus.tradeStatus,=out=,REJECTED",
            "counterparty.name,=like=,*BIG*"

    })

    void testVisitComparisonNodeCombinations(String field, String operator, String value) {
        System.out.println("Testing: field=" + field + ", operator=" + operator + ", value=" + value);

        ComparisonNode node = new ComparisonNode(new ComparisonOperator(operator), field, Arrays.asList(value));
        TradeRsqlVisitor visitor = new TradeRsqlVisitor();
        Specification<Trade> spec = visitor.visit(node, null);
        System.out.println("Specification result: " + spec);

        assertNotNull(spec, "Specification should not be null");

    }

    // To verify that the visit(ComparisonNode) method can handle a scenario where
    // the value provided for comparison is null e.g searching for records where a
    // field is null is valid (e.g., "find trades with no counterparty name"). The
    // visitor should handle null values gracefully and return a Specification
    @Test
    void testVisitComparisonNode_nullValue() {

        // A ComparionNode with a null value for the field, what happens if someone
        // tries to search for trades where the counterparty name is null
        ComparisonNode node = new ComparisonNode(new ComparisonOperator("=="), "counterparty.name",
                Arrays.asList((String) null));

        TradeRsqlVisitor visitor = new TradeRsqlVisitor();
        Specification<Trade> spec = visitor.visit(node, null);

        // Assert that a Specification is still returned (not null)
        assertNotNull(spec, "Specification should not be null when value is null");
    }

    @Test
    /*
     * The test tries to call visitor.visit with an invalid operator.
     * If no exception is thrown, fail() will make the test fail.
     * If IllegalArgumentException is thrown, the test passes.
     */
    void testVisitComparisonNode_invalidOperator() {
        // GIVEN: a ComparisonNOde with an invalid operator
        ComparisonNode node = new ComparisonNode(new ComparisonOperator("=invalid="), "counterparty.name",
                Arrays.asList("BigBank"));
        TradeRsqlVisitor visitor = new TradeRsqlVisitor();

        try {
            visitor.visit(node, null);
            // Call toPredicate to trigger the exception

            fail("Expected IllegalArgumentException to be thrown for invalid operator");
        } catch (IllegalArgumentException e) {
            System.out.println("Test passes, exception was thrown as expected");

        }

    }

}
