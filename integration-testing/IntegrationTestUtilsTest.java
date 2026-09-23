///usr/bin/env jbang "$0" "$@" ; exit $?
//SOURCES RunIntegrationTest.java

import java.nio.file.Path;
import java.util.List;

/** Negative controls for the output and coverage gates. Run from integration-testing. */
public class IntegrationTestUtilsTest {

    public static void main(String... args) throws Exception {
        String ordered = "51 PASS\n0 FAIL\n1 ABSTAIN";
        require(IntegrationTestUtils.checkRequiredOutput(ordered, new String[] {ordered}).isEmpty(),
            "the complete ordered result must pass");
        require(!IntegrationTestUtils.checkRequiredOutput("0 FAIL\n51 PASS\n1 ABSTAIN",
            new String[] {ordered}).isEmpty(), "reordered results must not satisfy an ordered contract");
        require(!IntegrationTestUtils.checkRequiredOutput("51 PASS\n0 FAIL",
            new String[] {ordered}).isEmpty(), "an absent abstention must not satisfy the contract");

        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var config = mapper.readValue(Path.of("../case-studies/spec-driven-petclinic/integration-testing/"
            + "configs/module-03-ears-usecase.json").toFile(), IntegrationTestUtils.ExampleInfo.class);
        for (String forbidden : List.of("score: 0.98", "confidence = 0.9", "rating 5", "98% satisfied")) {
            require(!IntegrationTestUtils.checkForbiddenOutput(forbidden,
                config.forbiddenOutputPatterns()).isEmpty(), "numeric assessment escaped: " + forbidden);
        }
        require(IntegrationTestUtils.checkForbiddenOutput(
            "51 out of 52 into 98% and call it done.\nOverall: ABSTAIN", config.forbiddenOutputPatterns()).isEmpty(),
            "the explicit rejection of scoring is legitimate prose");
        require(!IntegrationTestUtils.checkForbiddenOutput(
            "51 out of 52 into 98% and call it done.\nscore: 0.98", config.forbiddenOutputPatterns()).isEmpty(),
            "the exception must not hide a numeric assessment on another line");

        try {
            IntegrationTestUtils.verifyRoster(List.of("module-01-build-judge"));
            throw new AssertionError("a retired integration roster was accepted");
        }
        catch (IllegalStateException expected) {
            require(expected.getMessage().contains("differs from reactor"), "unexpected roster failure");
        }
        System.out.println("Integration harness negative controls passed");
    }

    private static void require(boolean condition, String explanation) {
        if (!condition) throw new AssertionError(explanation);
    }
}
