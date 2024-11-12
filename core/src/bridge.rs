use std::sync::Arc;

use crate::{config::Config, db::tokens, error::Error, rt, Auth2};

#[derive(uniffi::Object)]
pub struct Auth2Bridge {
    inner: Arc<Auth2>,
}

#[uniffi::export]
impl Auth2Bridge {
    #[uniffi::constructor]
    pub fn new(config: Config) -> Result<Arc<Self>, Error> {
        rt().block_on(async move {
            let inner = Auth2::new(config).await?;
            Ok(Arc::new(Self { inner }))
        })
    }

    pub async fn db_is_migration_available(&self) -> Result<bool, Error> {
        let inner = self.inner.clone();
        rt().spawn(async move { inner.db_is_migration_available().await })
            .await?
    }

    pub async fn db_run_migration(&self) -> Result<(), Error> {
        let inner = self.inner.clone();
        rt().spawn(async move { inner.db_run_migration().await })
            .await?
    }

    pub async fn db_reset(&self) -> Result<(), Error> {
        let inner = self.inner.clone();
        rt().spawn(async move { inner.db_reset().await }).await?
    }

    pub async fn add_token(
        &self,
        account: String,
        service: Option<String>,
        secret: String,
        algorithm: Option<TokenAlg>,
        digits: Option<u8>,
        period: Option<u32>,
    ) -> Result<TokenDetail, Error> {
        let inner = self.inner.clone();
        rt().spawn(async move {
            inner
                .add_token(account, service, secret, algorithm, digits, period)
                .await
        })
        .await?
    }

    pub async fn add_token_from_url(&self, url: String) -> Result<TokenDetail, Error> {
        let inner = self.inner.clone();
        rt().spawn(async move { inner.add_token_from_url(url).await })
            .await?
    }

    pub async fn remove_token(&self, id: u64) -> Result<(), Error> {
        let inner = self.inner.clone();
        rt().spawn(async move { inner.remove_token(id).await })
            .await?
    }

    pub async fn list_tokens(&self) -> Result<Vec<Token>, Error> {
        let inner = self.inner.clone();
        rt().spawn(async move { inner.list_tokens().await }).await?
    }

    pub async fn token_detail(&self, id: u64) -> Result<Option<TokenDetail>, Error> {
        let inner = self.inner.clone();
        rt().spawn(async move { inner.token_detail(id).await })
            .await?
    }

    pub async fn generate_current(&self, id: u64) -> Result<TokenResult, Error> {
        let inner = self.inner.clone();
        rt().spawn(async move { inner.generate_current(id).await })
            .await?
    }
}

#[derive(Debug, uniffi::Record)]
pub struct Token {
    pub id: u64,
    pub account: String,
    pub service: Option<String>,
}

impl From<tokens::TokenListItem> for Token {
    fn from(v: tokens::TokenListItem) -> Self {
        Self {
            id: v.id,
            account: v.account,
            service: v.service,
        }
    }
}

#[derive(Debug, uniffi::Record)]
pub struct TokenDetail {
    pub id: u64,
    pub account: String,
    pub service: Option<String>,
    pub algorithm: TokenAlg,
    pub digits: u8,
    pub period: u32,
}

impl From<tokens::Token> for TokenDetail {
    fn from(v: tokens::Token) -> Self {
        Self {
            id: v.id,
            account: v.data.account,
            service: v.data.service,
            algorithm: v.data.algorithm.into(),
            digits: v.data.digits,
            period: v.data.period,
        }
    }
}

#[derive(Debug, uniffi::Enum)]
pub enum TokenAlg {
    Sha1,
    Sha256,
    Sha512,
}

impl From<tokens::TokenAlg> for TokenAlg {
    fn from(v: tokens::TokenAlg) -> Self {
        match v {
            tokens::TokenAlg::Sha1 => Self::Sha1,
            tokens::TokenAlg::Sha256 => Self::Sha256,
            tokens::TokenAlg::Sha512 => Self::Sha512,
        }
    }
}

#[derive(Debug, uniffi::Record)]
pub struct TokenResult {
    pub current: String,
    pub expires: u32,
}
