use std::{
    sync::{Arc, LazyLock, Once},
    time::{SystemTime, UNIX_EPOCH},
};

use anyhow::anyhow;
use bridge::{Token, TokenAlg, TokenDetail, TokenResult};
use enc::{decrypt_secret, encrypt_secret};
use totp_rs::{Secret, TOTP};
use tracing_subscriber::{layer::SubscriberExt, Registry};

use config::Config;
use db::{tokens::TokenData, Database, Db};
use error::Error;
use logger::{FFILogLayer, Logger};

mod bridge;
mod config;
mod db;
mod enc;
mod error;
mod logger;

uniffi::setup_scaffolding!();

static RT: LazyLock<tokio::runtime::Runtime> = LazyLock::new(|| {
    tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()
        .expect("failed to initialize tokio runtime")
});

pub(crate) fn rt() -> &'static tokio::runtime::Runtime {
    &*RT
}

static INIT_LOGGER: Once = Once::new();

#[uniffi::export]
pub fn init_logger(logger: Arc<dyn Logger>) {
    INIT_LOGGER.call_once(|| {
        let subscriber = Registry::default().with(FFILogLayer(logger));
        tracing::subscriber::set_global_default(subscriber)
            .expect("failed to set global log subscriber");
    });
}

pub struct Auth2 {
    pub db: Arc<dyn Database>,
    config: Config,
}

impl Auth2 {
    pub async fn new(config: Config) -> Result<Arc<Self>, Error> {
        let db = Db::new(config.database_url.clone())?;
        Ok(Arc::new(Self { db, config }))
    }

    pub async fn db_is_migration_available(&self) -> Result<bool, Error> {
        Ok(self.db.is_migration_available().await?)
    }

    pub async fn db_run_migration(&self) -> Result<(), Error> {
        Ok(self.db.run_migration().await?)
    }

    pub async fn db_reset(&self) -> Result<(), Error> {
        Ok(self.db.reset_database().await?)
    }

    pub async fn add_token_from_url(&self, url: String) -> Result<TokenDetail, Error> {
        let Some(user_key) = self.config.key_store.get() else {
            tracing::error!("no user_key found");
            return Err(Error::InternalError("no user key found".into()));
        };

        let totp = TOTP::from_url_unchecked(&url).map_err(anyhow::Error::from)?;
        let secret = totp.get_secret_base32();
        let secret = encrypt_secret(user_key, secret)?;

        let data = TokenData {
            account: totp.account_name,
            service: totp.issuer,
            secret,
            algorithm: match totp.algorithm {
                totp_rs::Algorithm::SHA1 => db::tokens::TokenAlg::Sha1,
                totp_rs::Algorithm::SHA256 => db::tokens::TokenAlg::Sha256,
                totp_rs::Algorithm::SHA512 => db::tokens::TokenAlg::Sha512,
            },
            digits: totp.digits as u8,
            period: totp.step as u32,
        };

        let id = self.db.add_token(data).await?;
        let Some(token) = self.db.token_detail(id).await? else {
            return Err(anyhow!("token not found").into());
        };

        Ok(token.into())
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
        let Some(user_key) = self.config.key_store.get() else {
            tracing::error!("no user_key found");
            return Err(Error::InternalError("no user key found".into()));
        };

        let secret = encrypt_secret(user_key, secret)?;

        let mut data = TokenData {
            account,
            service,
            secret,
            ..Default::default()
        };
        if let Some(alg) = algorithm {
            data.algorithm = match alg {
                TokenAlg::Sha1 => db::tokens::TokenAlg::Sha1,
                TokenAlg::Sha256 => db::tokens::TokenAlg::Sha256,
                TokenAlg::Sha512 => db::tokens::TokenAlg::Sha512,
            };
        }
        if let Some(digits) = digits {
            data.digits = digits;
        }
        if let Some(period) = period {
            data.period = period;
        }

        let id = self.db.add_token(data).await?;
        let Some(token) = self.db.token_detail(id).await? else {
            return Err(anyhow!("token not found").into());
        };

        Ok(token.into())
    }

    pub async fn remove_token(&self, id: u64) -> Result<(), Error> {
        self.db.remove_token(id).await
    }

    pub async fn list_tokens(&self) -> Result<Vec<Token>, Error> {
        Ok(self
            .db
            .list_tokens()
            .await?
            .into_iter()
            .map(Token::from)
            .collect())
    }

    pub async fn token_detail(&self, id: u64) -> Result<Option<TokenDetail>, Error> {
        Ok(self.db.token_detail(id).await?.map(TokenDetail::from))
    }

    pub async fn generate_current(&self, id: u64) -> Result<TokenResult, Error> {
        let Some(token) = self.db.token_detail(id).await? else {
            return Err(Error::InternalError("no entry found".into()));
        };

        let Some(user_key) = self.config.key_store.get() else {
            tracing::error!("no user_key found");
            return Err(Error::InternalError("no user key found".into()));
        };

        let secret = decrypt_secret(user_key, token.data.secret)?;
        let secret = Secret::Encoded(secret)
            .to_bytes()
            .map_err(anyhow::Error::from)?;

        let totp = TOTP::new_unchecked(
            match token.data.algorithm {
                db::tokens::TokenAlg::Sha1 => totp_rs::Algorithm::SHA1,
                db::tokens::TokenAlg::Sha256 => totp_rs::Algorithm::SHA256,
                db::tokens::TokenAlg::Sha512 => totp_rs::Algorithm::SHA512,
            },
            token.data.digits as usize,
            1,
            token.data.period as u64,
            secret,
            token.data.service,
            token.data.account,
        );

        let current = totp.generate_current().map_err(anyhow::Error::from)?;

        let ts = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map_err(anyhow::Error::from)?
            .as_secs();
        let next = totp.next_step_current().map_err(anyhow::Error::from)?;

        Ok(TokenResult {
            current,
            expires: (next - ts) as u32,
        })
    }
}
