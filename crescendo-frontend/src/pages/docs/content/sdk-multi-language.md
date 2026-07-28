# Multi-Language SDK & API Reference

Crescendo provides auto-generated SDK client libraries for six additional programming languages, built directly from our OpenAPI 3.0 specification, along with a standalone Command Line Interface (CLI).

---

## 1. Java (`crescendo-java`)

### Installation (Maven)
```xml
<dependency>
  <groupId>com.crescendo</groupId>
  <artifactId>crescendo-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Usage Example
```java
import com.crescendo.sdk.ApiClient;
import com.crescendo.sdk.api.EmailsApi;
import com.crescendo.sdk.model.SendEmailRequest;
import com.crescendo.sdk.model.SendEmailResponse;

public class Main {
    public static void main(String[] args) {
        ApiClient client = new ApiClient();
        client.setBearerToken(System.getenv("CRESCENDO_API_KEY"));

        EmailsApi emailsApi = new EmailsApi(client);

        SendEmailRequest request = new SendEmailRequest()
            .from("notifications@domain.com")
            .to("user@example.com")
            .subject("Welcome to Crescendo")
            .htmlBody("<h1>Hello from Java</h1>");

        try {
            SendEmailResponse response = emailsApi.sendEmail(request);
            System.out.println("Email Sent. ID: " + response.getId());
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}
```

---

## 2. Go (`crescendo-go`)

### Installation
```bash language-bash
go get github.com/crescendo-app/crescendo-go
```

### Usage Example
```go
package main

import (
	"context"
	"fmt"
	"os"

	crescendo "github.com/crescendo-app/crescendo-go"
)

func main() {
	cfg := crescendo.NewConfiguration()
	cfg.AddDefaultHeader("Authorization", "Bearer "+os.Getenv("CRESCENDO_API_KEY"))

	client := crescendo.NewAPIClient(cfg)

	payload := crescendo.SendEmailRequest{
		From:     "notifications@domain.com",
		To:       "user@example.com",
		Subject:  "Welcome to Crescendo",
		HtmlBody: "<h1>Hello from Go</h1>",
	}

	resp, _, err := client.EmailsAPI.SendEmail(context.Background()).SendEmailRequest(payload).Execute()
	if err != nil {
		fmt.Printf("Error sending email: %v\n", err)
		return
	}

	fmt.Printf("Email Sent. ID: %s\n", resp.GetId())
}
```

---

## 3. Rust (`crescendo-rust`)

### Usage Example
```rust
use crescendo_sdk::apis::configuration::Configuration;
use crescendo_sdk::apis::emails_api;
use crescendo_sdk::models::SendEmailRequest;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut config = Configuration::new();
    config.bearer_access_token = Some(std::env::var("CRESCENDO_API_KEY")?);

    let payload = SendEmailRequest {
        from: "notifications@domain.com".to_string(),
        to: "user@example.com".to_string(),
        subject: "Welcome from Rust".to_string(),
        html_body: "<h1>Hello from Rust</h1>".to_string(),
        text_body: None,
        email_type: Some("TRANSACTIONAL".to_string()),
        tags: None,
    };

    let result = emails_api::send_email(&config, payload).await?;
    println!("Email Sent. ID: {}", result.id);

    Ok(())
}
```

---

## 4. C# / .NET (`crescendo-dotnet`)

### Usage Example
```csharp
using System;
using System.Threading.Tasks;
using Crescendo.Sdk.Api;
using Crescendo.Sdk.Client;
using Crescendo.Sdk.Model;

class Program
{
    static async Task Main(string[] args)
    {
        Configuration config = new Configuration();
        config.AccessToken = Environment.GetEnvironmentVariable("CRESCENDO_API_KEY");

        var emailsApi = new EmailsApi(config);

        var request = new SendEmailRequest(
            from: "notifications@domain.com",
            to: "user@example.com",
            subject: "Welcome from .NET",
            htmlBody: "<h1>Hello from C#</h1>"
        );

        try
        {
            SendEmailResponse response = await emailsApi.SendEmailAsync(request);
            Console.WriteLine($"Email Sent. ID: {response.Id}");
        }
        catch (ApiException e)
        {
            Console.WriteLine($"API Error [{e.ErrorCode}]: {e.Message}");
        }
    }
}
```

---

## 5. Command Line Interface (`crescendo-cli`)

The Crescendo CLI enables developers to trigger workflows, list email logs, and inspect system telemetry directly from terminal scripts.

### Installation
```bash language-bash
npm install -g @crescendo/cli
```

### CLI Commands
```bash language-bash
# Authenticate CLI
crescendo login --key cm_sk_live_1234567890abcdef

# Send a transactional email
crescendo emails send --from hi@domain.com --to user@example.com --subject "CLI Test" --body "<h1>Hello</h1>"

# Trigger a workflow execution
crescendo workflows trigger --id wf_998877 --data '{"event": "user.signup"}'

# List active email logs
crescendo logs list --status DELIVERED --limit 10
```
