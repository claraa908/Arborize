# Requisitos — Dashboard Gerencial e Metas (RF004)

## Objetivo
Disponibilizar um painel de indicadores (dashboard) centralizado e responsivo para os gestores da AMMA, exibindo em tempo real as métricas operacionais do viveiro (volume de mudas doadas/plantadas, estoque atual, garrafas PET arrecadadas, substituições de Nim Indiano) e o acompanhamento das metas de plantio e substituição segmentadas por zoneamento urbano e período, com suporte à exportação de relatórios.

## Histórias de usuário

### HU01 — Visualizar métricas operacionais em tempo real
Como gestor da AMMA,
quero visualizar um dashboard centralizado com cartões de resumo contendo o volume de mudas doadas/plantadas, estoque atual do viveiro, garrafas PET arrecadadas e substituições de Nim Indiano,
para acompanhar os indicadores operacionais do viveiro de forma rápida e visual.

### HU02 — Exportar Relatório de Impacto Ambiental
Como gestor da AMMA,
quero clicar no botão "Exportar Relatório" para abrir um modal de geração de relatórios onde posso definir o período (data inicial e final), selecionar os dados desejados (Mudas Doadas, Balanço de Recicláveis, Laudos Emitidos) e escolher o formato (PDF Oficial ou Excel/CSV),
para obter documentação oficial para prestação de contas.

### HU03 — Acompanhar e filtrar metas por zoneamento urbano e período
Como gestor da AMMA,
quero visualizar um painel de acompanhamento por zona contendo gráficos de progresso e uma tabela detalhada com as colunas Bairro, Meta de Plantio, Realizado, Saldo Líquido e Prioridade, filtráveis por "Bairro/Zona" e "Ano/Trimestre",
para monitorar o cumprimento das metas estratégicas da autarquia.

## Regras de negócio

- **RN01:** Os cálculos de garrafas PET arrecadadas e mudas doadas devem refletir fielmente as transações confirmadas no módulo de Viveiro e Eco Ponto.
- **RN02:** As métricas de substituição de Nim Indiano devem consolidar as árvores invasoras removidas e substituídas por espécies nativas da Caatinga.
- **RN03:** O saldo líquido exibido na tabela de acompanhamento por zona deve ser calculado estritamente pela fórmula: `saldo líquido = plantios realizados - supressões autorizadas na zona`.
- **RN04:** A prioridade exibida na tabela deve ser derivada do déficit percentual entre a Meta de Plantio e o Realizado na respectiva zona: Alta acima de 50%, Média acima de 20% até 50% e Baixa até 20%. Quando não houver meta positiva, exibir "Sem meta".

## Critérios de aceitação

### Cenário 1 — Carga de métricas operacionais na inicialização
**Dado que** o gestor da AMMA navega para a rota do Dashboard Gerencial (`/dashboard` ou equivalente)
**Quando** a tela é renderizada
**Então** a store correspondente deve acionar o serviço para buscar os dados consolidados do back-end, exibindo estados de carregamento (spinner/skeleton) e, em seguida, preenchendo os 4 cards principais e os gráficos de evolução e espécies sem erros.

### Cenário 2 — Filtragem de metas por zoneamento e período
**Dado que** o painel de acompanhamento por zona está visível
**Quando** o gestor altera o select "Bairro/Zona" (ex: "Zona Centro") ou o "Ano/Trimestre"
**Então** os dados visuais (barras de progresso e linhas da tabela) devem ser atualizados instantaneamente para refletir apenas os registros daquele escopo.

### Cenário 3 — Validação e submissão do modal de exportação
**Dado que** o modal "Gerar Relatório de Impacto Ambiental" está aberto
**Quando** o gestor preenche a data inicial e final (validando que a final não seja anterior à inicial), marca os dados desejados, seleciona o formato (PDF/CSV/XLSX) e clica em "Gerar Relatório"
**Então** o sistema deve disparar a requisição correspondente, exibir o feedback de sucesso via toast e iniciar o download do arquivo nos formatos PDF, CSV ou XLSX.

## Restrições

- **Back-end:** As agregações de dados devem ser feitas preferencialmente via métodos de repositório otimizados com Spring Data JPA em `backend/arborize/src/main/java/br/com/arborize/repositories`, retornando DTOs em `record` sem expor entidades JPA diretamente no contrato HTTP.
- **Front-end:** A interface deve seguir rigorosamente o design system e a paleta de cores definidos em `frontend/src/assets/main.css` (utilizando classes como `amma-green`, `amma-green-dark`, `amma-bg`, etc.), aplicando os tokens de arredondamento oficiais (`rounded-box`, `rounded-btn`) e garantindo responsividade em desktop e mobile.
- **Referências Visuais:** A estrutura de layout, os componentes de modais, calendários e tabelas devem ser construídos estritamente com base nos mockups salvos na pasta `docs/` do projeto.
- **Fonte de laudos:** Enquanto não existir uma fonte de dados do módulo de Laudos, a opção "Laudos Emitidos" permanece desabilitada na interface e é rejeitada pela API, sem contabilização fictícia.