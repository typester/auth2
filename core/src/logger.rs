use std::{collections::HashMap, fmt::Debug, sync::Arc};

use tracing::Subscriber;
use tracing_subscriber::Layer;

#[uniffi::export(with_foreign)]
pub trait Logger: Send + Sync + Debug {
    fn log(&self, msg: String);
}

pub struct FFILogLayer(pub Arc<dyn Logger>);

impl<S: Subscriber> Layer<S> for FFILogLayer {
    fn on_event(
        &self,
        event: &tracing::Event<'_>,
        _ctx: tracing_subscriber::layer::Context<'_, S>,
    ) {
        let mut fields = HashMap::new();
        let mut visitor = LogVisitor(&mut fields);
        event.record(&mut visitor);

        let message: String = match fields.remove("message") {
            Some(message) => message.to_string(),
            None => "".to_string(),
        };
        fields.remove("name");
        self.0.log(format!(
            "{}: {}",
            message,
            serde_json::to_string(&fields).unwrap()
        ));
    }
}

struct LogVisitor<'a>(&'a mut HashMap<String, serde_json::Value>);

impl<'a> tracing::field::Visit for LogVisitor<'a> {
    fn record_f64(&mut self, field: &tracing::field::Field, value: f64) {
        self.0.insert(field.name().into(), serde_json::json!(value));
    }

    fn record_i64(&mut self, field: &tracing::field::Field, value: i64) {
        self.0.insert(field.name().into(), serde_json::json!(value));
    }

    fn record_u64(&mut self, field: &tracing::field::Field, value: u64) {
        self.0.insert(field.name().into(), serde_json::json!(value));
    }

    fn record_i128(&mut self, field: &tracing::field::Field, value: i128) {
        self.0.insert(field.name().into(), serde_json::json!(value));
    }

    fn record_u128(&mut self, field: &tracing::field::Field, value: u128) {
        self.0.insert(field.name().into(), serde_json::json!(value));
    }

    fn record_bool(&mut self, field: &tracing::field::Field, value: bool) {
        self.0.insert(field.name().into(), serde_json::json!(value));
    }

    fn record_str(&mut self, field: &tracing::field::Field, value: &str) {
        self.0.insert(field.name().into(), serde_json::json!(value));
    }

    fn record_error(
        &mut self,
        field: &tracing::field::Field,
        value: &(dyn std::error::Error + 'static),
    ) {
        self.0.insert(
            field.name().into(),
            serde_json::json!(format!("error: {}", value)),
        );
    }

    fn record_debug(&mut self, field: &tracing::field::Field, value: &dyn std::fmt::Debug) {
        self.0.insert(
            field.name().into(),
            serde_json::json!(format!("{:?}", value)),
        );
    }
}
