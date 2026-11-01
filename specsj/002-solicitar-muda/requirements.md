# Requisitos — Solicitação de Muda (formulário, conversão e acompanhamento)

> Referências: RF006 e RF007 (Essenciais), Épico 03 (história de solicitação e conversão). Depende de `001-assistente-plantio`.
> Padrões técnicos e visuais: `docs/padroes-de-projeto.md`.

## Objetivo

Permitir que o cidadão peça a doação de mudas da espécie escolhida, informando endereço de destino e seus dados, e saiba antes do envio quantas garrafas PET precisará entregar no Eco Ponto.

## Histórias de usuário

### HU01 — Preencher o pedido de muda

Como cidadão,
quero confirmar a espécie, informar a quantidade, o endereço de destino e meus dados,
para fazer um pedido formal de doação de mudas.

### HU02 — Ver a conversão em recicláveis

Como cidadão,
quero ver quantas garrafas PET preciso entregar antes de enviar o pedido,
para decidir com clareza se quero seguir.

### HU03 — Enviar a solicitação

Como cidadão,
quero enviar o pedido e receber um número de solicitação,
para acompanhar o andamento.

### HU04 — Ver o andamento logo após o envio

Como cidadão,
quero ver o resumo do pedido e a próxima etapa,
para saber que devo entregar os recicláveis no Eco Ponto.

## Regras de negócio

- **RN01:** O pedido só pode ser iniciado com uma **espécie selecionada** vinda do assistente (spec 001). Sem espécie, o cidadão é levado de volta ao assistente com a mensagem "Escolha uma espécie para continuar."
- **RN02:** A quantidade de mudas começa em **1** e é um inteiro entre 1 e o máximo configurável (ver "Questões em aberto").
- **RN03:** Campos obrigatórios: quantidade, rua, número, bairro, nome completo, CPF e telefone. **Complemento** é opcional.
- **RN04:** O CPF deve ser válido (dígitos verificadores) e o telefone deve ter DDD + número. A validação ocorre no navegador **e** no servidor.
- **RN05:** Conversão de recicláveis: **10 garrafas PET por muda** (exemplo do grupo: 3 mudas = 30 garrafas; 2 mudas = 20). O valor deve ser **configurável**, não fixo no código.
- **RN06:** A conversão é calculada no **backend** (Service) e exibida antes do envio; o frontend apenas mostra o valor recebido.
- **RN07:** Ao enviar, o sistema cria a solicitação com status **"Aguardando recicláveis"** e devolve o **número da solicitação** (formato `#000123`, derivado do identificador do registro com 6 dígitos).
- **RN08:** O cidadão só envia depois de ver a tela de conversão e confirmar em "Enviar Solicitação".
- **RN09:** CPF e telefone são dados pessoais: **não vão para logs, mensagens de erro nem para respostas da API**. O CPF é guardado apenas com os dígitos.
- **RN10:** O endereço de destino é em Quixadá (município da AMMA); não há campo de cidade nesta versão.
- **RN11:** A funcionalidade é **pública**: não exige login.
- **RN12:** A tela de acompanhamento desta spec é montada **a partir da resposta do envio**. Não existe consulta pública de solicitação por número, porque números sequenciais permitiriam listar nomes e endereços de outras pessoas.

## Critérios de aceitação

### Cenário 1 — Pedido completo e envio

**Dado que** selecionei a espécie "Ipê-amarelo" no assistente  
**Quando** preencho quantidade 2, endereço e identificação válidos, clico em "Continuar" e depois em "Enviar Solicitação"  
**Então** a solicitação é criada com status "Aguardando recicláveis" e sou levado à tela "Acompanhar solicitação" com o número do pedido.

### Cenário 2 — Cálculo da conversão

**Dado que** estou no formulário com a espécie selecionada  
**Quando** informo quantidade 3 e clico em "Continuar"  
**Então** a tela "Conversão Eco Ponto" mostra "Para 3 mudas de Ipê-amarelo: 30 garrafas PET" e o resumo com quantidade, espécie e recicláveis necessários.

### Cenário 3 — Campos obrigatórios

**Dado que** deixei rua, número, bairro, nome, CPF ou telefone em branco  
**Quando** clico em "Continuar"  
**Então** não avanço, cada campo vazio mostra "Preencha este campo." junto ao campo e o foco vai para o primeiro erro.

### Cenário 4 — CPF e telefone inválidos

**Dado que** informei um CPF com dígitos verificadores incorretos  
**Quando** clico em "Continuar"  
**Então** vejo "Informe um CPF válido." no campo e o formulário não avança. Para telefone: "Informe um telefone com DDD."

### Cenário 5 — Limites de quantidade

**Dado que** a quantidade está em 1  
**Quando** clico em "−"  
**Então** o valor permanece 1. Ao atingir o máximo, o botão "+" fica desabilitado e um texto informa o limite.

### Cenário 6 — Voltar sem perder dados

**Dado que** estou na tela de conversão  
**Quando** clico em "Voltar"  
**Então** retorno ao formulário com todos os campos preenchidos como deixei.

### Cenário 7 — Acesso sem espécie

**Dado que** abri a página do formulário sem ter escolhido espécie  
**Quando** a página carrega  
**Então** sou redirecionado ao assistente de plantio com a mensagem da RN01.

### Cenário 8 — Falha no envio

**Dado que** a API está indisponível ou retorna erro  
**Quando** clico em "Enviar Solicitação"  
**Então** vejo "Não foi possível enviar sua solicitação agora. Seus dados foram mantidos." com opção de tentar novamente; durante o envio o botão fica desabilitado para evitar duplo clique.

### Cenário 9 — Validação no servidor

**Dado que** a API recebe dados inválidos (quantidade fora do limite, CPF inválido, campo obrigatório vazio)  
**Quando** processa a requisição  
**Então** responde `400` em `ProblemDetail` com a lista `errors` por campo, sem criar solicitação e sem repetir CPF ou telefone no corpo da resposta.

### Cenário 10 — Acompanhar solicitação

**Dado que** enviei a solicitação  
**Quando** a tela de acompanhamento abre  
**Então** vejo a linha do tempo com "Solicitação Enviada" concluída, "Aguardando recicláveis" em andamento e as etapas "Aprovação da AMMA" e "Retirada da muda" bloqueadas, além do "Resumo do Pedido".

## Restrições

- Cálculo e regras de negócio no **backend**; o frontend valida só para dar retorno rápido.
- **Única estrutura nova no banco:** a tabela da solicitação descrita abaixo ("Modelo de dados"). Nada além disso.
- Sem novas bibliotecas: máscaras de CPF e telefone são funções utilitárias simples no frontend.
- Endpoints novos sob o prefixo `/api`, em rota pública. Não alterar a segurança além do necessário e **não remover** o `denyAll()` de `/api/arvores`.
- Fora do escopo desta spec: aprovação pela AMMA e registro de recebimento de recicláveis (RF008), comprovante com QR Code (RF009), baixa de estoque pelo IFCE (RF010), consulta posterior do pedido e controle de estoque.

## Modelo de dados (nova entidade de solicitação)

Entidade em `entities/`, repositório em `repositories/`, enum de status em `enums/`.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | Long | Chave primária; o número exibido (`#000123`) é este valor com 6 dígitos |
| `especie` | relação com a espécie existente | Obrigatório |
| `quantidadeMudas` | inteiro | 1 até o máximo configurável |
| `totalRecicaveis` | inteiro | Valor calculado no momento do envio (garrafas PET) |
| `rua`, `numeroEndereco`, `bairro` | texto | Obrigatórios |
| `complemento` | texto | Opcional |
| `nomeSolicitante` | texto | Obrigatório |
| `cpf` | texto (11 dígitos) | Somente dígitos; nunca em respostas ou logs |
| `telefone` | texto | Somente dígitos |
| `status` | enum `AGUARDANDO_RECICLAVEIS` | Novos valores entram nas specs de aprovação e retirada |
| `criadaEm` | data e hora | Preenchida no servidor |

Criar a tabela pelo mecanismo que o projeto já usa para o esquema (conferir antes).

## Questões em aberto (resolver com o grupo antes de implementar)

1. **Login:** o formulário pede nome, CPF e telefone e não prevê conta de usuário. Esta spec assume que o cidadão **não precisa estar logado**. Confirmar.
2. **Modelo de dados:** a tabela acima é uma proposta; alinhar com o grupo (principalmente com quem cuida do estoque e da aprovação).
3. **Quantidade máxima por pedido:** não definida. Usar provisoriamente `arborize.solicitacao.max-mudas = 5` (configurável) até a AMMA decidir.
4. **Proporção de recicláveis:** confirmar com a AMMA que são 10 garrafas PET por muda (`arborize.solicitacao.garrafas-por-muda = 10`) e se vale para todas as espécies.
5. **Escopo da tela de acompanhamento:** confirmar se entra nesta sprint ou em outra spec. Aqui entra apenas a versão logo após o envio.
6. **Consulta posterior:** como o cidadão volta a ver o pedido (por exemplo, número da solicitação + CPF)? Não especificado.
7. **Duplicidade e estoque:** não há proteção no servidor contra o mesmo pedido enviado duas vezes após falha de rede, nem verificação de estoque. Aceitar nesta versão ou decidir uma regra.
