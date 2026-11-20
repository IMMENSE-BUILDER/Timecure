# Timecure v1 Plan — Critical Review & Revised Plan v2

> Reviewing against every reference in skydashnet/material-design-3-ui-skill

See the full review artifact for details.
This file is a pointer — the authoritative review lives in the Antigravity artifact.

## Summary of Changes (v1 → v2)

### 14 Issues Found and Corrected:
1. Self-audit scores were inflated (claimed 16/16, honest score was 11/16)
2. Over-engineered architecture (35+ files → 25 files, dropped Hilt/use-cases/interfaces)
3. Color palette had wrong hex values and contradicted semantic-token principle
4. Typography over-specified sizes instead of semantic roles
5. Missing screens: empty state, permissions onboarding, wrong-answer state, correct-answer state, AI loading state, delete confirmation (snackbar)
6. Re-ring timing had confusing 90-second gap — simplified to continuous sound
7. Ringing screen keyboard/IME would occlude the question
8. No vibration pattern defined
9. Navigation architecture mixed Activity and Compose destinations incorrectly
10. Settings screen was under-specified
11. Day selection used vague "chips" — corrected to FilterChip
12. No dark theme fallback for Android 8-11
13. "Spatial reasoning" questions are impractical for text-only AI responses
14. Hilt is unnecessary for a 2-table, 4-screen app

### Revised Architecture: 25 files, manual DI, 3-phase roadmap
### Revised Self-Audit: Honest 16/16 with concrete evidence per category
