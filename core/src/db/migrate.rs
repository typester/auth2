use std::{collections::HashMap, path::Path};

use anyhow::bail;
use async_trait::async_trait;
use sqlx::{migrate::Migrate, Connection, SqliteConnection};
use tokio::fs::File;

use super::Db;

#[async_trait]
pub trait MigrateDatabase {
    async fn is_migration_available(&self) -> anyhow::Result<bool>;
    async fn run_migration(&self) -> anyhow::Result<()>;
    async fn reset_database(&self) -> anyhow::Result<()>;
}

#[async_trait]
impl MigrateDatabase for Db {
    async fn is_migration_available(&self) -> anyhow::Result<bool> {
        tracing::debug!("checking available migrations");
        let migrator = sqlx::migrate!();
        // Seems like the SqlitePool doesn't implement Migrate trait
        let mut conn = SqliteConnection::connect(&self.database_url).await?;

        conn.ensure_migrations_table().await?;

        let applied_migrations: HashMap<_, _> = conn
            .list_applied_migrations()
            .await?
            .into_iter()
            .map(|m| (m.version, m))
            .collect();

        let has_mismatched_migration = migrator
            .iter()
            .filter(|m| !m.migration_type.is_down_migration())
            .find(|m| {
                if let Some(applied) = applied_migrations.get(&m.version) {
                    m.checksum != applied.checksum
                } else {
                    false
                }
            });
        if has_mismatched_migration.is_some() {
            bail!("mismatched migration is found");
        }

        let has_unapplied_migration = match migrator
            .iter()
            .filter(|m| !m.migration_type.is_down_migration())
            .find(|m| !applied_migrations.contains_key(&m.version))
        {
            Some(_) => true,
            None => false,
        };

        Ok(has_unapplied_migration)
    }

    async fn run_migration(&self) -> anyhow::Result<()> {
        let migrator = sqlx::migrate!();
        Ok(migrator.run(&self.pool).await?)
    }

    async fn reset_database(&self) -> anyhow::Result<()> {
        let path = Path::new(&self.database_url["sqlite://".len()..]);
        let file = File::create(path).await?;
        file.set_len(0).await?;
        Ok(())
    }
}
