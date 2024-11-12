use aes_gcm::{aead::Aead, Aes256Gcm, Key, KeyInit, Nonce};
use anyhow::anyhow;
use base64::Engine;
use rand::Rng;
use sha2::Sha256;

use crate::error::Error;

fn generate_iv() -> [u8; 12] {
    let mut iv = [0; 12];
    rand::thread_rng().fill(&mut iv);
    iv
}

fn derive_aes_key(user_key: &[u8], salt: &[u8]) -> [u8; 32] {
    let mut key = [0; 32];
    pbkdf2::pbkdf2_hmac::<Sha256>(user_key, salt, 10_000, &mut key);
    key
}

pub fn encrypt_secret(user_key: String, secret: String) -> anyhow::Result<String> {
    let salt = generate_iv();
    let encrypt_key = derive_aes_key(user_key.as_bytes(), &salt);
    let encrypt_key = Key::<Aes256Gcm>::from_slice(&encrypt_key);

    let iv = generate_iv();
    let nonce = Nonce::from_slice(&iv);

    let cipher = Aes256Gcm::new(encrypt_key);
    let encrypted = cipher
        .encrypt(nonce, secret.as_bytes())
        .map_err(|e| anyhow!(e))?;

    let mut result = Vec::with_capacity(12 + 12 + encrypted.len());
    result.extend_from_slice(&iv);
    result.extend_from_slice(&salt);
    result.extend_from_slice(&encrypted);

    Ok(base64::engine::general_purpose::STANDARD.encode(&result))
}

pub fn decrypt_secret(user_key: String, encrypted: String) -> Result<String, Error> {
    let encrypted = base64::engine::general_purpose::STANDARD
        .decode(encrypted)
        .map_err(|e| anyhow!(e))?;

    let iv = &encrypted[0..12];
    let salt = &encrypted[12..24];
    let encrypted = &encrypted[24..];

    let decrypt_key = derive_aes_key(user_key.as_bytes(), salt);
    let decrypt_key = Key::<Aes256Gcm>::from_slice(&decrypt_key);

    let nonce = Nonce::from_slice(iv);
    let cipher = Aes256Gcm::new(decrypt_key);

    let decrypted = cipher
        .decrypt(nonce, encrypted)
        .map_err(|_| Error::DecryptError)?;
    Ok(String::from_utf8(decrypted).map_err(anyhow::Error::from)?)
}

#[cfg(test)]
mod tests {
    use crate::{enc::decrypt_secret, error::Error};

    use super::encrypt_secret;

    #[test]
    fn test_encrypt() {
        let data = encrypt_secret("test".into(), "secret".into()).unwrap();
        println!("encrypted: {}", data);
        let res = decrypt_secret("test".into(), data.clone()).unwrap();
        assert_eq!(res, "secret");

        let res = decrypt_secret("test?".into(), data.clone());
        match res {
            Err(Error::DecryptError) => (),
            _ => {
                panic!("unexpected result: {:?}", res);
            }
        }
    }
}
