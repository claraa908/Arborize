# Design — Dashboard Gerencial e Metas (RF004)

## Objetivo da experiência

Prover ao gestor da AMMA uma visualização centralizada e em tempo real das métricas operacionais do viveiro (estoque, doações, recicláveis, substituições) e do acompanhamento das metas de plantio e supressão segmentadas por zoneamento urbano, com suporte à exportação de relatórios.

## Histórias atendidas

- **HU01:** O design prevê a exibição centralizada de métricas operacionais em tempo real por meio de cartões de resumo e gráficos analíticos.
- **HU02:** O design provê a interface de um modal interativo para exportação personalizada de relatórios de impacto ambiental.
- **HU03:** O design inclui filtros de zoneamento/período e uma tabela detalhada com cálculo de saldo líquido e indicadores de prioridade.

## Fluxo principal

1. A pessoa acessa a rota do painel gerencial a partir do menu lateral da aplicação (`AppSidebar`).
2. O sistema carrega os dados operacionais e exibe os cards de resumo juntamente com os gráficos de evolução e espécies.
3. A pessoa interage com os seletores de "Bairro/Zona" e "Ano/Trimestre" para refinar o acompanhamento de metas.
4. O sistema atualiza instantaneamente as barras de progresso e a tabela de detalhamento por zona.
5. A pessoa clica no botão "Exportar Relatório".
6. O sistema exibe o modal de exportação, permitindo configurar o período, o escopo dos dados e o formato desejado (PDF/CSV) antes de iniciar o download.

## Estrutura da interface

1. **Painel Superior e Ações**
   - Título da página e botão de ação primária "Exportar Relatório" com ícone.
2. **Cards de Métricas Operacionais**
   - Quatro blocos em grade contendo Mudas Doadas/Plantadas, Estoque Atual do Viveiro, Garrafas PET Arrecadadas e Substituição de Nim Indiano.
3. **Seção de Gráficos Analíticos**
   - Gráfico de Linhas (Evolução de Doações e Plantios) e Gráfico de Rosca (Espécies Predominantes no Viveiro).
4. **Painel de Acompanhamento por Zona**
   - Filtros de Bairro/Zona e Período, barras de progresso percentual e tabela completa com colunas de Meta, Realizado, Saldo Líquido e Prioridade.

## Campos e controles

### Filtro Bairro/Zona
- Tipo: seleção (Dropdown / select estilizado).
- Obrigatório: sim.
- Rótulo: Bairro/Zona.
- Placeholder ou opções: "Todas", "Zona Centro", "Planalto Universitário", etc.
- Validação: Nenhuma restrição complexa além dos valores disponíveis no sistema.
- Comportamento: Recarrega imediatamente os dados de metas correspondentes à zona selecionada.

### Data Inicial / Data Final (Modal de Relatório)
- Tipo: data (Datepicker).
- Obrigatório: sim.
- Rótulo: Data inicial / Data final.
- Placeholder ou opções: `01/01/2026` / `30/09/2026`.
- Validação: A data final não pode ser anterior à data inicial.
- Comportamento: Define o intervalo de registros contemplados no relatório exportado.

### Formato de Exportação (Modal de Relatório)
- Tipo: seleção única (Radio buttons).
- Obrigatório: sim.
- Rótulo: PDF Oficial / CSV / Excel (XLSX).
- Comportamento: Determina o tipo de arquivo gerado ao submeter o modal.

## Definições aplicadas à implementação

- Os cards exibem os totais confirmados acumulados; o gráfico de evolução mostra os últimos 12 meses.
- O período inicial do acompanhamento é o ano e trimestre correntes. As opções de ano e zona são obtidas das metas cadastradas.
- O déficit percentual é `max(0, (meta - realizado) / meta * 100)`. Metas acima de 50% de déficit são Alta; acima de 20% até 50%, Média; até 20%, Baixa. Meta zero é exibida como "Sem meta".
- A opção "Laudos Emitidos" fica desabilitada até haver uma fonte de dados persistida para o módulo.
- O arquivo XLSX é gerado no backend com o suporte ZIP/XML do JDK; PDF usa PDFBox e CSV é UTF-8.

## Estados da interface

- **Inicial:** O dashboard é carregado com os dados globais consolidados e os filtros configurados no padrão inicial.
- **Carregando:** Exibição de elementos visuais de espera (spinners ou skeleton loaders) nas regiões de cartões e tabelas.
- **Sucesso:** Renderização correta das métricas, gráficos e fechamento/download bem-sucedido a partir do modal.
- **Erro:** Disparo de notificação via toast (`feedbackStore`) informando falha de comunicação com o servidor.
- **Vazio:** Exibição de mensagem informativa quando nenhum registro for encontrado para o período ou zona filtrados.

## Responsividade

- **Desktop:** Grade otimizada de 4 colunas para os cartões superiores, gráficos dispostos lado a lado e tabela ocupando a largura total da seção inferior.
- **Mobile:** Os cartões empilham-se verticalmente em colunas reduzidas, os gráficos adaptam-se para exibição em bloco único e a tabela ganha suporte a rolagem horizontal (`overflow-x-auto`).

## Acessibilidade

- Uso de cores de contraste alinhadas ao design system da AMMA, rótulos textuais associados aos campos de formulário, foco visível em elementos interativos e estrutura baseada em tags semânticas.

## Critérios de aceitação

- [ ] Critério 01: Os cartões de indicadores e gráficos refletem corretamente as métricas operacionais consolidadas.
- [ ] Critério 02: A filtragem por zoneamento urbano atualiza as barras de progresso e a tabela de metas em tempo real.
- [ ] Critério 03: O modal de exportação permite selecionar o período, os dados e o formato, acionando o download adequadamente.