# Tests

There are two types of tests, the legacy `.yml` tests and the new `graphql.nadel.tests.next.NadelIntegrationTest` tests.

# Creating a test

If you need, refer to `graphql.nadel.tests.next.fixtures.basic.EchoTest` for a basic example.

1. Use the IntelliJ file template to create a new test. There should be a `Nadel Test` option in the "New File" popup.
2. Write the query, schema and dummy runtime wiring sufficient for your test.
3. To generate the snapshot, run `Update Test Snapshots` configuration in IntelliJ or `graphql.nadel.tests.next.UpdateTestSnapshotsKt.main`
4. Review the snapshot to ensure correctness, as the contents are the assertions for the test.
5. If you need to update the test snapshot, run the `main` function inside the generated snapshot file.
6. Commit the snapshot once you are happy with it.
