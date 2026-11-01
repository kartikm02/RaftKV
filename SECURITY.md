# Security

The current program has no network endpoint and requires no credentials. Do not commit credentials, environment files, private keys, keystores, data directories, or benchmark data containing sensitive values. `.gitignore` includes common secret and data paths but is not a substitute for reviewing staged changes. If a secret is committed, revoke or rotate it immediately; removing a later commit does not invalidate it.

A future local deployment must bind exposed ports to loopback by default, isolate node traffic, avoid baked-in credentials and image secrets, and document authentication and encryption limits. A future AWS deployment needs separately designed IAM, secrets management, TLS, network controls, and monitoring before it can be considered secure.
