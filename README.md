# gcc188-backend

Backend (API REST) do sistema de gestão da cooperativa de transportes, conforme o
*Levantamento de Requisitos* e o *Design*: Java 25, Spring Boot 4, Spring Data JPA/Hibernate,
Flyway, PostgreSQL, Spring Security com JWT em cookies (Nimbus JOSE).

> **Estado:** compilação verificada localmente com Java 25 + Gradle 9.1.0 (`compileJava` e
> `compileTestJava` verdes; há nota de deprecação pontual em `ApiExceptionHandler`). Os
> testes de integração (Testcontainers) rodam no CI, que tem Docker. Rode `./gradlew build`.

## Como rodar

```bash
docker run -d --name pg -e POSTGRES_DB=cooperativa -e POSTGRES_USER=cooperativa \
  -e POSTGRES_PASSWORD=cooperativa -p 5432:5432 postgres:17
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

O perfil `dev` cria o administrador `admin@cooperativa.local` / `Troque-me-agora-123`
(troca de senha obrigatória no primeiro acesso) e usa cookies sem `Secure`.
Em produção: `app.auth.jwt-secret` (≥ 32 bytes), `app.bootstrap.admin.*`, `spring.datasource.*`
como *secrets* em `/run/secrets/` (configtree) ou variáveis de ambiente.
Actuator na porta 8081 (não publicar).

## Estrutura (Design, seção 5)

Pacotes por módulo, com camadas internas: `br.ufla.cooperativa.{shared,auth,usuarios,cooperandos,clientes,contratos,pagamentos,despesas,relatorios}`
e, em cada um, `dto/ service/ web/ persistence/`. Enumerações do domínio ficam na raiz do módulo.
`web` nunca depende de `persistence` (regra A1, verificada por ArchUnit); os serviços devolvem DTOs.

* Eventos síncronos: `ContratoAprovado` gera o cronograma; `ContratoCancelado` cancela PREVISTO;
  `SessoesInvalidadas` revoga refresh tokens.
* Portas: `PendenciasFinanceiras` (encerramento, RN06) e `PagamentosDoContrato` (detalhe do contrato).
* Escopo (RN10): `EscopoDoUsuario` gera a `Specification`; fora do escopo → 404.
* Erros: `application/problem+json` (RFC 9457) com `campos_invalidos`, `codigo_regra`, `correlation_id`.
* Autenticação: `ACCESS_TOKEN` (JWT, 15 min), `REFRESH_TOKEN` (rotativo, 7 dias, hash SHA-256 no banco,
  reuso revoga a família), `XSRF-TOKEN` + `X-XSRF-TOKEN`. Bloqueio após 5 falhas por 15 min.

## Desvios e decisões a confirmar

1. **Contrato (D06/D07):** o pacote Maven do repositório `gcc188-api-contract` não existe ainda;
   DTOs e controladores foram escritos à mão (JSON em `snake_case`). Ao publicar o contrato,
   trocar os DTOs pelas interfaces geradas.
2. **MapStruct (D12)** não usado: mapeamentos manuais (`paraResponse`). **Lombok** adicionado para entidades.
3. **Spotless/Checkstyle** fora do build; **JaCoCo** só gera relatório (o limite de 70% do RNF-11
   deve ser ligado quando a suíte crescer).
4. **`cpf` e `token_hash`** são `VARCHAR` (não `CHAR`), para o `ddl-auto=validate` do Hibernate.
5. **A4 (ArchUnit)** não aplicada: entidades de módulos diferentes se associam por JPA
   (pagamento→contrato, despesa→cooperando/contrato), como as chaves estrangeiras exigem.
6. **RN03 × parcela:** a taxa só é comparada ao bruto de cada parcela ao *submeter* (e aprovar),
   pois CA-02 parte de um RASCUNHO já existente com taxa inválida.
7. Quem elabora o contrato não o aprova (RN12): responde 422 `RN12` (ex.: administrador que criou).
8. Totais de despesas (RF-DSP-05) agregados em memória; relatórios usam JPQL com projeções.
9. `login` é isento de CSRF; `refresh` e demais `POST` exigem `X-XSRF-TOKEN`.
10. Versão do Spring Boot fixada em `4.0.0` no catálogo (PD12): ajustar para a linha estável vigente.
11. Não implementados: Schemathesis no CI, atualização de migrações sobre a última *tag*, k6.

## Testes

`./gradlew test` — unitários (taxa, parcelamento, estados, documentos), ArquiteturaTest (A1, A2, A3, A5, A6)
e `FluxoContratoIntegrationTest` (PostgreSQL via Testcontainers; requer Docker): CA-01, CA-04, CA-05, CA-11.
