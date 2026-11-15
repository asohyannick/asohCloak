ARG KC_VERSION=26.8.0
FROM quay.io/keycloak/keycloak:${KC_VERSION} AS builder
ENV KC_DB=postgres \
    KC_CACHE=local \
    KC_HEALTH_ENABLED=false \
    KC_METRICS_ENABLED=false \
    KC_FEATURES=token-exchange:v1,admin-fine-grained-authz:v1
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:${KC_VERSION}
COPY --from=builder /opt/keycloak/ /opt/keycloak/
ENV KC_DB=postgres \
    KC_CACHE=local \
    KC_HTTP_ENABLED=true \
    KC_PROXY_HEADERS=xforwarded \
    KC_HOSTNAME_STRICT=false \
    KC_DB_POOL_INITIAL_SIZE=1 \
    KC_DB_POOL_MIN_SIZE=1 \
    KC_DB_POOL_MAX_SIZE=4 \
    JAVA_OPTS_KC_HEAP="-Xms64m -Xmx200m -XX:MaxMetaspaceSize=120m" \
    JAVA_OPTS_APPEND="-XX:+UseSerialGC -Xss512k -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=48m"
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]