# Getting Started with Crescendo

Welcome to Crescendo, a powerful automation platform built to orchestrate real-world, multi-step workflows across your favorite apps and internal APIs. 

## The Dashboard

When you log into Crescendo, you land on the **Dashboard**. From here, you can monitor your active workflows, track execution statistics, and manage your connected accounts. 

### Key Areas
- **Execution Overview:** Quickly see how many workflows have run successfully and if any are pending or failing.
- **Recent Workflows:** Jump back into the automations you were recently editing.
- **App Integrations:** Manage OAuth connections and API keys for third-party services.

## Creating Your First Workflow

Crescendo relies on a **Directed Rooted Tree** execution model. This means a workflow can branch logically (using `If/Else` or `Switch` statements) and execution follows the active path without leaving footprints on unexecuted branches.

### Step 1: Define a Trigger
Every workflow starts with a Trigger. 
1. Navigate to **Workflows → Create Workflow**.
2. Click the central **(+)** icon to add a trigger.
3. Select from native triggers like **Webhook**, **Schedule**, or app-specific triggers like **GitHub Issue Created**.

### Step 2: Add Actions
Actions are the steps executed once a workflow is triggered.
1. Click the **(+)** icon below your trigger.
2. Select an app and an action (e.g., **Gmail → Send Email** or **Slack → Send Message**).
3. Map data from previous steps using the `{}` variables button.

### Step 3: Test and Activate
- You can test individual steps directly in the canvas to ensure your API keys and mappings are correct.
- Once satisfied, toggle the workflow from **Draft** to **Active** in the top-right corner.

> [!TIP]
> **Autosave is built-in!** Crescendo uses a highly optimized 3-tier save coordinator. Your work is saved silently in the background without interrupting your flow.
