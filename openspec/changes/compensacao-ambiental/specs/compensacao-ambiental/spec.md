# Spec Delta — Compensação Ambiental (RF017)

## Purpose

Esta capacidade permite a funcionários autorizados da AMMA cadastrar termos de compensação ambiental de empresas, registrar entregas acumuladas de mudas comprovadas por recibos e acompanhar o estado do acordo, calculado pelas entregas e ajustável pela equipe para refletir outros insumos acordados.

## ADDED Requirements

### Requirement: Acesso interno autorizado
O sistema MUST disponibilizar as operações e informações de compensação ambiental somente a usuários autenticados com perfil `FUNCIONARIO_AMMA` ou `ADMINISTRADOR_AMMA`, sem exposição na interface pública do cidadão.

#### Scenario: Usuário AMMA autorizado
- **GIVEN** que o usuário está autenticado e possui perfil `FUNCIONARIO_AMMA` ou `ADMINISTRADOR_AMMA`
- **WHEN** acessa a tela ou uma operação de compensação ambiental
- **THEN** o sistema permite a operação conforme a permissão concedida

#### Scenario: Usuário sem perfil autorizado
- **GIVEN** que o usuário não está autenticado ou não possui perfil interno autorizado da AMMA
- **WHEN** tenta acessar a rota, consultar um termo ou executar operação de escrita diretamente
- **THEN** o sistema nega o acesso sem expor dados de termos ou acordos

### Requirement: Cadastro de termo de compromisso
O sistema MUST permitir que um usuário autorizado da AMMA cadastre CNPJ e razão social da empresa, quantitativo acordado de mudas e o PDF assinado do Termo de Compromisso (até 10 MB, conforme a tela de referência). O quantitativo entregue começa em zero; número do termo, data de assinatura e prazo podem ser capturados conforme a tela, mas não são regras obrigatórias confirmadas no RF017.

#### Scenario: Cadastro válido
- **GIVEN** que o usuário autorizado preenche os campos obrigatórios e anexa um arquivo PDF válido
- **WHEN** confirma o cadastro do termo
- **THEN** o sistema registra a empresa, o termo e seu arquivo PDF e o disponibiliza na consulta com zero mudas entregues e estado Em aberto

#### Scenario: Campo obrigatório ausente ou quantitativo inválido
- **GIVEN** que falta dado obrigatório ou o quantitativo acordado não é um número inteiro positivo
- **WHEN** o usuário tenta salvar o termo
- **THEN** o sistema bloqueia o envio, destaca os campos inválidos e preserva os valores informados

#### Scenario: Arquivo do termo inválido
- **GIVEN** que o arquivo anexado do termo não é um PDF válido ou excede os limites configurados de tamanho/tipo
- **WHEN** o usuário tenta salvar o termo
- **THEN** o sistema rejeita o arquivo com mensagem clara e não salva um termo incompleto

#### Scenario: Falha ao persistir o PDF
- **GIVEN** que ocorre falha ao armazenar o arquivo PDF do termo
- **WHEN** o cadastro é confirmado
- **THEN** o sistema informa a falha, não confirma sucesso e não deixa o termo salvo sem o PDF obrigatório

### Requirement: Consulta e acompanhamento dos acordos
O sistema MUST listar e permitir consultar os termos de compensação ambiental registrados, exibindo empreendimento/empresa, identificação do termo, quantitativos acordado e entregue e o estado do acordo.

#### Scenario: Listagem de termos
- **GIVEN** que existem termos cadastrados
- **WHEN** o usuário interno autorizado abre a tela de compensação ambiental
- **THEN** o sistema exibe os acordos, quantitativos e estados e permite abrir os detalhes de um termo

#### Scenario: Lista sem termos
- **GIVEN** que não há termos cadastrados
- **WHEN** o usuário interno autorizado abre a tela de compensação ambiental
- **THEN** o sistema apresenta estado vazio informativo e o acesso para cadastrar um novo termo

#### Scenario: Falha ao carregar a listagem
- **GIVEN** que a consulta dos termos falha
- **WHEN** o usuário abre ou atualiza a listagem
- **THEN** o sistema apresenta estado de erro com opção de tentar novamente, sem representar a falha como lista vazia

### Requirement: Registro acumulativo de entregas e recibos
O sistema MUST permitir registrar parcelas positivas de mudas vinculadas a um termo, com recibo comprobatório em PDF ou imagem JPG/PNG, acumulando-as ao histórico e total entregue sem substituir comprovantes anteriores. Cada arquivo de recibo tem limite de 2 MB, adotado como limite restritivo consistente com a validação de imagens existente.

#### Scenario: Registro de entrega válida com acúmulo
- **GIVEN** que o usuário autorizado informa uma entrega válida e anexa um recibo comprobatório em PDF ou imagem válido
- **WHEN** registra a entrega no termo
- **THEN** o sistema vincula a nova entrega e o recibo ao termo e soma a quantidade ao total já entregue, preservando as entregas anteriores

#### Scenario: Entrega ou recibo inválido
- **GIVEN** que a quantidade informada é inválida (ausente, não inteira ou não positiva) ou o recibo não é um PDF/imagem válido dentro dos limites configurados
- **WHEN** o usuário tenta registrar a entrega
- **THEN** o sistema rejeita a operação, informa a validação e mantém os registros anteriores intactos

#### Scenario: Falha no armazenamento do recibo
- **GIVEN** que ocorre falha ao armazenar o arquivo do recibo
- **WHEN** o usuário confirma o registro da entrega
- **THEN** o sistema não contabiliza a nova entrega, não altera o total anterior e informa a falha sem exibir sucesso

### Requirement: Gestão do estado do acordo
O sistema MUST permitir que o usuário autorizado da AMMA altere o estado entre Em aberto, Parcialmente doado e Totalmente doado/Fechado e exibir o valor por texto e ícone. O cadastro inicia Em aberto; cada nova parcela de mudas com recibo atualiza o estado pela quantidade acumulada, enquanto a equipe também pode ajustá-lo para refletir o acordo completo, inclusive outros insumos.

#### Scenario: Parcela atualiza o estado automaticamente
- **GIVEN** que o usuário autorizado registra uma nova parcela de mudas com recibo válido
- **WHEN** a entrega acumulada é zero, menor que o acordado ou igual/superior ao acordado
- **THEN** o sistema exibe respectivamente Em aberto, Parcialmente doado ou Totalmente doado/Fechado

#### Scenario: Usuário autorizado ajusta estado do acordo
- **GIVEN** que usuário autorizado da AMMA seleciona um dos três estados válidos
- **WHEN** confirma a atualização do estado
- **THEN** o sistema persiste o estado selecionado e o exibe em texto e ícone

#### Scenario: Nova parcela após ajuste manual
- **GIVEN** que o estado foi ajustado manualmente após a última parcela registrada
- **WHEN** uma nova parcela válida de mudas é registrada
- **THEN** o sistema recalcula o estado usando o total acumulado de mudas comprovadas, sem remover ou substituir o histórico de recibos

#### Scenario: Estado enviado por usuário não autorizado ou inválido
- **GIVEN** que um usuário sem permissão tenta atualizar o estado ou que o valor não pertence aos três estados permitidos
- **WHEN** a requisição de atualização é processada
- **THEN** o sistema rejeita a operação e mantém o estado anterior

### Requirement: Interface acessível e estados de operação
A interface MUST seguir os padrões visuais do Arborize e representar claramente carregamento, vazio, erro, validação e sucesso, sem comunicar estado ou status exclusivamente por cor.

#### Scenario: Operação em progresso
- **GIVEN** que uma consulta, cadastro, atualização de estado ou registro de entrega está em andamento
- **WHEN** a interface aguarda resposta
- **THEN** exibe indicador de carregamento e bloqueia envios duplicados

#### Scenario: Sucesso ou erro em formulário
- **GIVEN** que o usuário envia um formulário válido ou inválido
- **WHEN** a operação conclui com sucesso ou encontra erro/validação
- **THEN** exibe feedback de sucesso via `AppToastContainer` (ou confirmação via `AppConfirmModal` quando aplicável) ou apresenta mensagens de erro claras e associadas, preservando os dados informados em caso de falha

#### Scenario: Uso responsivo e por teclado
- **GIVEN** que o usuário acessa a tela em largura reduzida ou navega por teclado
- **WHEN** percorre campos, botões e tabelas/cards
- **THEN** os controles permanecem acessíveis, com `labels` associados, foco visível e boa leitura
