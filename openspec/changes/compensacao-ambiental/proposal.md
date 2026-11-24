# Proposal

## Why

A AMMA precisa registrar os termos de compromisso de compensação ambiental de empreendimentos e acompanhar, em um único fluxo interno, o que foi acordado e o que já foi entregue. A Sprint 3 prioriza o `RF017` / `Épico 06`, e as referências visuais apresentam listagem/acompanhamento, cadastro de termo e consulta/edição com recibos.

## What Changes

- Criar um painel administrativo exclusivo para funcionários da AMMA cadastrarem o CNPJ e a razão social da empresa, anexarem o Termo de Compromisso assinado em PDF e registrarem o quantitativo de mudas acordado.
- Disponibilizar listagem e consulta dos acordos, exibindo quantitativos acordados e entregues e os estados *Em aberto*, *Parcialmente doado* e *Totalmente doado/Fechado*.
- Permitir o registro acumulativo de parcelas/entregas de mudas com recibos apresentados à autarquia, atualizar o estado com base no total comprovado e permitir ajuste manual do estado por perfil autorizado da AMMA para acordos que considerem outros insumos.
- Restringir o fluxo ao perfil interno autorizado da AMMA, sem exposição na área pública do cidadão.

## Capabilities

### New Capabilities
- `compensacao-ambiental`: cadastro, consulta, registro acumulativo de entregas/recibos, gestão de estado e acompanhamento interno de termos de compensação ambiental.

### Modified Capabilities
- Nenhuma. Não há spec anterior de compensação ambiental no repositório.

## Impact

- Frontend: nova área autenticada de compensação ambiental (listagem/acompanhamento, formulário de cadastro e consulta/edição/entregas), usando Vue 3 (Composition API e `<script setup lang="ts">`), TypeScript, Tailwind CSS e Pinia conforme os padrões do projeto.
- Backend: novos endpoints REST e camadas `Controller` / `Service` / `Repository`, com validação `jakarta.validation`, controle de acesso por perfil autorizado e respostas de erro padronizadas com `ProblemDetail`.
- Persistência e arquivos: PostgreSQL para termos, quantitativos, histórico de entregas e metadados/referências; MinIO para armazenamento seguro de PDFs de termos e recibos (PDF ou imagem).
- Escopo e paralelismo: não altera módulos nem contratos de outras specs da Sprint 3. Itens de baixa prioridade (como doações voluntárias de cidadãos, plantio particular, adoção/certificados, alertas automáticos ou cronograma de manutenção) ficam fora desta mudança.

## Dependências e lacunas

- RF017 / Épico 06 foi confirmado pelo trecho fornecido pelo usuário: razão social, CNPJ, PDF assinado, mudas acordadas/quitadas e acompanhamento por recibos. Campos opcionais e limite conservador de recibos estão discriminados como decisões/lacunas no design.
- As telas exportadas em `openspec/docs/interfaces/spec-03/` foram consultadas como referência visual inicial; confirmar detalhes visuais, campos, breakpoints e fluxos com o Figma caso haja acesso adicional disponível.
- RF017 confirma CNPJ, razão social, PDF assinado e quantitativos de mudas acordados versus quitados por recibos. Número do termo, data de assinatura e prazo aparecem no mockup, mas não no texto fornecido do requisito; confirmar sua obrigatoriedade antes de tratá-los como regra de negócio.
- RF017 estabelece acompanhamento com base nos recibos. Por decisão explícita do usuário, o estado também pode ser ajustado por perfil AMMA autorizado para refletir acordos que envolvam outros insumos; uma nova parcela de mudas recalcula o estado a partir do total comprovado.
- Limites de arquivo: a referência visual indica até 10 MB para o termo e aceita PDF/PNG/JPG nos recibos, sem especificar tamanho para recibos. A política definida para esta entrega deve ser documentada na spec/design.
- A atomicidade entre persistência no PostgreSQL e upload/remoção de arquivos no MinIO (evitando registros incompletos ou objetos órfãos em caso de falha) e a proteção efetiva das rotas no backend devem ser verificadas e aplicadas na implementação.
