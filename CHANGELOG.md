# Changelog

## [0.2.1](https://github.com/gcc188-cooperativa/gcc188-backend/compare/gcc188-backend-v0.2.0...gcc188-backend-v0.2.1) (2026-11-11)


### Bug Fixes

* **ci:** publicar a imagem no mesmo run do release-please (D30) ([#135](https://github.com/gcc188-cooperativa/gcc188-backend/issues/135)) ([27e7d3f](https://github.com/gcc188-cooperativa/gcc188-backend/commit/27e7d3f289ef9ec88ee16bcb2b9b67b6f070188b))

## [0.2.0](https://github.com/gcc188-cooperativa/gcc188-backend/compare/gcc188-backend-v0.1.0...gcc188-backend-v0.2.0) (2026-11-11)


### Features

* **auth:** autenticação JWT, sessões e CSRF (RF-AUT-01 a 03) ([9b494db](https://github.com/gcc188-cooperativa/gcc188-backend/commit/9b494dbbde3c82d10cab907909f109fa03e80b92))
* **clientes:** cadastro e vínculo de clientes (RF-CLI-01 a 04) ([d0114ea](https://github.com/gcc188-cooperativa/gcc188-backend/commit/d0114eaa10deab2bb8d13f9da5372a005ef6e55d))
* **contratos:** ciclo de aprovação e cronograma de pagamentos (RF-CTR-01 a 10) ([84b2486](https://github.com/gcc188-cooperativa/gcc188-backend/commit/84b2486f93b5a62f7a65a1978c95d0f78ad9a984))
* **cooperandos:** cadastro e vínculo de cooperandos (RF-COO-01 a 04) ([8e473af](https://github.com/gcc188-cooperativa/gcc188-backend/commit/8e473af0152924c6f45e4703d5cb937a70539d4d))
* **despesas:** despesas por origem e totalização (RF-DSP-01 a 05) ([f1be183](https://github.com/gcc188-cooperativa/gcc188-backend/commit/f1be1838fa2dafa534e7f20d2018e18203ffa057))
* **pagamentos:** baixa, repasse e estorno (RF-PAG-01 a 05) ([147a73c](https://github.com/gcc188-cooperativa/gcc188-backend/commit/147a73cdcfd49516b04b9b56dfc9be9151acd3c5))
* **relatorios:** extratos, resumo financeiro e inadimplência (RF-REL-01 a 04) ([9f211e4](https://github.com/gcc188-cooperativa/gcc188-backend/commit/9f211e4cf59258ea47a812449c22cf5100017883))
* **shared:** erros RFC 9457, paginação, auditoria e tempo de negócio ([7d84729](https://github.com/gcc188-cooperativa/gcc188-backend/commit/7d847298222b0e6de4e2eac95b2c029c6fd24c53))
* **usuarios:** cadastro de usuários (CRUD, RF-USR-01 a 04) ([b8ebfad](https://github.com/gcc188-cooperativa/gcc188-backend/commit/b8ebfadeecd82098134745a70d55973c82e0a474))


### Bug Fixes

* **ci:** bump da action do Trivy para v0.36.0 ([#1](https://github.com/gcc188-cooperativa/gcc188-backend/issues/1)) ([68ee8c0](https://github.com/gcc188-cooperativa/gcc188-backend/commit/68ee8c07b7e6b6cf156967fe44cf1e551ddaf8bd))


### Performance Improvements

* **db:** índices das consultas principais ([2e88078](https://github.com/gcc188-cooperativa/gcc188-backend/commit/2e88078cba24de515dc9437337c3b0efeb650159))
