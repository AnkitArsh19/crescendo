from app.schemas.workflow import WorkflowSpec, TriggerNode, ActionNode

# manually create a workflow spec to test
test_spec = WorkflowSpec(
    workflow_name="Email to Slack",
    trigger=TriggerNode(app_key="gmail", trigger_key="new_email"),
    actions=[
        ActionNode(app_key="slack", action_key="sendMessage", 
                  config={"channel": "#general"})
    ],
    description="Posts new emails to Slack"
)

if __name__ == "__main__":
    print(test_spec.model_dump_json(indent=2))