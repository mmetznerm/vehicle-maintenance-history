# GitHub Branch Protection

Use this checklist after the `PR Checks` workflow has run at least once on
GitHub. The required status checks only appear in the branch protection UI after
GitHub has seen them.

## Rule

Create a branch protection rule for:

```text
main
```

## Recommended Settings

- Require a pull request before merging.
- Require status checks to pass before merging.
- Require branches to be up to date before merging.
- Require conversation resolution before merging.
- Require linear history.
- Do not allow force pushes.
- Do not allow deletions.

For a solo repository, keep pull request approvals optional so you can still
merge your own PRs after the required checks pass. If the repository has at
least one reviewer, enable:

- Require approvals: `1`.
- Dismiss stale pull request approvals when new commits are pushed.
- Do not allow bypassing the above settings.

## Required Status Checks

Select these checks from the `PR Checks` workflow:

```text
Backend Unit Tests
Backend Integration Tests
Frontend Quality
```

After the security workflows have run at least once, also require these checks
for a stricter gate:

```text
CodeQL (java-kotlin)
CodeQL (javascript-typescript)
Dependency Review
Trivy Repository Scan
```

Only require `Dependency Review` after `Settings > Code security and analysis >
Dependency graph` is enabled and the job has passed at least once.

## Notes

- Keep job names unique across workflows so GitHub can resolve required checks
  without ambiguity.
- For a solo portfolio project, the PR history plus required checks already
  demonstrate a professional review and quality gate workflow.
