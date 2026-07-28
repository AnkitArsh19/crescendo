# Analytics & Insights

The Insights Dashboards provide comprehensive telemetry reporting for email broadcast performance, customer audience engagement, and overall automated workflow throughput. Monitoring these diagnostic indicators allows you to optimize communication strategies and resource utilization.

## Email Campaign Performance Metrics

When you open **Insights** or navigate to an individual completed campaign report, Crescendo renders visual statistical evaluations computed from Mailer feedback notifications and tracking endpoints.

### Key Performance Indicators
* **Delivery Rate:** The percentage of dispatched email messages accepted cleanly by target recipient mail servers without triggering hard bounce rejection errors.
* **Open Rate:** The percentage of verified delivered messages that recipients opened. Tracking is executed via zero-footprint transparent pixel rendering inserted automatically into outgoing HTML templates.
* **Click-Through Rate (CTR):** Measures engagement activity by calculating the ratio of unique recipients who clicked at least one hyperlinked element inside your broadcasted message body.
* **Unsubscribe Ratio:** Tracks the total number of contacts electing to trigger opt-out processing by clicking the mandatory `{{unsubscribe_url}}` compliance footer link.
* **Spam Complaint Score:** Monitors negative user reports across provider Feedback Loop (FBL) architectures. Maintaining a complaint ratio below 0.1 percent is critical to preserving verified domain reputations with major email services.

## Audience Engagement Analytics

Beyond single campaign execution events, the Insights console tracks historical audience interactions across extended time horizons:
* **Contact Growth Trends:** Visualizes net subscriber acquisition rates over daily, weekly, or quarterly periods after subtracting unsubscribed or bounced accounts.
* **Tag & Segment Engagement Scores:** Compares interaction ratios across diverse user tags (e.g., identifying that contacts tagged as "Enterprise" exhibit double the baseline newsletter click engagement compared to trial user tiers).

## Workflow Execution Infrastructure Telemetry

To support operational DevOps monitoring and verify enterprise system stability, the workflow telemetry tab displays quantitative usage statistics:
* **Throughput & Total Execution Counts:** Highlights daily and monthly aggregate workflow runs compared against active workspace tier quotas.
* **Average Step Latency (p50 / p95):** Plots the operational processing speed (measured in milliseconds) across external API calls and internal logical evaluation nodes. Noticeable upward latency deviations often indicate external third-party SaaS provider congestion or throttling events.
* **Error & Failure Frequency Distribution:** Charts failed workflow invocations categorized by originating integration app (e.g., highlighting repeated authentication failures within a connected Slack channel vs. timeout occurrences from a custom internal webhook).

## Exporting Telemetry Reports

Workspace administrators requiring offline spreadsheet analysis or executive compliance reporting can click **Export Analytics Data** from the insights top control bar. Reports export instantly as cleanly formatted UTF-8 CSV tables or PDF summary documents containing complete date ranges and raw statistical counts.
