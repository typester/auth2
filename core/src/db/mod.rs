use std::sync::{Arc, LazyLock};

use frostflake::{GeneratorAsync, GeneratorOptions};
use sqlx::SqlitePool;

use migrate::MigrateDatabase;
use tokens::TokensDatabase;

pub mod migrate;
pub mod tokens;

pub trait Database: Send + Sync + MigrateDatabase + TokensDatabase {}

pub struct Db {
    database_url: String,
    pool: SqlitePool,
    id_generator: LazyLock<Arc<GeneratorAsync>>,
}

impl Db {
    pub fn new(database_url: String) -> anyhow::Result<Arc<Self>> {
        let pool = SqlitePool::connect_lazy(&database_url)?;
        Ok(Arc::new(Self {
            database_url,
            pool,
            id_generator: LazyLock::new(|| GeneratorAsync::spawn(GeneratorOptions::default())),
        }))
    }

    async fn next_id(&self) -> anyhow::Result<u64> {
        self.id_generator.generate().await
    }
}

impl Database for Db {}
