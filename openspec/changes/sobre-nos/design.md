# Design

## Context

Veja `proposal.md` para motivação e escopo.

Estado atual relevante do repositório:
- **Navegação pública pronta:** `TopBar.vue` já possui `{ label: 'Sobre Nós', routeName: 'sobre' }`, faltando apenas registrar a rota `/sobre` (`name: 'sobre'`) em `frontend/src/router/index.ts`.
- **Segurança no backend:** `SecurityConfig.java` já libera `/api/**` com `permitAll()` (exceto rotas explicitamente restritas antes dessa regra, como `/api/materials/admin/**`, `/api/termos-compensacao/**` e `/api/arvores/**`). Portanto, `GET /api/sobre/indicadores` já é naturalmente público sem enfraquecer nenhuma regra de segurança existente.
- **Dados já existentes no PostgreSQL (sem novas tabelas nem migrations Flyway):**
  - `EspecieRepository.sumMudasDisponiveis()` já calcula a soma do estoque atual do viveiro (`mudas_disponiveis`).
  - `MovimentacaoMuda` registra saídas físicas de mudas com `TipoMovimentacaoMuda.SAIDA_PLANTIO` (e quantidade inteira).
  - `SolicitacaoMuda` registra `total_recicaveis` (garrafas PET / recicláveis convertidos).
  - `SolicitacaoSubstituicao` registra `status` com `SubstituicaoBotanicaStatus.CONCLUIDA` para substituições concluídas de espécies invasoras (Nim Indiano).

## Goals / Non-Goals

**Goals:**
- Entregar uma página `/sobre` visualmente refinada, responsiva (mobile-first) e fiel à paleta/tokens de `docs/padroes-de-projeto.md` e à referência visual aprovada pelo usuário.
- Alimentar os 4 indicadores de impacto com dados reais do PostgreSQL via endpoint REST limpo em camadas (`Controller -> Service -> Repository`).
- Garantir resiliência na UI: falha no endpoint de indicadores degrada apenas a seção de indicadores (com botão "Tentar novamente"), mantendo todo o conteúdo institucional disponível.

**Non-Goals:**
- Criar novas tabelas, colunas ou migrations Flyway.
- Alterar o painel administrativo, o `DashboardService` interno ou a `HomeView.vue`.
- Introduzir formulário de contato/envio de e-mail.

## Decisions

### 1. Endpoint REST e contrato DTO (`GET /api/sobre/indicadores`)
- **Decisão:** Criar `SobreController` (`@RestController`, `@RequestMapping("/api/sobre")`) com `@GetMapping("/indicadores")` retornando `ResponseEntity<SobreIndicadoresResponseDTO>`, onde `SobreIndicadoresResponseDTO` é um Java `record`:
  ```java
  public record SobreIndicadoresResponseDTO(
      long mudasDoadas,
      long estoqueViveiro,
      long garrafasPetArrecadadas,
      long ninsSubstituidos
  ) {}
  ```
- **Justificativa:** Segue o padrão mais recente documentado em `docs/padroes-de-projeto.md` (prefixo `/api`, DTO como `record`, controller fino delegando ao service, somente leitura).
- **Alternativa descartada:** Reutilizar `/api/dashboard/overview` — descartado porque `/api/dashboard` é voltado ao painel gerencial interno e não agrega `garrafasPetArrecadadas` nem `ninsSubstituidos`.

### 2. Origem exata das 4 agregações no `SobreIndicadoresService`
- **Decisão:** Consultar com `@Transactional(readOnly = true)`:
  - `estoqueViveiro` → `especieRepository.sumMudasDisponiveis()` (já existente).
  - `mudasDoadas` → `movimentacaoMudaRepository.somarQuantidadePorTipo(TipoMovimentacaoMuda.SAIDA_PLANTIO)` (nova `@Query` com `coalesce(sum(m.quantidade), 0)`).
  - `garrafasPetArrecadadas` → `solicitacaoMudaRepository.somarTotalRecicaveis()` (nova `@Query` com `coalesce(sum(s.totalRecicaveis), 0)`).
  - `ninsSubstituidos` → `solicitacaoSubstituicaoRepository.countByStatus(SubstituicaoBotanicaStatus.CONCLUIDA)` (método derivado Spring Data JPA).
- **Justificativa:** Consultas agregadas parametrizadas no banco (evitam carregar entidades em memória), sempre retornam `>= 0` (via `coalesce`), zero risco de NPE e zero alteração de schema.

### 3. Camada de serviço no Frontend (sem store global desnecessária)
- **Decisão:** Criar `frontend/src/types/sobre.ts` e `frontend/src/services/sobreService.ts` (`getIndicadores()`) consumidos diretamente pela `SobreNosView.vue` com estado local (`indicadores`, `isLoading`, `error`).
- **Justificativa:** Conforme `docs/padroes-de-projeto.md`, Pinia é indicado quando o estado atravessa múltiplas rotas/componentes. Os indicadores de `/sobre` pertencem exclusivamente a essa view, então estado local + service tipado mantém o código mais simples e desacoplado.

### 4. Refinamento visual da `SobreNosView.vue`
- **Decisão:** Organizar `frontend/src/views/public/SobreNosView.vue` em 4 blocos alinhados à referência aprovada:
  1. **Hero institucional (`rounded-box`):** gradiente botânico suave (`from-amma-green-lighter via-emerald-50/70 to-[#EEF4DE]`), borda `amma-green-light`, eyebrow `SOBRE NÓS`, título `Autarquia Municipal do Meio Ambiente de Quixadá` e pílula translúcida com subtítulo.
  2. **3 pilares (`grid-cols-1 md:grid-cols-3 gap-5`):** *Nossa missão*, *O que fazemos* e *Parceiros*, com ícones Material Symbols em badges arredondados `bg-amma-green-lighter text-amma-green`.
  3. **4 indicadores (`grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5`):** *Mudas doadas*, *Estoque do viveiro*, *Garrafas PET* e *Nins substituídos*, formatados via `Intl.NumberFormat('pt-BR')` e com barra visual proporcional discreta (mínimo visual de 8% quando valor > 0 e 0% quando 0, evitando barra enganosa quando zerado).
  4. **Bloco "Fale com a AMMA" (`rounded-box`):** informações de atendimento presencial em Quixadá/CE + botões de atalho (`RouterLink`) para `arborizacao`, `eventos-publico` e `biblioteca-catalogo`.

## Risks / Trade-offs

- **[Banco local ou ambiente novo com poucos registros]** Em um ambiente recém-subido, os indicadores podem retornar `0` → **Mitigação:** exibir `0` de forma limpa e legível com barra em `0%`, sem mensagens de erro (zero é um estado válido de dados reais).
- **[Indisponibilidade momentânea da API]** Se o backend estiver fora do ar, a página institucional não deve quebrar → **Mitigação:** isolar o estado de carregamento e erro exclusivamente na seção de indicadores, mantendo os blocos institucionais e os atalhos totalmente funcionais.
