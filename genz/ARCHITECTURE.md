# Backend architecture

The Java source uses a feature-first package structure. Code that changes for the
same business capability lives together instead of being spread across global
`controller`, `service`, `repository`, `entity`, and `dto` packages.

```text
com.example.genz
|-- auth
|-- challenge
|-- common
|   |-- api
|   `-- exception
|-- community
|   |-- chat
|   |-- friendship
|   |-- notification
|   `-- post
|-- config
|-- goal
|-- invitation
|-- learning
|-- notification
|-- qrcode
|-- reward
|-- security
|-- settings
|-- transaction
`-- wallet
```

## Feature layout

A feature contains only the layers it needs:

```text
feature
|-- controller   HTTP or WebSocket entry points
|-- dto          API request and response models
|-- entity       Persistence models owned by the feature
|-- enums        Domain values owned by the feature
|-- mapper       Entity/DTO conversion
|-- repository   Persistence access
`-- service      Business logic
```

Some features are intentionally smaller. For example, `learning` and `settings`
currently contain persistence models, while `qrcode` contains API and service
code. Cross-cutting application configuration remains in `config`, authentication
infrastructure remains in `security`, and reusable API error handling lives in
`common.exception`.

## Adding code

1. Add a class to the package for the business feature it supports.
2. Create a layer subpackage only when the feature needs that layer.
3. Keep feature-owned enums and DTOs with their feature.
4. Put code in `common` only when it is genuinely shared by multiple features.
5. Mirror the feature package under `src/test/java` for feature tests.
