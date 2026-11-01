# Tasks — Dashboard Gerencial e Metas (RF004)

## Implementação

- [x] **T01 — Criar/Ajustar Entidades JPA e Repositórios**
  - Implementar ou ajustar as entidades JPA (`MudaTransaction`, `EcoPontoTransaction`, `Supressao`, `Plantio`, `MetaZoneamento`) em `backend/arborize/src/main/java/br/com/arborize/entities` e seus respectivos repositórios em `repositories`.
  - **Relacionada a:** HU01, HU03, RN01, RN02, RN03

- [x] **T02 — Implementar Service de Métricas e Metas**
  - Desenvolver a lógica de agregação e cálculo no back-end (ex: somatório de mudas, contagem de garrafas PET, cálculo do saldo líquido de plantios e supressões por zona) utilizando Spring Data JPA.
  - **Relacionada a:** HU01, HU03, RN01, RN02, RN03

- [x] **T03 — Criar Endpoints REST para o Dashboard e Relatórios**
  - Implementar os controllers em `backend/arborize/src/main/java/br/com/arborize/controllers` expondo as rotas de agregação de indicadores operacionais, filtragem por zoneamento e exportação de relatórios.
  - **Relacionada a:** HU01, HU02, HU03

- [x] **T04 — Desenvolver Componentes de Cards e Gráficos no Front-End**
  - Criar os componentes visuais em `frontend/src` para os cards de resumo (Mudas, Estoque, PET, Substituição de Nim) e os gráficos de evolução/espécies seguindo o design system da AMMA.
  - **Relacionada a:** HU01

- [x] **T05 — Implementar Seção de Acompanhamento por Zona e Filtros**
  - Construir os seletores de "Bairro/Zona" e "Ano/Trimestre", a barra de progresso e a tabela detalhada com Meta, Realizado, Saldo Líquido e Prioridade.
  - **Relacionada a:** HU03, RN03

- [x] **T06 — Implementar Modal e Funcionalidade de Exportação de Relatórios**
  - Desenvolver o modal "Gerar Relatório de Impacto Ambiental" com seleção de datas, checkboxes de inclusão de dados e escolha de formato (PDF / CSV / XLSX). Laudos ficam indisponíveis até o módulo possuir fonte persistida.
  - **Relacionada a:** HU02

## Verificação

- [x] **T07 — Verificar cenário: Atualização de indicadores operacionais**
  - Confirmar se os cards de indicadores operacionais refletem os dados corretos computados pelo back-end ao carregar a página.
  - **Critério relacionado:** Cenário 1

- [x] **T08 — Verificar cenário: Filtragem de metas por zoneamento urbano**
  - Confirmar se a tabela e as barras de progresso são recarregadas corretamente ao alterar os filtros de zona e período.
  - **Critério relacionado:** Cenário 2

- [x] **T09 — Verificar cenário: Exportação de Relatório**
  - Confirmar se o modal abre corretamente, aceita as seleções de parâmetros e inicia o download do arquivo solicitado.
  - **Critério relacionado:** Cenário 3

## Pronto quando

- [x] Todas as histórias de usuário implementáveis com as fontes de dados existentes estão concluídas; Laudos permanece indisponível até existir fonte.
- [x] Os critérios de aceitação de indicadores, filtros e exportação estão cobertos por testes de serviço/controlador e build do frontend.
- [x] O fluxo foi revisado em desktop e mobile.
- [x] Os estados de sucesso e erro são compreensíveis.
- [x] Os controles principais são acessíveis pelo teclado.