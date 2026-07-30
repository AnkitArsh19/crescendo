# Generated SDKs and CLI

Crescendo maintains generated SDKs for Java, Go, PHP, Ruby, Rust, and .NET. They are generated from the OpenAPI specification, while the Node.js and Python clients are hand-written. The generated clients expose the public API groups such as Workflows, Emails, Templates, Domains, Connections, and App Catalog.

## Keep generated clients in sync

The generated clients should be treated as a convenience layer over the OpenAPI contract. When upgrading one, regenerate or update it from the released spec and verify the API version in your lockfile. The live [OpenAPI reference](/docs/api/workflows) is the authoritative source for endpoint paths, required fields, and scopes.

## Java

```java
// Generated API and model names depend on the released SDK version.
// Configure the bearer token once, then use the API group that owns the endpoint.
var configuration = new io.crescendo.client.ApiClient();
configuration.setBearerToken(System.getenv("CRESCENDO_API_KEY"));
```

## Go

```go
// Generated API names depend on the released SDK version.
configuration := crescendo.NewConfiguration()
configuration.AddDefaultHeader("Authorization", "Bearer "+os.Getenv("CRESCENDO_API_KEY"))
client := crescendo.NewAPIClient(configuration)
```

## .NET

```csharp
// Configure the generated client with a server-side API key.
var configuration = new Configuration {
    AccessToken = Environment.GetEnvironmentVariable("CRESCENDO_API_KEY")
};
```

## PHP, Ruby, and Rust

Use the generated client’s configuration object to set the Bearer token. If you do not want a generated client, the live API reference includes equivalent examples in PHP, Ruby, and Rust for every endpoint.

## CLI

The CLI package is named `@crescendo/cli`. It is useful for local development and scripting, but avoid passing an API key directly on a command line in shared terminals or CI logs. Prefer a secret environment variable.

```bash
npm install --global @crescendo/cli
export CRESCENDO_API_KEY='re_...'
```

Run `crescendo --help` after installation to see the commands available in the installed release. This avoids documentation drifting from the CLI version you actually use.
