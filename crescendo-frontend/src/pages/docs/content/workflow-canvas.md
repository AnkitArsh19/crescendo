# Workflow Studio

Workflow Studio is where you build a directed workflow graph. A workflow starts with a trigger, moves through actions, and can split into named paths with logic nodes.

## Build a valid graph

1. Create a workflow and add a **trigger**. A trigger is the entry point for each run.
2. Add **actions** for the work that should happen after the trigger. An action may need a connected account selected in its configuration panel.
3. Drag from one node’s output handle to the next node’s input handle to create an edge.
4. Save with **Ctrl+S** or the Save control. The canvas validates the graph before it is persisted.
5. Activate only when every connected account and required configuration field is ready.

The canvas supports normal action nodes, branching nodes, and multiple downstream paths. It is valid for paths to reconverge when their dependencies are complete.

## Pass data between steps

Use the variable picker next to a supported configuration field to insert data from an earlier step. It inserts a reference in the form:

```text
{{steps.<step-number>.<field>}}
```

For a webhook trigger, the incoming event data is available from the trigger output. For example, a later message body can use a trigger field selected through the picker. Prefer the picker over hand-typing: field names vary by trigger and action, and the picker only shows values that are upstream of the node you are editing.

> [!IMPORTANT]
> A reference is evaluated at run time. Make sure the source value exists for every branch that can reach the destination node; otherwise the destination can receive an empty value.

## Add logic

### If / else

`Logic: If` evaluates condition rows and emits either the **true** or **false** output. Connect each output handle to the path that should run for that result.

Condition rows support equality, containment, prefix/suffix, emptiness, numeric comparisons, and regular expressions. Groups use **AND** inside a group and **OR** between groups.

### Switch

`Logic: Switch` routes to the first matching rule in rules mode, or to a numeric output in expression mode. Its output handles are named `output_0`, `output_1`, and so on. Keep the number of rules and the connected outputs aligned so a match has a destination.

## Before activation

- Select the correct connection for every provider action.
- Resolve warnings in dynamic resource fields, such as an AI-suggested Slack channel that is not available in the selected account.
- Check every required field and every variable reference.
- Start with a reversible or test destination where an action has side effects.
- After activation, use **History** to inspect the exact input, output, status, and error for each executed step.

See [Workflow runs](/docs/workflow-runs) for troubleshooting and [Connections and apps](/docs/apps-integrations) for provider setup.
