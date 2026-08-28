Remaining plan — next steps

1. Remove deprecated stubs
   - Delete ServiceLocator (already removed) and any remaining deprecated placeholders.

2. Finalize debug-header handling
   - Gate or remove X-Debug-Username in production; prefer auth plugin or test-only TestFakes.

3. Expand test fakes
   - Add TestFakes entries for email sender, time/clock, ID generators as needed.

4. Standardize test components
   - Ensure all server tests call module(TestServerComponent::class.create(TestFakes())).
   - Enable kspTest (already done) so generated factories are available.

5. CI and seeding
   - Document CI flags: use -Dgamearena.testSeed=true to enable deterministic seeding or prefer TestFakes.
   - Add a job to run :server:test in CI after KSP generation.

6. Integration tests
   - Add end-to-end login/register flows (cookies), SSE/connectivity smoke tests.

7. Clean build and validation
   - Run: .\gradlew.bat :server:compileTestKotlin --console=plain then :server:test --tests "cz.kotu.gamearena.*"

8. Documentation
   - Update AGENTS.md (done) and README with running instructions for local dev and CI.

9. Optional
   - Replace temporary TestFakes DB with an in-memory Room driver if desirable.
   - Consider migrating to a full DI component for server runtime if more bindings are needed.

Notes for next chat
- I can implement any of the above; tell me which item(s) to prioritize.
- If you want, run the full server test suite now and I will report failures.
