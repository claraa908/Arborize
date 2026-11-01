# Requisitos — Assistente de Plantio (quiz e recomendação de espécies)

> Referências: RF005 (Essencial), Épico 03 (primeira história), RN002, RN005 do documento de requisitos.
> Padrões técnicos e visuais: `docs/padroes-de-projeto.md`.

## Objetivo

Ajudar o cidadão a descobrir quais espécies nativas da Caatinga combinam com o local onde ele quer plantar, por meio de um quiz curto, e levá-lo a escolher uma espécie para pedir a muda (spec `002-solicitar-muda`).

## Histórias de usuário

### HU01 — Responder o quiz de plantio

Como cidadão,
quero responder perguntas simples sobre o local de plantio, uma por vez,
para informar as condições do meu espaço sem precisar de conhecimento técnico.

### HU02 — Receber espécies recomendadas

Como cidadão,
quero ver as espécies nativas adequadas às minhas respostas,
para escolher uma muda que não cause problemas no local (fiação, calçada, espaço).

### HU03 — Refazer o quiz

Como cidadão,
quero refazer o quiz com outras respostas,
para comparar recomendações caso eu tenha mais de um local ou tenha errado uma resposta.

### HU04 — Selecionar uma espécie

Como cidadão,
quero selecionar uma das espécies recomendadas,
para seguir direto ao pedido de doação da muda.

## Regras de negócio

- **RN01:** O quiz tem **10 perguntas**, exibidas uma por vez, com indicador "Pergunta N de 10" e barra de progresso.
- **RN02:** Toda pergunta exige uma resposta para avançar. Perguntas objetivas têm a opção "Não sei informar" (ou "Não tenho certeza"), tratada de forma conservadora (ver RN05).
- **RN03:** O cidadão pode **voltar** e alterar respostas anteriores sem perder as demais.
- **RN04:** Só são recomendadas espécies **nativas** da Caatinga e **ativas** no catálogo existente de espécies. Espécies exóticas ou invasoras (ex.: Nim Indiano) nunca aparecem.
- **RN05:** Regras de adequação (versão simplificada e **provisória**, ver "Questões em aberto"):
  - Fiação elétrica "Sim" ou "Não tenho certeza" → somente espécies de **porte pequeno**.
  - Espaço "Pouco espaço" ou "Não sei informar" → somente **porte pequeno**.
  - Espaço "Espaço razoável" → porte pequeno ou médio.
  - Espaço "Bastante espaço" → qualquer porte, **exceto grande se o local for calçada** (o plantio não pode danificar a infraestrutura, RN005 do negócio).
  - Quando mais de uma regra se aplicar, vale a mais restritiva.
- **RN06:** São exibidas **até 3 espécies**. Ordenação provisória: ordem alfabética do nome popular.
- **RN07:** As respostas e a espécie escolhida ficam **apenas em memória** no navegador (store Pinia). Recarregar a página reinicia o quiz. Nada é gravado no banco nesta spec.
- **RN08:** A espécie escolhida em "Selecionar Espécie" é repassada para a spec `002-solicitar-muda`.
- **RN09:** A funcionalidade é **pública**: não exige login.

## Critérios de aceitação

### Cenário 1 — Fluxo completo com recomendação

**Dado que** estou na tela "Assistente de Plantio"  
**Quando** respondo todas as perguntas e clico em "Finalizar"  
**Então** vejo a tela "Espécies recomendadas" com até 3 cartões, cada um com foto, nome popular, nome científico, selo de adequação, porte e botão "Selecionar Espécie".

### Cenário 2 — Calçada estreita com fiação

**Dado que** respondi calçada, pouco espaço e fiação elétrica "Sim"  
**Quando** finalizo o quiz  
**Então** só aparecem espécies nativas de porte pequeno.

### Cenário 3 — Resposta obrigatória

**Dado que** estou em qualquer pergunta sem ter escolhido uma opção  
**Quando** clico em "Próxima"  
**Então** não avanço e vejo a mensagem "Escolha uma opção para continuar."

### Cenário 4 — Voltar e alterar

**Dado que** estou na pergunta 3  
**Quando** clico em "Voltar" e mudo a resposta da pergunta 2  
**Então** a pergunta 2 mostra a nova resposta, a pergunta 3 mantém a resposta que eu já tinha dado e o progresso é atualizado.

### Cenário 5 — Nenhuma espécie adequada

**Dado que** as respostas resultam em um filtro sem nenhuma espécie no catálogo  
**Quando** finalizo o quiz  
**Então** vejo uma mensagem explicando que não encontramos espécies para essas condições e o botão "Refazer Quiz".

### Cenário 6 — Refazer o quiz

**Dado que** estou na tela de espécies recomendadas  
**Quando** clico em "Refazer Quiz"  
**Então** volto à pergunta 1 com todas as respostas limpas.

### Cenário 7 — Selecionar espécie

**Dado que** estou na tela de espécies recomendadas  
**Quando** clico em "Selecionar Espécie" em um cartão  
**Então** sou levado ao formulário "Solicitar muda" com essa espécie já preenchida.

### Cenário 8 — Falha na busca de recomendações

**Dado que** a API de recomendação está indisponível ou retorna erro  
**Quando** finalizo o quiz  
**Então** vejo uma mensagem de erro clara (sem detalhes técnicos), minhas respostas são mantidas e posso tentar de novo.

### Cenário 9 — Resposta inválida na API (servidor)

**Dado que** a API recebe respostas incompletas ou com valores inexistentes  
**Quando** processa a requisição  
**Então** responde `400` no formato `ProblemDetail`, com a lista `errors` indicando as perguntas inválidas, sem alterar nenhum dado.

## Restrições

- A recomendação é calculada no **backend** (camada Service), consultando o catálogo de espécies **já existente** (`EspecieController`/repositório de espécies). O frontend não decide quais espécies exibir.
- **Não criar tabelas nem colunas novas** nesta spec. Se a entidade de espécie não tiver porte, indicador nativa/exótica ou situação ativa, **parar e pedir decisão** (ver questão em aberto 3).
- Nenhum dado pessoal é coletado nesta spec.
- Fotos das espécies: usar o mecanismo que o catálogo atual já usa. Não expor bucket, chave interna do MinIO nem credenciais no contrato. Sem foto, exibir imagem de substituição.
- Endpoint novo sob o prefixo `/api`, em rota pública. Não alterar a configuração de segurança além do necessário e **não remover** o `denyAll()` de `/api/arvores`.
- O catálogo completo e filtrável (RF019) **não** faz parte desta spec.
- Sem novas bibliotecas no frontend nem no backend.

## Questões em aberto (resolver com o grupo antes de implementar)

1. **Perguntas 4 a 9 ainda não foram definidas.** Definidas: 1 (onde plantar), 2 (espaço), 3 (fiação) e 10 (tipo de terra). O RF005 cita também "largura de calçada", provavelmente uma das faltantes. Enquanto isso, o quiz é **orientado a dados** (lista de perguntas em arquivo tipado) para incluir as demais sem mexer na lógica.
2. **Regras de adequação (RN05) e ordenação (RN06)** são provisórias. Confirmar com o grupo/AMMA, inclusive como "tipo de terra" influencia a recomendação (hoje só é coletado).
3. **Catálogo de espécies:** confirmar quais atributos a entidade atual tem (porte, nativa/exótica, ativa, foto, nome científico) e se há dados cadastrados. Faltando atributo, decidir com o grupo antes de mexer no esquema.
4. **Textos das opções:** as descrições curtas e os textos de apoio de cada pergunta serão redigidos em português simples e precisam ser revisados pelo grupo. Para nomes de espécies, usar sempre o nome que vier do catálogo.
