#[derive(Debug, uniffi::Error, thiserror::Error)]
pub enum Error {
    #[error("internal error: {0}")]
    InternalError(String),

    #[error("database migrate error: {0}")]
    MigrationError(String),

    #[error("data decryption error")]
    DecryptError,
}

impl From<uniffi::UnexpectedUniFFICallbackError> for Error {
    fn from(value: uniffi::UnexpectedUniFFICallbackError) -> Self {
        Self::InternalError(value.to_string())
    }
}

impl From<tokio::task::JoinError> for Error {
    fn from(value: tokio::task::JoinError) -> Self {
        Self::InternalError(value.to_string())
    }
}

impl From<anyhow::Error> for Error {
    fn from(value: anyhow::Error) -> Self {
        Self::InternalError(value.to_string())
    }
}

impl From<sqlx::Error> for Error {
    fn from(value: sqlx::Error) -> Self {
        Self::InternalError(value.to_string())
    }
}

impl From<serde_json::Error> for Error {
    fn from(value: serde_json::Error) -> Self {
        Self::InternalError(value.to_string())
    }
}
