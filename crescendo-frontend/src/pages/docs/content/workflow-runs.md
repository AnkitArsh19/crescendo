# Workflow runs and troubleshooting

A workflow run records one execution of an active workflow. Open **History**, select a run, and inspect the step timeline when a workflow does not behave as expected.

## Run states

| State | Meaning |
| --- | --- |
| `PENDING` | The run has been accepted and is waiting to execute. |
| `RUNNING` | One or more steps are executing. |
| `SUCCESS` | All required executed paths completed successfully. |
| `FAILED` | A required step failed and the run could not continue. |
| `CANCELLED` | The run was cancelled before completion. |
| `SKIPPED` | A step was not selected because its branch was not taken. |

## Diagnose a failed step

1. Find the first failed step rather than the last one in the graph.
2. Compare its evaluated inputs with the action’s required fields.
3. Check the connection: it may be expired, missing a provider scope, or point to a different account than the selected resource.
4. Read the provider response and error details. Correct the workflow, connection, or target resource as appropriate.
5. Save the changed workflow and trigger a new test event.

## Branches and skipped steps

An if node selects its true or false path. A switch selects one named output. Downstream nodes on untaken paths are marked skipped rather than failed. At a merge point, the engine waits only for the paths that were actually selected.

## Operational advice

- Use a dedicated test account or test channel for new provider integrations.
- Make side-effecting actions idempotent at the destination when possible.
- Keep enough source data in the trigger payload to diagnose a run, but do not include secrets or unnecessary personal data.
- If a provider is rate-limiting or unavailable, correct the cause before sending a new run. Do not assume a failed run can safely be replayed without considering duplicate external side effects.
