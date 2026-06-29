from app.schemas.workflow import WorkflowSpec, TriggerNode, ActionNode

# manually create a workflow spec to test
test_spec = WorkflowSpec(
    workflow_name="Email to Slack",
    trigger=TriggerNode(app_name="Gmail", trigger_type="new_email"),
    actions=[
        ActionNode(app_name="Slack", action_type="post_message", 
                  config={"channel": "#general"})
    ],
    description="Posts new emails to Slack"
)

print(test_spec.model_dump_json(indent=2))