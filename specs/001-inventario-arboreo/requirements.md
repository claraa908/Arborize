# Requisitos: Inventário Arbóreo Georreferenciado

**Spec:** `001-inventario-arboreo`  
**Requisito principal:** RF021  
**Referências complementares:** RU005, RNF004, RNF005, RNF006, RNF007, RNF011 e RNF012  
**Sistema:** Arborize | AMMA de Quixadá

**Estado da etapa:** implementação concluída; pronta para integração; aceite final pendente. O escopo funcional está congelado durante a integração, exceto por correções comprovadamente necessárias. As decisões institucionais pendentes estão registradas na seção 7.

## 1. Objetivo

Permitir que a equipe da AMMA cadastre, visualize, pesquise e atualize indivíduos arbóreos do município por meio de um inventário georreferenciado, com mapa interativo, listagem e fichas técnicas individuais.

O módulo será inicialmente disponibilizado somente na área administrativa do Arborize.

Cada árvore cadastrada representa um indivíduo específico, independentemente de outras árvores da mesma espécie.

## 2. Histórias de usuário

### HU01: Consultar o inventário

Como funcionário da AMMA, quero visualizar as árvores cadastradas em um mapa interativo e em uma listagem, para acompanhar sua distribuição geográfica no município.

### HU02: Cadastrar uma árvore

Como funcionário autorizado da AMMA, quero cadastrar uma árvore informando sua espécie, localização e características técnicas, para incluí-la no inventário municipal.

### HU03: Consultar a ficha técnica

Como funcionário da AMMA, quero selecionar uma árvore no mapa ou na listagem e visualizar sua ficha técnica, para consultar sua identificação, localização e condições de conservação.

### HU04: Editar uma árvore

Como funcionário autorizado da AMMA, quero atualizar informações de uma árvore, incluindo sua localização e condição fitossanitária, para manter o inventário atualizado.

### HU05: Pesquisar e filtrar árvores

Como funcionário da AMMA, quero pesquisar por código ou espécie e aplicar filtros por bairro, espécie e condição fitossanitária, para encontrar registros específicos.

## 3. Informações do cadastro

| Campo | Regra |
|---|---|
| Identificador único | ID numérico permanente gerado automaticamente; código legível derivado conforme RN14 |
| Espécie botânica | Obrigatória, selecionada do catálogo existente |
| Latitude | Obrigatória, em graus decimais |
| Longitude | Obrigatória, em graus decimais |
| Zona rural | Escolha explícita por checkbox, desmarcado por padrão; o geocodificador não define o modo |
| Bairro urbano | Obrigatório no modo urbano; selecionado pelo ID em catálogo ativo por um único seletor nativo |
| Localidade rural | Obrigatória em zona rural; texto informado pelo operador, sem catálogo presumido |
| CEP | Opcional; preenchido somente quando disponível e confirmado |
| Logradouro ou ponto de referência | Opcional; pode receber sugestão editável da geocodificação reversa |
| Data de plantio | Opcional |
| DAP (cm) | Opcional |
| Altura estimada (m) | Opcional |
| Condição fitossanitária | Valor inicial: Não avaliada |
| Observações | Opcionais |
| Fotografias | Opcionais, até 3 imagens |

Os campos opcionais desconhecidos deverão permanecer sem preenchimento. O sistema não deverá utilizar datas fictícias ou valores numéricos artificiais para representar informações ausentes.

### Classificação fitossanitária inicial

- Não avaliada;
- Aparentemente saudável;
- Com sinais de comprometimento;
- Morta.

Essa classificação é provisória e deverá ser validada pelos responsáveis técnicos da AMMA.

## 4. Regras de negócio

**RN01: Identificação individual**

Cada árvore deve receber um identificador único e permanente, que não pode ser alterado durante a edição.

**RN02: Associação com espécie botânica**

Cada árvore deve estar vinculada a uma espécie existente no catálogo botânico do Arborize. Uma mesma espécie pode estar associada a várias árvores.

O cadastro de árvores não deve criar novas espécies botânicas automaticamente.

**RN03: Georreferenciamento**

Toda árvore deve possuir latitude e longitude numericamente válidas.

As coordenadas devem utilizar graus decimais, respeitando os intervalos:

- Latitude: -90 a 90.
- Longitude: -180 a 180.

A verificação de pertencimento ao município depende de uma delimitação territorial confiável.

Enquanto essa referência não existir, o sistema não deverá bloquear o cadastro por possível localização fora de Quixadá nem apresentar alertas baseados em limites geográficos presumidos.

Caso uma delimitação confiável seja incorporada futuramente, pontos fora da área esperada poderão gerar um alerta de conferência, sem bloqueio automático.

**RN04: Dados técnicos desconhecidos**

Data de plantio, DAP e altura estimada são opcionais.

Quando informados, DAP e altura devem ser valores positivos, expressos em centímetros e metros, respectivamente.

Dados ausentes devem ser apresentados como "Não informado".

O método de medição e a identificação de valores estimados dependem de validação técnica da AMMA.

**RN05: Condição fitossanitária**

O registro deve possuir uma condição fitossanitária, inicialmente definida como "Não avaliada".

As condições disponíveis devem seguir a classificação provisória descrita neste documento, até sua validação institucional.

**RN06: Árvores mortas**

Uma árvore identificada como morta deve continuar no inventário.

Essa condição não representa autorização de supressão nem remoção física.

**RN07: Organização territorial**

Latitude e longitude são a informação geográfica principal e devem ser registradas imediatamente quando o usuário seleciona um ponto no mapa. O operador escolhe explicitamente entre área urbana e zona rural; o geocodificador nunca determina essa classificação.

Por padrão, o cadastro segue o fluxo urbano. Para uma árvore urbana, é obrigatório selecionar um bairro ativo existente no catálogo PostgreSQL pelo seu identificador estável. A interface apresenta um único seletor nativo, acessível por teclado, com bairros ativos em ordem alfabética e a opção inicial “Selecione o bairro”; não apresenta um campo de busca separado para este catálogo. Não é permitido cadastrar ou salvar árvore urbana com bairro digitado livremente ou sem seleção. Uma seleção ativa deve ser reconhecida como válida e limpar a mensagem de validação anterior; opções ausentes ou inativas continuam inválidas. Na edição, uma associação ativa existente deve ser carregada no seletor. Para uma árvore rural, é obrigatório informar `localidadeRural`; não se associa bairro urbano nem se exige catálogo de localidades rurais. CEP e referência de endereço são opcionais em ambos os fluxos.

A geocodificação reversa pode sugerir referência/endereço, CEP e nomes territoriais quando disponíveis. Uma sugestão de bairro urbano só pode pré-selecionar um registro ativo do catálogo quando houver uma única correspondência após normalização de caixa, acentuação e espaços. Ausência ou ambiguidade deixa a seleção sem alteração. No fluxo rural, a resposta pode sugerir um texto de localidade, sempre editável e sem inferir a classificação. Respostas assíncronas não substituem valores que o operador editou após o início da solicitação.

Novos clientes enviam a escolha explícita `zonaRural`, bairro urbano por `bairroId` ou `localidadeRural`, conforme o modo. O backend traduz essa escolha para `classificacaoTerritorial` legado (`BAIRRO_IDENTIFICADO` para urbano e `FORA_DA_AREA_URBANA` para rural), sem inferir o modo por coordenadas. `BAIRRO_NAO_IDENTIFICADO` permanece consultável em registros legados e não é reclassificado automaticamente. O texto `bairroNome` e a associação `bairro_id` são preservados para compatibilidade; no novo fluxo urbano, o nome canônico é obtido do registro ativo selecionado. A relação, tabela e IDs de `Bairro` existentes não serão removidos.

O filtro por bairro é apresentado como um seletor de escolha única alimentado pelos bairros ativos do mesmo catálogo. Sua opção inicial é “Todos os bairros”; ao selecionar um bairro, o frontend envia o nome no parâmetro textual `bairro` já existente, nunca o ID. “Todos os bairros” omite esse parâmetro. O filtro combina-se aos demais critérios, preserva o tamanho de página e aplica os mesmos critérios à tabela e ao mapa. O backend continua consultando nomes efetivamente associados às árvores, incluindo o fallback legado. A carga inicial dos nomes de referência não comprova homologação pela AMMA e não deve criar polígonos, coordenadas ou limites territoriais.

**RN08: Pesquisa e filtros**

A pesquisa deverá aceitar correspondências parciais por identificador, nome popular ou nome científico da espécie.

Os filtros disponíveis serão:

- Bairro;
- Espécie;
- Condição fitossanitária.

A pesquisa e os filtros poderão ser combinados por interseção: um registro somente será apresentado quando atender a todos os critérios ativos.

Mapa e listagem deverão refletir o mesmo conjunto de resultados.

**RN09: Atualização de registros**

O usuário autorizado poderá editar informações técnicas e corrigir a localização de uma árvore.

Os campos não modificados deverão ser preservados.

Será possível limpar intencionalmente campos opcionais.

O identificador único deve permanecer inalterado.

**RN10: Fotografias**

Cada árvore poderá possuir até três fotografias opcionais.

Serão permitidos:

- JPG;
- PNG;
- WebP.

O tamanho máximo será de 2 MB por imagem.

Durante a edição, fotografias existentes deverão ser preservadas, exceto quando houver uma ação explícita de remoção ou substituição.

**RN11: Controle de acesso**

O inventário será disponibilizado na área administrativa do Arborize.

A consulta exigirá acesso interno autorizado. Cadastro e edição serão permitidos somente aos perfis internos com as permissões correspondentes.

O backend deverá verificar a autenticação e a autorização das operações protegidas.

A definição definitiva dos perfis autorizados depende de validação com a AMMA.

**RN12: Persistência dos dados**

Os registros devem permanecer armazenados e disponíveis para consultas posteriores.

Falhas de operação não poderão gerar confirmações falsas de sucesso.

**RN13: Bases cartográficas**

O mapa do inventário deverá oferecer a base cartográfica OpenStreetMap e, quando
configurada, uma base de imagens de satélite MapTiler Satellite. A atribuição exigida
por cada provedor deverá permanecer visível. A chave MapTiler será fornecida por
configuração de ambiente `VITE_MAPTILER_API_KEY`; nenhuma chave real será armazenada
no repositório. Sem a chave, a base OpenStreetMap continuará disponível e a interface
informará que a opção de satélite não está configurada.

**RN14: Código institucional legível**

Cada árvore deve apresentar um código legível derivado diretamente do identificador
numérico permanente já atribuído pelo sistema, no formato `ARV-QXD-000001`. O código é
composto pelo prefixo `ARV-QXD-` seguido do ID decimal preenchido à esquerda com zeros
até seis dígitos. IDs com mais de seis dígitos devem ser apresentados integralmente, sem
truncamento. O código é derivado, não editável e não deve ser armazenado como uma segunda
sequência de identificação.

O código deve ser apresentado de forma idêntica na listagem, na ficha técnica e nos
popups dos marcadores. A pesquisa deve localizar registros por correspondência parcial
do código, sem diferenciar maiúsculas de minúsculas e desconsiderando espaços externos;
os critérios de pesquisa por espécie existentes permanecem disponíveis.

**RN15: Miniaturas de fotografias**

Quando houver fotografias associadas, a listagem e o popup do marcador devem exibir uma
miniatura de uma delas. Para manter a escolha determinística sem criar novo cadastro de
capa, será utilizada a fotografia de menor identificador entre as associadas à árvore.
Quando não houver fotografia, a interface deve exibir uma ilustração padrão claramente
identificada como "Sem fotografia"; não deve criar ou sugerir uma fotografia real.

O conteúdo da miniatura deve ser recuperado somente pelo endpoint protegido de conteúdo
da fotografia. URLs públicas do MinIO, chaves internas e acesso sem autorização não
podem ser expostos. A listagem deve carregar conteúdo de forma tardia para linhas visíveis;
no mapa, o conteúdo deve ser solicitado somente para o popup selecionado/aberto, nunca em
lote para todos os marcadores. Falhas e acesso negado devem permanecer distintos da
ausência de fotografia.

**RN16: Seleção e envio seguro de fotografias no cadastro**

Durante o cadastro, a seleção de fotografias válidas deve apresentar pré-visualizações
antes da criação da árvore e permitir remover ou substituir arquivos selecionados. A
seleção, remoção ou substituição não deve apagar dados já preenchidos, reinicializar o
mapa nem remover a localização marcada. Erros de formato, tamanho ou quantidade devem
ser comunicados sem bloquear, ocultar ou quebrar a tela.

Os arquivos selecionados no cadastro só devem ser enviados depois que a criação da árvore
for confirmada, usando o identificador retornado e o endpoint de fotografias existente.
Se a árvore for criada, mas um ou mais uploads falharem, o sistema deve informar o sucesso
parcial, preservar a referência da árvore criada e permitir tratar os arquivos pendentes
sem repetir o cadastro nem criar outro indivíduo.

**RN17: Seletores de bairro no formulário e na consulta**

O formulário urbano de cadastro e edição usa somente um seletor HTML nativo associado ao rótulo “Bairro *”. A lista provém de `GET /api/bairros`, inclui somente bairros ativos e é ordenada alfabeticamente; a opção inicial é “Selecione o bairro”. A seleção e o payload de cadastro/edição usam o ID numérico do bairro. Não existe uma busca textual separada no formulário. Uma sugestão automática do geocodificador continua possível somente para correspondência única com opção ativa e não substitui escolha manual posterior. O modo rural continua ocultando o seletor e exigindo `localidadeRural`.

Na página principal, o filtro territorial é um seletor nativo de escolha única, também carregado de `GET /api/bairros`, com “Todos os bairros” como opção inicial. O valor enviado ao endpoint é o nome do bairro no parâmetro textual existente `bairro`; a opção geral omite esse parâmetro. Os demais filtros e o tamanho de página são preservados, e tabela e mapa recebem os mesmos critérios. A busca textual do backend continua podendo localizar nomes associados a registros legados.

## 5. Critérios de aceitação

### CA01: Cadastrar árvore

**Dado que** um usuário autorizado informa uma espécie existente e coordenadas válidas,  
**Quando** confirma o cadastro,  
**Então** o sistema salva a árvore, gera um identificador único e disponibiliza seu registro para consulta.

### CA02: Impedir cadastro incompleto

**Dado que** o usuário está preenchendo o formulário,  
**Quando** tenta salvar sem espécie ou com coordenadas inválidas,  
**Então** o sistema impede o cadastro e informa os campos que precisam ser corrigidos.

### CA03: Aceitar dados desconhecidos

**Dado que** a data de plantio, o DAP ou a altura são desconhecidos,  
**Quando** o usuário cadastra a árvore sem esses dados,  
**Então** o sistema aceita o registro e apresenta os valores ausentes como "Não informado".

### CA04: Visualizar o inventário

**Dado que** existem árvores cadastradas,  
**Quando** o funcionário acessa o Inventário Arbóreo,  
**Então** o sistema apresenta os registros no mapa e na listagem, permitindo acessar a ficha técnica a partir de qualquer uma dessas visualizações.

### CA05: Pesquisar e filtrar

**Dado que** existem diferentes árvores cadastradas,  
**Quando** o funcionário pesquisa ou combina filtros,  
**Então** o sistema apresenta somente os registros correspondentes aos critérios ativos, tanto no mapa quanto na listagem.

### CA06: Editar uma árvore

**Dado que** uma árvore está cadastrada,  
**Quando** um usuário autorizado altera suas informações e salva,  
**Então** as alterações são persistidas, o identificador permanece o mesmo e os campos não modificados são preservados.

### CA07: Identificar árvore morta

**Dado que** uma árvore foi identificada como morta,  
**Quando** o técnico atualiza sua condição,  
**Então** o registro permanece disponível no inventário com a classificação correspondente.

### CA08: Anexar fotografias

**Dado que** o usuário está cadastrando ou editando uma árvore,  
**Quando** adiciona até três imagens válidas,  
**Então** o sistema permite associá-las ao registro e disponibilizá-las para consulta.

### CA09: Ausência de resultados

**Dado que** o inventário não possui registros ou nenhum registro corresponde aos filtros,  
**Quando** o funcionário realiza a consulta,  
**Então** o sistema apresenta uma mensagem adequada, sem exibir dados fictícios.

### CA10: Falha de operação

**Dado que** ocorre uma falha durante a consulta ou o salvamento,  
**Quando** a operação não é concluída,  
**Então** o sistema apresenta uma mensagem de erro e não informa sucesso indevidamente.

### CA11: Registro inexistente

**Dado que** o funcionário tenta acessar uma árvore que não existe,  
**Quando** a consulta é realizada,  
**Então** o sistema informa que o registro não foi encontrado.

### CA12: Controle de acesso

**Dado que** um usuário não está autenticado ou não possui autorização,  
**Quando** tenta acessar uma operação protegida do inventário,  
**Então** o backend impede a operação conforme as permissões exigidas.

### CA13: Filtros combinados

**Dado que** existem árvores de diferentes bairros, espécies e condições,  
**Quando** o funcionário combina pesquisa e múltiplos filtros,  
**Então** o mapa e a listagem apresentam somente os registros que satisfazem todos os critérios.

### CA14: Corrigir localização

**Dado que** uma árvore já está cadastrada,  
**Quando** o usuário autorizado corrige suas coordenadas e salva,  
**Então** o sistema mantém seu identificador e apresenta a árvore na localização atualizada.

### CA15: Limpar campos opcionais

**Dado que** uma árvore possui campos opcionais preenchidos,  
**Quando** o usuário autorizado remove intencionalmente um desses valores e salva,  
**Então** o sistema persiste a alteração e apresenta o campo como "Não informado".

### CA16: Rejeitar fotografias inválidas

**Dado que** o usuário seleciona arquivos para anexar,  
**Quando** um arquivo possui formato incompatível, ultrapassa 2 MB ou faz o total exceder três fotografias,  
**Então** o sistema rejeita a operação inválida, informa o motivo e preserva a tela, os
campos preenchidos, o mapa e a localização marcada.

### CA17: Diferenciar erro de inventário vazio

**Dado que** ocorre uma falha ao carregar os registros,  
**Quando** o sistema não consegue concluir a consulta,  
**Então** apresenta um estado de erro, sem confundir a falha com ausência de árvores.

### CA18: Preservar formulário após falha

**Dado que** o usuário preencheu os dados do formulário,  
**Quando** ocorre uma falha ao salvar,  
**Então** o sistema informa o problema e preserva os dados preenchidos para uma nova tentativa.

### CA19: Validação territorial

**Dado que** o usuário informa coordenadas numericamente válidas,  
**Quando** o sistema ainda não possui uma delimitação municipal confiável,  
**Então** o cadastro não é bloqueado por uma verificação territorial presumida.

### CA20: Alternar base cartográfica

**Dado que** o mapa e seus marcadores estão carregados, e que a base de satélite está
configurada,  
**Quando** o funcionário alterna entre Mapa e Satélite,  
**Então** a base muda sem perder centro, zoom, marcadores ou seleção atual, e os créditos
do provedor ativo permanecem visíveis.

### CA21: Satélite sem configuração

**Dado que** não há chave MapTiler configurada,  
**Quando** o funcionário acessa o mapa,  
**Então** OpenStreetMap permanece disponível, a opção Satélite informa que precisa ser
configurada e nenhuma imagem ou registro fictício é apresentado.

### CA22: Apresentar e pesquisar pelo código institucional

**Dado que** uma árvore possui um identificador numérico permanente,  
**Quando** o sistema apresenta a árvore na listagem, na ficha técnica ou no popup do mapa,  
**Então** exibe o mesmo código `ARV-QXD-` seguido do ID preenchido à esquerda até seis
dígitos, sem truncar IDs maiores, e a pesquisa parcial pelo código encontra o registro
sem diferenciar maiúsculas de minúsculas ou espaços externos.

### CA23: Exibir miniatura na listagem

**Dado que** uma árvore da página atual possui fotografias associadas,  
**Quando** sua linha estiver visível na listagem,  
**Então** o sistema carrega tardiamente uma miniatura da fotografia associada pelo endpoint
protegido; quando não houver fotografia, apresenta a ilustração identificada como "Sem
fotografia", sem criar conteúdo fictício. Falha ou acesso negado ao conteúdo é apresentado
como indisponibilidade, não como ausência de fotografia.

### CA24: Exibir miniatura no popup sem carregar todas as fotos do mapa

**Dado que** existem árvores com e sem fotografias no conjunto de marcadores,  
**Quando** o funcionário abre o popup de uma árvore,  
**Então** o sistema apresenta seu código e carrega somente a miniatura dessa árvore pelo
endpoint protegido; os demais marcadores não provocam requisições de conteúdo de imagem.
Se não houver fotografia, o popup apresenta a ilustração "Sem fotografia". Falhas e
acesso negado são apresentados como indisponibilidade, não como ausência de imagem.

### CA25: Selecionar fotografias sem interromper o cadastro

**Dado que** o operador preencheu dados da árvore e marcou sua localização no mapa,
**Quando** seleciona fotografias válidas dentro dos limites de formato, tamanho e
quantidade,
**Então** o formulário apresenta pré-visualizações antes de criar a árvore, permite
remover ou substituir cada arquivo selecionado e preserva os campos, o mapa e o ponto
marcado, sem desaparecer, travar ou quebrar o layout.

### CA26: Enviar fotografias após criar a árvore e tratar sucesso parcial

**Dado que** o operador selecionou fotografias válidas durante o cadastro,
**Quando** a árvore é criada com sucesso e ocorre falha no envio de uma ou mais imagens,
**Então** o sistema informa que a árvore foi criada e que há fotografias pendentes,
preserva a identificação do registro e permite tentar novamente somente os uploads
pendentes pelo endpoint existente, sem repetir o cadastro da árvore.

### CA27: Selecionar localização e sugerir dados territoriais

**Dado que** o usuário seleciona um ponto no mapa,  
**Quando** o ponto é escolhido,  
**Então** latitude e longitude são atualizadas imediatamente, sem aguardar geocodificação reversa.

**E** quando o provedor retorna endereço, bairro ou CEP, o sistema apresenta esses valores como sugestões editáveis, sem classificá-los como delimitação oficial.

### CA28: Cadastrar sem bairro ou CEP

**Dado que** a geocodificação não retorna bairro ou CEP,  
**Quando** o usuário registra a árvore como `BAIRRO_NAO_IDENTIFICADO`,  
**Então** o cadastro é permitido com os campos bairro e CEP sem preenchimento e sem valores fictícios.

### CA29: Confirmar ou corrigir bairro sugerido

**Dado que** o provedor sugere um bairro,  
**Quando** o usuário autorizado confirma ou corrige o texto e salva,  
**Então** o sistema persiste o nome informado sem exigir correspondência com um catálogo e mantém a classificação territorial escolhida pelo usuário.

### CA30: Distinguir situação territorial

**Dado que** não há delimitação municipal ou urbana confiável integrada,  
**Quando** uma árvore é cadastrada ou editada,  
**Então** o sistema não infere `FORA_DA_AREA_URBANA` a partir das coordenadas ou do geocoder e preserva separadamente os estados `BAIRRO_NAO_IDENTIFICADO` e `FORA_DA_AREA_URBANA`.

### CA31: Filtrar pelos bairros registrados

**Dado que** árvores possuem nomes de bairro efetivamente registrados,  
**Quando** o usuário pesquisa parcial ou integralmente um bairro no filtro e combina esse critério com outros filtros,  
**Então** mapa e listagem retornam somente árvores cujo bairro registrado corresponde ao texto pesquisado, sem distinguir maiúsculas e minúsculas e sem depender de `GET /api/bairros`.

### CA32: Preservar associações territoriais existentes

**Dado que** há árvores associadas a registros da entidade `Bairro`,  
**Quando** a transição para o armazenamento textual é aplicada,  
**Então** os vínculos e nomes existentes são preservados, os nomes são disponibilizados no novo campo territorial e nenhum dado ou tabela é removido.

> **Evolução do escopo:** CA27–CA32 registram a etapa anterior de bairro textual opcional sem catálogo obrigatório. Para novos cadastros e edições, seu fluxo é substituído pelos CA33–CA39 abaixo. Eles permanecem como histórico e como requisitos de compatibilidade/leitura de registros legados; em caso de conflito, RN07 e CA33–CA39 são normativos para a experiência atual.

### CA33: Cadastrar árvore urbana com bairro ativo

**Dado que** o formulário está no modo urbano e o catálogo ativo de bairros foi carregado,
**Quando** o operador seleciona um bairro ativo no seletor e cadastra a árvore,
**Então** a requisição envia o `bairroId` selecionado, o backend valida que o registro existe e está ativo, e a árvore persiste a associação e seu nome canônico.

### CA34: Exigir bairro urbano e localidade rural conforme o modo

**Dado que** o operador tenta salvar uma árvore,
**Quando** o modo é urbano sem `bairroId` ativo ou rural sem `localidadeRural` não vazia,
**Então** a interface destaca e foca o primeiro campo inválido e o backend rejeita a requisição sem persistir a árvore.

**E** quando o modo é rural e a localidade é válida, o registro persiste sem `bairroId` nem `bairroNome` urbano.

### CA35: Sugerir dados sem classificar a zona

**Dado que** o operador seleciona coordenadas no mapa,
**Quando** latitude e longitude são atualizadas e a geocodificação retorna endereço, CEP ou nome territorial,
**Então** o endereço/CEP são sugestões editáveis e, no modo urbano, o bairro só é pré-selecionado por correspondência única com o catálogo ativo após normalização de acentuação, caixa e espaços.

**E** no modo rural, eventual sugestão de localidade permanece texto editável; nenhuma resposta do provedor muda o modo urbano/rural.

**E** respostas atrasadas não sobrescrevem campos editados manualmente depois do início da requisição; correspondências ambíguas ou ausentes não selecionam um bairro.

### CA36: Carregar catálogo de referência sem duplicações

**Dado que** o catálogo PostgreSQL pode conter bairros anteriores ou registros com variações de caixa, acentuação e espaços,
**Quando** a carga inicial de referência é executada no banco aprovado,
**Então** os nomes do PMPI que não possuam equivalentes são inseridos uma única vez como ativos, sem renomear, reativar, excluir ou trocar IDs/associações existentes.

**E** quando a mesma carga é executada novamente, nenhum registro duplicado é criado e os registros preexistentes mantêm seus valores.

### CA37: Preservar árvores legadas

**Dado que** existem árvores antigas com associação `bairro_id`, bairro textual ou classificação `BAIRRO_NAO_IDENTIFICADO`,
**Quando** o novo contrato é habilitado ou a árvore é consultada/editada,
**Então** IDs, metadados, vínculos e informação territorial anterior continuam disponíveis, sem reclassificação automática.

**E** clientes antigos que enviem a classificação legada continuam aceitos durante a transição se atenderem às regras anteriores; novos clientes usam `zonaRural` e o campo compatível ao modo.

### CA38: Filtrar por bairro com catálogo e dados legados

**Dado que** árvores urbanas novas e registros legados possuem associações ou nomes de bairro,
**Quando** o operador combina filtro textual de bairro com pesquisa, espécie ou condição,
**Então** tabela e mapa retornam o mesmo conjunto, com correspondência parcial e sem distinção de caixa, contemplando o nome canônico da associação e o fallback dos dados legados.

**E** localidades rurais não são apresentadas como bairros nem inferidas como tais pelo filtro.

### CA39: Editar árvores urbanas, rurais e legadas

**Dado que** uma árvore existente é carregada para edição,
**Quando** o operador salva uma atualização urbana com bairro ativo ou rural com localidade obrigatória,
**Então** o backend atualiza apenas a classificação/representação territorial explicitamente informada e preserva ID, metadados, coordenadas não alteradas e demais campos não editáveis.

**E** uma árvore legada sem classificação urbana/rural conhecida permanece sem reclassificação até que o operador forneça os dados necessários para um modo válido.

### CA40: Selecionar bairro no cadastro e edição

**Dado que** o catálogo de bairros foi carregado,  
**Quando** o operador acessa o fluxo urbano de cadastro ou edição,  
**Então** encontra um único seletor nativo rotulado “Bairro *”, com “Selecione o bairro” inicialmente e bairros ativos em ordem alfabética, navegável por teclado.

**E** quando seleciona um bairro ativo, a interface reconhece o ID como válido e remove o erro anterior desse campo; quando o ID está ausente ou não pertence ao catálogo ativo, a validação continua rejeitando a operação.

**E** ao editar uma árvore com associação ativa, o seletor apresenta essa associação; no modo rural, o seletor fica oculto e a validação continua exigindo a localidade rural.

**E** uma sugestão geográfica inequívoca pode selecionar o ID correspondente, mas uma escolha manual posterior é preservada contra respostas atrasadas.

### CA41: Filtrar o inventário por bairro selecionado

**Dado que** os bairros ativos foram carregados na página do Inventário,  
**Quando** o operador seleciona um bairro ou “Todos os bairros” e aplica os filtros,  
**Então** o filtro envia o nome selecionado no parâmetro textual `bairro`, ou omite esse parâmetro para “Todos os bairros”, e a tabela e o mapa usam o mesmo conjunto de critérios.

**E** os filtros de pesquisa, espécie e condição e o tamanho de página permanecem preservados; nomes legados continuam cobertos pela pesquisa textual do backend quando correspondem ao nome selecionado.

## 6. Limites do escopo

Esta spec contempla:

- Consulta administrativa do inventário;
- Mapa interativo de árvores;
- Listagem de registros;
- Cadastro de indivíduos arbóreos;
- Visualização de ficha técnica;
- Edição de registros;
- Pesquisa e filtros;
- Associação e visualização de fotografias.

Não contempla:

- Consulta pública do inventário;
- Dashboard e gestão de metas;
- Histórico detalhado de intervenções;
- Cadastro de vistorias e podas como processos independentes;
- Emissão e assinatura de laudos;
- Autorização de supressões;
- Exclusão definitiva de árvores;
- Funcionamento offline.

## 7. Dependências e decisões pendentes

### 7.1. Estado das decisões

Os comportamentos de cadastro, consulta, edição, filtros, paginação, mapa e fotografias
descritos neste documento compõem o escopo funcional desta spec; isso não equivale à
aprovação institucional das regras técnicas que seguem pendentes. A classificação
fitossanitária listada em RN05 é provisória. A homologação pela AMMA da nomenclatura
territorial, os procedimentos de medição/estimativa e os perfis autorizados continuam sem
aprovação formal. O fluxo urbano atual usa catálogo ativo. A lista de 24 bairros do PMPI foi
verificada em fonte pública e, conforme confirmação operacional do usuário, carregada no
PostgreSQL local de desenvolvimento após autorização explícita. Isso não representa homologação
da AMMA nem valida uso em banco compartilhado ou de produção. A solução de autenticação também
depende da arquitetura compartilhada do Arborize.

MapLibre, OpenStreetMap e MapTiler Satellite com configuração por variável Vite são
decisões técnicas adotadas durante a implementação. O funcionamento do estilo MapTiler
com uma chave real ainda não foi verificado. A adoção técnica não representa validação
institucional da AMMA nem disponibilidade contratual do provedor.

### 7.2. Mudança de escopo registrada durante a implementação

A alternância para imagens de satélite foi solicitada depois do início da implementação
da spec e incorporada como RN13 e CA20–CA21 nesta revisão. Ela não deve ser descrita como
parte do escopo original.

### 7.3. Evolução de escopo: código institucional e miniaturas

Após a implementação inicial, foi solicitado apresentar o ID permanente em formato
legível `ARV-QXD-000001` e mostrar miniaturas de fotografias na listagem e nos popups do
mapa. A evolução está formalizada em RN14–RN15 e CA22–CA24. O código é calculado a partir
do ID existente, sem uma sequência adicional; os DTOs e o carregamento tardio do conteúdo
protegido foram implementados em T26–T27. A validação integrada geral permanece na matriz
de tarefas e não é declarada concluída nesta revisão.

### 7.4. Evolução de escopo: robustez da seleção de fotografias no cadastro

O usuário relatou que, ao selecionar uma fotografia em `/sistema/inventario/nova`, a área
principal da interface fica quase toda branca, enquanto a sidebar continua visível. O
relato é registrado como defeito observado e reproduzido pelo usuário; sua causa técnica
permanece desconhecida e deve ser investigada, sem assumir uma origem antes de obter
evidências. RN16 e CA25–CA26 formalizam o comportamento esperado e a prevenção de cadastros
duplicados em falhas parciais de upload. A investigação e os testes de regressão ficam
planejados na T28; esta documentação não afirma que o defeito foi reproduzido
independentemente nem corrigido.

### 7.5. Evolução de escopo: bairro opcional sem catálogo obrigatório

Essa decisão foi aplicada na etapa registrada por CA27–CA32, incluindo a persistência de
texto territorial e CEP. A nova evolução abaixo volta a exigir associação de catálogo no
fluxo urbano; os critérios anteriores permanecem como histórico e compatibilidade dos
registros legados. O comportamento vigente é o da RN07 e dos CA33–CA39.

### 7.8. Evolução de escopo: seleção explícita urbano/rural e catálogo inicial de referência

A experiência de cadastro passa a oferecer duas escolhas explícitas. O padrão é urbano,
com seleção obrigatória de bairro ativo por `bairroId`; o modo rural substitui o seletor por
`localidadeRural` textual obrigatório. A geocodificação apenas sugere valores e não altera o
modo. A implementação preserva `Bairro`, `bairro_id`, `bairroNome`, `cep` e
`classificacaoTerritorial` legados. Uma migração aditiva adiciona `localidade_rural`; o
backend traduz os novos contratos para a classificação antiga e continua aceitando o formato
legado na transição. Árvores antigas sem bairro/classe conhecida permanecem consultáveis e
não são reclassificadas automaticamente.

A carga inicial preparada inclui somente a lista de 24 bairros publicada no Plano Municipal
da Primeira Infância de Quixadá, com referência “Fonte: IBGE 2023” na seção de indicadores
sociodemográficos (PMPI 2022–2031, p. 56 no PDF; tabela indica 24 bairros). A lista contém:
Alto da Boa Vista, Alto São Francisco, Baviera, Bôto, Campo Novo, Campo Velho, Carrascal I,
Carrascal II, Cedro, Centro, Cohab, Combate, Curicaca, Herval, Irajá, Jardim dos Monólitos,
Monte Alegre, Nova Jerusalém, Planalto Renascer, Planalto Universitário, Putiú, Residencial,
São João e Triângulo. Fonte consultada em 2026-11-01:
https://quixada.ce.gov.br/conselhoarquivo/1/302/planomunicipalpelaprimeirainfancia.pdf.

Uma página histórica da CREDE12 sobre Quixadá atribui à Lei Municipal nº 2.319, de
20-12-2007, 22 bairros, incluindo Carrascal sem divisão I/II e sem “Residencial”:
https://www.ce.gov.br/crede12-seduc/2016/02/25/quixada-historico/ (consultada em 2026-11-01).
O PMPI e essa referência histórica são fontes divergentes e nenhuma equivale à validação
formal da AMMA para o inventário. O SQL é manual, idempotente e não destrutivo; não é
executado pelo boot da aplicação. A carga autorizada no PostgreSQL local de desenvolvimento
já foi realizada e registrada em `tasks.md`. Qualquer aplicação em outro ambiente exige nova
autorização operacional, conferência de banco/schema e revisão pré/pós-execução. Não há inclusão
de polígonos, coordenadas ou delimitação urbano/rural.

O SQL de referência e o backfill foram exercitados com reexecução em PostgreSQL descartável,
com verificações de duplicidade normalizada e preservação de IDs, associações, estados de
atividade e nomes já preenchidos. Em operação posterior autorizada, a carga dos 24 bairros foi
aplicada ao PostgreSQL local de desenvolvimento, conforme confirmação do usuário; não foi
executado backfill territorial. Essa carga local não equivale a homologação da AMMA nem autoriza
aplicação em outros ambientes. A validação institucional continua pendente.

### 7.6. Validações institucionais

Precisam de confirmação com a AMMA:

- Classificação fitossanitária oficial;
- Fonte e abrangência da lista de bairros;
- Política institucional para nomenclatura territorial, caso a AMMA exija normalização ou catálogo oficial;
- Procedimentos de medição do DAP e estimativa de altura;
- Perfis internos autorizados a consultar, cadastrar e editar.

Essas pendências não autorizam a criação de regras técnicas ou institucionais fictícias.

### 7.6. Dependência de autenticação

O backend atual ainda não possui autenticação e autorização efetivas.

A implementação dessas proteções é necessária para atender à RN11 e ao CA12.

O módulo poderá ser desenvolvido e testado em ambiente controlado, mas não poderá ser considerado integralmente concluído ou pronto para uso administrativo em produção enquanto as restrições de acesso não estiverem implementadas.

### 7.7. Base técnica adotada

O design e a implementação registrados para esta spec utilizam:

- Reutilização do catálogo de espécies existente;
- Implementação do domínio de indivíduos arbóreos;
- Integração do mapa interativo;
- Fonte cartográfica apropriada;
- Armazenamento e recuperação de fotografias;
- Representação de campos opcionais;
- Tratamento de erros e validações;
- Consistência dos resultados entre mapa e listagem.

Esses detalhes serão descritos no `design.md`, sem alterar o escopo funcional estabelecido neste documento.
