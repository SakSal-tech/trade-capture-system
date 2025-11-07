package com.technicalchallenge.service;

import org.junit.jupiter.api.Test; // JUnit test annotation
import org.springframework.data.jpa.domain.Specification;

import com.technicalchallenge.model.Trade;

import static org.junit.jupiter.api.Assertions.*; // For assertions like assertNotNull
import org.mockito.Mockito;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.OrNode;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Arrays;
import java.util.List;

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
        // check spec is not null to make sure the visitor actually returns something
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

    /*
     * Parameterized test to verify that TradeRsqlVisitor correctly handles multiple
     * values for 'in' and 'out' operators. This simulates queries like:
     * tradeType=in=(SPOT,OPTION,SWAP)
     * tradeStatus.tradeStatus=out=(CANCELLED,REJECTED,LIVE)
     * The test splits the value string from the CSV source into a list, constructs
     * a ComparisonNode, and asserts that the visitor returns a non-null
     * Specification for these multi-value scenarios.
     */
    @ParameterizedTest
    @CsvSource({

            "tradeType, =in=, SPOT; option;swap",
            "tradeStatus.tradeStatus,=out=,CANCELLED;REJECTED;LIVE"

    })

    void testVisitComparisonNode_multipleValuesInOut(String field, String operator, String value) {
        // Split the semicolon-separated string into a list of values for the operator

        List<String> values = Arrays.asList(value.split(";"));// separate the items in
        // IN OUT

        ComparisonNode node = new ComparisonNode(new ComparisonOperator(operator), field, Arrays.asList(value));
        TradeRsqlVisitor visitor = new TradeRsqlVisitor();
        Specification<Trade> spec = visitor.visit(node, null);
        assertNotNull(spec, "Speccification should not be null for operator " + operator + " and values " + values);

    }

    /**
     * Parameterized test to verify TradeRsqlVisitor handles wildcard patterns for
     * the 'like' operator.
     * Tests patterns at the start, end, and middle of the string.
     */
    @ParameterizedTest
    @CsvSource({
            "counterparty.name,=like=,Bank*",
            "counterparty.name,=like=,*Bank",
            "counterparty.name,=like=,*Ban*"
    })
    void testVisitComparisonNode_wildcardLikePatterns(String field, String operator, String value) {
        List<String> values = Arrays.asList(value); // Single value in a list
        ComparisonNode node = new ComparisonNode(new ComparisonOperator(operator), field, values);
        TradeRsqlVisitor visitor = new TradeRsqlVisitor();
        Specification<Trade> spec = visitor.visit(node, null);
        assertNotNull(spec, "Specification should not be null for wildcard pattern: " + value);
    }

    /**
     * Test that TradeRsqlVisitor handles nonexistent fields .
     * Expects an IllegalArgumentException or similar when an invalid field is used.
     */
    @Test
    void testVisitComparisonNode_nonexistentField() {
        List<String> values = Arrays.asList("SomeValue");
        ComparisonNode node = new ComparisonNode(new ComparisonOperator("=="), "nonexistentField", values);
        TradeRsqlVisitor visitor = new TradeRsqlVisitor();

        assertThrows(IllegalArgumentException.class, () -> visitor.visit(node, null),
                "Expected exception for nonexistent field");
    }

    /**
     * Test that TradeRsqlVisitor handles unexpected value types (e.g., string for
     * numeric field).
     * Expects an IllegalArgumentException or similar when a type mismatch occurs
     * for
     * example, passing a string to a numeric field.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testVisitComparisonNode_unexpectedValueType() {
        // GIVEN: Using a numeric field (tradeId) but passing a non-numeric value
        List<String> values = Arrays.asList("NotANumber");
        ComparisonNode node = new ComparisonNode(new ComparisonOperator("=="), "tradeId", values);
        TradeRsqlVisitor visitor = new TradeRsqlVisitor();

        // The visitor builds a Specification object
        Specification<Trade> spec = visitor.visit(node, null);

        // Mock JPA Criteria API objects (no real DB connection)
        @SuppressWarnings("unchecked") // to avoid unchecked conversion” or “raw type used without generics from
                                       // Mockito.when(path.getJavaType()).thenReturn(Long.class); which is raw types
                                       // here. It's intentional and safe for this mock test

        Root<Trade> root = (Root<Trade>) Mockito.mock(Root.class); // Represents the entity "Trade"
        CriteriaQuery<?> query = Mockito.mock(CriteriaQuery.class); // Represents the SQL query
        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class); // Used to build WHERE clauses
        Path<?> path = Mockito.mock(Path.class); // Represents the field path (tradeId)

        // Define mock behavior
        Path rawPath = (Path) path;
        Class rawClass = (Class) Long.class;
        Mockito.when(root.get(Mockito.eq("tradeId"))).thenReturn(rawPath); // Use raw type cast to avoid generics error
        Mockito.when(path.getJavaType()).thenReturn(rawClass); // Use raw type cast to avoid generics error

        // WHEN + THEN
        // Now, toPredicate() uses these mocks, tries to convert "NotANumber" to Long,
        // which throws NumberFormatException → wrapped as IllegalArgumentException.
        assertThrows(IllegalArgumentException.class,
                () -> spec.toPredicate(root, query, cb),
                "Expected IllegalArgumentException for invalid numeric value");
    }

}
