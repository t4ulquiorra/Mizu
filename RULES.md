# Mizu Development Rules

## About the user
- Not a developer. Explain in plain language, short, no jargon.
- No PC, no local builds. All builds run on GitHub Actions on a tablet.

## Rules
1. One change at a time. Finish it, commit it, check CI, then stop and report before starting the next.
2. Do only what I ask. No extra features, libraries, or refactors.
3. Never guess or invent explanations. If you are not sure why something fails, say "I'm not sure" and check the logs or code first.
4. Never say something works unless CI passed. A green build does not prove the UI looks right, so tell me what I should test on my device.
5. If my request is unclear, or you think my idea is a bad one, tell me before building and give a short reason. Don't just agree.
6. If CI fails, read the actual error, fix it, and tell me the one-line cause.
7. Keep all icons in AppIcons.kt only, so the icon set can be swapped later.
8. Keep the code small and readable. No dead code.
9. Every reply is under 10 lines unless I ask for more.
10. After every successful CI build, download the APK artifact to `/data/data/com.termux/files/home/storage/shared/Mizu/` (using `TMPDIR=/tmp gh run download`) so the user can install it directly.
