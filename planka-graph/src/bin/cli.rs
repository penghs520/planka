use pgraph::cli;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    cli::start_cli().await
} 