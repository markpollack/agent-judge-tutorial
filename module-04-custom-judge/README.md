# Module 04 — Write a judge

```bash
./mvnw exec:java -pl module-04-custom-judge
```

Modules 01–03 *used* judges. This one writes them, and the whole job is four steps:

> **task → criterion → evidence → judgment**

`Judge` is a `@FunctionalInterface`, so the smallest judge that can exist is a lambda:

```java
Judge pomCheck = ctx -> Files.exists(ctx.workspace().resolve("pom.xml"))
    ? Judgment.pass("pom.xml found")
    : Judgment.fail("pom.xml missing");
```

Start there. Add only what the criterion actually needs.

## A lambda has no identity

```
  Has metadata: false
```

Infrastructure cannot discover its name, so it has none in a verdict. `Judges.named(...)`
gives it one:

```java
Judge namedPom = Judges.named(pomCheck, "pom-check", "Verifies pom.xml exists",
    JudgeType.DETERMINISTIC);
```

Name a judge as soon as anything other than the call site will read its result.

## Two habits worth forming early

**State the denominator.** `"Found 4 Java file(s)"` and `"No Java files found"` are
different facts. A bare `PASS` hides which one it was.

**`ERROR` is not `FAIL`.** When the scan throws, the workspace has not been rejected — it
has not been read:

```java
catch (Exception e) {
    return Judgment.error("Error scanning: " + e.getMessage());
}
```

## Three criteria, not three votes

The last section runs three judges and requires all of them. Note what it is *not*: a
vote. These are three different questions, so the composition is a conjunction and every
result stays visible.

[Module 06](../module-06-definition-of-done) builds that out properly.
[Module 08](../module-08-jury) is the case where voting is the right answer.

## Next

[Module 05](../module-05-derived-judge) makes a reusable judge out of several facts.
