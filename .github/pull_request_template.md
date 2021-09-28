Please make sure you consider the following:

- [ ] Add tests that use __typename in queries
- [ ] Does this change work with all nadel transformations (rename, type rename, hydration, etc)? Add tests for this.
- [ ] Is it worth using hints for this change in order to be able to enable a percentage rollout?
- [ ] Do we need to add integration tests for this change in the graphql gateway?
- [ ] Do we need a pollinator change for this?
