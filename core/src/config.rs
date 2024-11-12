use std::{fmt::Debug, sync::Arc};

#[derive(Debug, uniffi::Record)]
pub struct Config {
    pub database_url: String,
    pub key_store: Arc<dyn KeyStore>,
}

#[uniffi::export(with_foreign)]
pub trait KeyStore: Send + Sync + Debug {
    fn get(&self) -> Option<String>;
}
