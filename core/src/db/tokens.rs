use async_trait::async_trait;
use serde::{Deserialize, Serialize};

use crate::error::Error;

use super::Db;

#[derive(Debug, sqlx::FromRow)]
pub struct Token {
    pub id: u64,
    #[sqlx(flatten)]
    pub data: TokenData,
}

#[derive(Debug, sqlx::FromRow)]
pub struct TokenData {
    pub account: String,
    pub service: Option<String>,
    pub secret: String,
    #[sqlx(json)]
    pub algorithm: TokenAlg,
    pub digits: u8,
    pub period: u32,
}

impl Default for TokenData {
    fn default() -> Self {
        Self {
            account: String::default(),
            service: None,
            secret: String::default(),
            algorithm: TokenAlg::Sha1,
            digits: 6,
            period: 30,
        }
    }
}

#[derive(Debug, sqlx::FromRow)]
pub struct TokenListItem {
    pub id: u64,
    pub account: String,
    pub service: Option<String>,
}

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "UPPERCASE")]
pub enum TokenAlg {
    Sha1,
    Sha256,
    Sha512,
}

#[async_trait]
pub trait TokensDatabase {
    async fn add_token(&self, token: TokenData) -> Result<u64, Error>;
    async fn remove_token(&self, id: u64) -> Result<(), Error>;
    async fn list_tokens(&self) -> Result<Vec<TokenListItem>, Error>;
    async fn token_detail(&self, id: u64) -> Result<Option<Token>, Error>;
}

#[async_trait]
impl TokensDatabase for Db {
    async fn add_token(&self, token: TokenData) -> Result<u64, Error> {
        let id = self.next_id().await?;

        let _ = sqlx::query("INSERT INTO tokens (id, account, service, secret, algorithm, digits, period) VALUES (?, ?, ?, ?, ?, ?, ?)")
            .bind(id as i64)
            .bind(token.account)
            .bind(token.service)
            .bind(token.secret)
            .bind(serde_json::to_string(&token.algorithm)?)
            .bind(token.digits)
            .bind(token.period)
            .execute(&self.pool).await?;

        Ok(id)
    }

    async fn remove_token(&self, id: u64) -> Result<(), Error> {
        let _ = sqlx::query("DELETE FROM tokens WHERE id = ?")
            .bind(id as i64)
            .execute(&self.pool)
            .await?;
        Ok(())
    }

    async fn list_tokens(&self) -> Result<Vec<TokenListItem>, Error> {
        let tokens: Vec<TokenListItem> =
            sqlx::query_as("SELECT id, account, service FROM tokens ORDER BY id")
                .fetch_all(&self.pool)
                .await?;
        Ok(tokens)
    }

    async fn token_detail(&self, id: u64) -> Result<Option<Token>, Error> {
        let token: Option<Token> = sqlx::query_as("SELECT * FROM tokens WHERE id = ?")
            .bind(id as i64)
            .fetch_optional(&self.pool)
            .await?;
        Ok(token)
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Arc;

    use tempfile::tempdir;

    use super::TokenData;
    use crate::db::{Database, Db};

    #[tokio::test]
    async fn test_tokens() {
        let temp_dir = tempdir().unwrap();
        let database_url = format!("sqlite://{}/database.db", temp_dir.path().to_str().unwrap());

        let db: Arc<dyn Database> = Db::new(database_url).unwrap();
        db.reset_database().await.unwrap();
        db.run_migration().await.unwrap();

        let res = db
            .add_token(TokenData {
                account: "dameleon".into(),
                secret: "hoge".into(),
                ..Default::default()
            })
            .await;
        assert!(res.is_ok());
        let id = res.unwrap();

        let tokens = db.list_tokens().await.unwrap();
        assert_eq!(tokens.len(), 1);
        assert_eq!(tokens[0].id, id);

        let token = db.token_detail(id).await.unwrap();
        assert!(token.is_some());
        let token = token.unwrap();

        assert_eq!(token.data.account, "dameleon");
        assert_eq!(token.data.secret, "hoge");

        db.remove_token(id).await.unwrap();

        let token = db.token_detail(id).await.unwrap();
        assert!(token.is_none());
        let tokens = db.list_tokens().await.unwrap();
        assert_eq!(tokens.len(), 0);
    }
}
