---
sessionId: session-260820-083657-8huu
---

# Requirements

### Overview & Goals
Move legal-action validation into `ContactsBoardState` and make it explain rejected actions through a nullable `String` result.

### Scope
#### In scope
- Implement `ContactsBoardState.isActionLegal(...)` as the single legality check, returning `null` when legal and an explanatory `String` when rejected.
- Replace duplicated inline legality checks in `ContactsGameFacadeImpl` with board-state validation.
- Display the board-state validation message in `ContactsPlayerScreen` for invalid selections.
- Preserve existing gameplay behavior, including mismatch faults and multi-connect resolution state changes.
- Add focused unit coverage for validation messages and facade behavior.

#### Out of scope
- Changing `ContactsGameFacade` method signatures.
- Updating `ServerContactsGameFacade` or changing facade method signatures.
- Changing network DTOs or persistence.

# Technical Design

### Current Implementation
- `../core/src/commonMain/kotlin/cz/kotu/game/contacts/model/ContactsBoardState.kt` already defines the intended `isActionLegal(...)` hook, but it is unimplemented and currently returns `Unit` implicitly.
- `../core/src/commonMain/kotlin/cz/kotu/game/contacts/model/ContactsGameFacadeImpl.kt` performs ownership, solved-state, action-count, action-type, and multi-connect checks inline, silently returning on failure.
- `ContactsGameFacade` remains a `Unit`-returning in-process API; `ServerContactsGameFacade` independently maps thrown errors to `String?` and is unchanged.

### Key Decisions
- Use `String?` for `isActionLegal`: `null` means legal, non-null is the specific rejection reason.
- Keep board-state validation pure: it only inspects the supplied state and action inputs and does not mutate faults, hints, solved contacts, or resolution state.
- Keep contact-number mismatch outside legality validation because it is a valid attempted connect that produces a fault through `withFaultFor`.
- Have facade paths call the validator before state mutation and return silently as they do today when an error is present.

### Proposed Changes
- Implement ordered checks in `ContactsBoardState.isActionLegal(...)` for unsupported/disallowed action types, invalid selected-contact counts, acting-player ownership, opposing-player ownership, solved contacts, and multi-connect target-rack consistency/resolution constraints.
- Refactor `ContactsGameFacadeImpl.action`, `connect`, and `multiConnect` to delegate applicable checks to `isActionLegal`, removing duplicated boolean conditions while preserving special resolution flow and mismatch handling.
- Use stable, descriptive error strings that identify the first failed rule; tests should assert the message contract for each rule.

### File Structure
- Modify `../core/src/commonMain/kotlin/cz/kotu/game/contacts/model/ContactsBoardState.kt`.
- Modify `../core/src/commonMain/kotlin/cz/kotu/game/contacts/model/ContactsGameFacadeImpl.kt`.
- Extend `../core/src/commonTest/kotlin/cz/kotu/game/contacts/model/ContactsBoardStateTest.kt` with legal/illegal validator cases.
- Update `../core/src/commonTest/kotlin/cz/kotu/game/contacts/model/ContactsGameFacadeImplTest.kt` only where needed to verify existing state transitions remain unchanged.

### Risks
- Validation order can change which message is returned when multiple rules fail; define and test a deterministic order.
- Multi-connect and `ResolveMultiConnect` have distinct state-dependent rules; preserve their existing specialized flow rather than forcing all resolution behavior into one generic branch.

# Testing

### Validation Approach
Use focused common tests for the pure board validator and existing facade regression tests for state transitions.

### Key Scenarios
- Valid standard, multi-connect, hint, and resolution actions return `null`.
- Invalid action counts, ownership, solved contacts, disallowed action types, and invalid multi-connect targets return the expected descriptive string.
- Facade still solves matching contacts, records faults for mismatches, and preserves multi-connect resolution behavior.

### Edge Cases
- A mismatch remains a legal attempted connect and still creates a fault rather than returning a validation error.
- Multiple invalid conditions produce the documented first error deterministically.
- Existing test fixtures continue to resolve contact IDs through the board state without introducing duplicate contact storage.

# Delivery Steps

### ✓ Step 1: Implement board-state validation contract
`ContactsBoardState.isActionLegal(...)` returns deterministic error text for illegal actions and `null` for legal ones.

- Implement checks for action availability and selection counts.
- Validate player ownership, opposing ownership, and unsolved-contact requirements.
- Cover multi-connect target-rack and resolution-specific constraints without mutating state.
- Define stable message text and deterministic check ordering.

### ✓ Step 2: Delegate facade legality checks
`ContactsGameFacadeImpl` uses `ContactsBoardState.isActionLegal(...)` instead of duplicating legal-action predicates.

- Replace inline checks in `action`, `connect`, and `multiConnect` where applicable.
- Preserve the existing `Unit` facade contract and silent rejection behavior.
- Keep contact mismatch fault generation and multi-connect resolution state transitions unchanged.

### ✓ Step 3: Add focused regression coverage
Core tests verify validation messages and confirm existing Contacts gameplay behavior is preserved.

- Add board-state tests for valid actions and each major rejection category.
- Update facade tests only where needed to verify delegation-compatible behavior.
- Do not run Gradle commands; the relevant verification command for implementation is `.[?25lgradlew.bat --console=plain :core:jvmTest`.