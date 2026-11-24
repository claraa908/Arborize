# Tasks

## 1. Backend — Endpoint público de indicadores (`GET /api/sobre/indicadores`)

- [x] 1.1 Adicionar as consultas agregadas somente leitura em `MovimentacaoMudaRepository` (`somarQuantidadePorTipo`), `SolicitacaoMudaRepository` (`somarTotalRecicaveis`) e `SolicitacaoSubstituicaoRepository` (`countByStatus`), reutilizando `EspecieRepository.sumMudasDisponiveis()`, e verificar compilação do módulo backend via `mvn test-compile` em `backend/arborize`.
- [x] 1.2 Criar `SobreIndicadoresResponseDTO`, `SobreIndicadoresService` (`@Transactional(readOnly = true)`) e `SobreController` (`GET /api/sobre/indicadores`) seguindo `Controller -> Service -> Repository`, com testes unitários em JUnit/Mockito (`SobreIndicadoresServiceTest` e `SobreControllerTest`) passando via `mvn test -Dtest=SobreIndicadoresServiceTest,SobreControllerTest`.

## 2. Frontend — Tipos, serviço e tela institucional `/sobre`

- [x] 2.1 Criar `frontend/src/types/sobre.ts` e `frontend/src/services/sobreService.ts` (`getIndicadores`) consumindo `GET /api/sobre/indicadores` pela instância Axios compartilhada (`api`), com teste unitário em Vitest validando a chamada e retorno tipado.
- [x] 2.2 Criar `frontend/src/views/public/SobreNosView.vue` com visual refinado baseado na referência aprovada e em `docs/padroes-de-projeto.md` (hero institucional da AMMA, 3 pilares *Nossa missão* / *O que fazemos* / *Parceiros*, 4 indicadores reais com skeleton de carregamento, formatação `pt-BR`, barra proporcional e erro isolado com "Tentar novamente", e bloco *Fale com a AMMA* com atalhos para *Arborização*, *Eventos* e *Biblioteca Digital*), validando todos os estados com testes em Vitest.

## 3. Roteamento público e verificação integrada

- [x] 3.1 Registrar a rota pública `/sobre` (`name: 'sobre'`) sob `PublicLayout` em `frontend/src/router/index.ts` (conectando automaticamente ao item `Sobre Nós` já existente em `TopBar.vue`) e validar navegação e destaque ativo na `TopBar` em testes em Vitest.
- [x] 3.2 Executar os testes do backend (`mvn test -Dtest=SobreIndicadoresServiceTest,SobreControllerTest`), os testes do frontend (`npx vitest run src/__tests__/sobreNosView.spec.ts`) e o build do frontend (`npm run build-only`), confirmando tudo passando sem erros.

## Workflow follow-up

- Revisar visualmente `/sobre` em desktop e mobile com o backend local rodando.
- Integrar na `integration` e arquivar a change no OpenSpec (`openspec archive`) conforme combinado pela equipe.
