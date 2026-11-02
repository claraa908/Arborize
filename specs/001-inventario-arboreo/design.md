# Design: Inventário Arbóreo Georreferenciado

**Spec:** `001-inventario-arboreo`  
**Documento:** `design.md`  
**Requisito principal:** RF021  
**Sistema:** Arborize | AMMA de Quixadá  
**Estado da etapa:** implementação concluída; pronta para integração; aceite final pendente. Escopo funcional congelado, salvo correções comprovadamente necessárias. O baseline técnico atualizado em 2026-11-01 distingue itens implementados e pendentes ao longo do documento.

## 1. Visão geral

O Inventário Arbóreo permitirá que usuários internos autorizados da AMMA consultem, cadastrem e atualizem indivíduos arbóreos georreferenciados.

O módulo será composto por quatro telas principais:

1. Inventário com mapa, pesquisa, filtros e listagem.
2. Cadastro de árvore.
3. Ficha técnica de árvore.
4. Edição de árvore.

A implementação deverá seguir o `requirements.md` desta spec.

Este documento define a experiência visual, o comportamento esperado e a arquitetura técnica proposta. Decisões técnicas que dependam de informações ainda não disponíveis devem permanecer identificadas como pendências.

O código legível derivado do ID permanente e as miniaturas na listagem e nos popups
(RN14–RN15, CA22–CA24) já estão implementados conforme T26–T27. A verificação integrada geral
continua condicionada às pendências de frontend, autorização e aceite da T21/T22.

Também registra a evolução RN16 e CA25–CA26 para seleção robusta de fotografias no cadastro.
O defeito foi relatado e reproduzido pelo usuário na tela `/sistema/inventario/nova`: após
selecionar uma fotografia, a área principal fica quase toda branca e a sidebar permanece
visível. A causa técnica não foi identificada. A investigação e a correção têm prioridade
sobre refinamentos estéticos menores e estão planejadas na T28; este documento não afirma
que houve reprodução independente ou correção.

### Fora do escopo

Não implementar nesta spec:

- Consulta pública do inventário.
- Gestão de metas de arborização.
- Histórico detalhado de intervenções e vistorias.
- Laudos e autorizações de supressão.
- Exclusão definitiva de árvores.
- Operação offline.

## 2. Arquitetura existente a preservar

### Frontend

A aplicação usa Vue 3, TypeScript, Vite, Vue Router, Pinia e Axios. O mapa é feito com
MapLibre GL JS já declarado no `frontend/package.json`. Tailwind CSS 4, League Spartan e
Material Symbols compõem o padrão visual existente.

Código do Inventário já presente:

- `frontend/src/views/auth/InventarioView.vue`: consulta inicial, pesquisa, filtros,
  tabela e mapa conectados à store.
- `frontend/src/views/auth/inventario/ArvoreFormView.vue` e
  `ArvoreDetalhesView.vue`: a view do formulário compartilha o fluxo de cadastro e edição;
  a ficha consulta `GET /api/arvores/{id}` pela store e apresenta os estados da consulta.
- `frontend/src/components/inventario/InventarioMapa.vue`:
  componente MapLibre com marcadores recebidos por props, seleção, navegação e alternância
  de bases.
- `frontend/src/components/inventario/ArvoreFormulario.vue`:
  formulário compartilhado de cadastro e edição, com validação e submissão pela store.
- `frontend/src/components/inventario/ArvoreLocalizacaoMapa.vue`:
  mapa auxiliar para busca de endereço e seleção de coordenadas no cadastro/edição; não associa
  bairros ou limites.
- `frontend/src/components/ui/AppDataTable.vue`: tabela compartilhada estendida para
  paginação recebida e destaque de seleção.
- `frontend/src/types/inventario.ts`, `services/inventarioService.ts`,
  `stores/inventarioStore.ts` e `components/inventario/useInventarioConsulta.ts`:
  contratos, chamadas, estados e coordenação de pesquisa.
- Rotas administrativas são definidas em `frontend/src/router/index.ts`, sob
  `/sistema/inventario`.

O `inventarioService` já faz chamadas reais. A página principal consulta ao montar. O
frontend deve tratar 401/403 como acesso indisponível, nunca como lista vazia. A existência
de rotas visuais, por si só, não equivale a autorização institucional para uso do módulo.

### Backend

O backend usa Java 21, Spring Boot, Spring Web MVC, Spring Data JPA e PostgreSQL. Entidades,
DTOs, mappers, controllers, services e repositories seguem a estrutura em
`backend/arborize/src/main/java/br/com/arborize/`. A API usa `ProblemDetail` estruturado
para erros do Inventário. `GlobalExceptionHandler` aplica o contrato globalmente; o endpoint
`GET /api/bairros` lista os bairros ativos já cadastrados em ordem alfabética.

A persistência JPA utiliza a configuração de schema existente. O perfil comum contém
`spring.jpa.hibernate.ddl-auto=update`; testes de persistência isolados usam um banco
PostgreSQL temporário com `create-drop`. Não iniciar a aplicação contra banco existente
para validar alterações de schema.

### Estado atual confirmado no código

O domínio `Arvore`, o vínculo legado com `Bairro`, consultas, cadastro, leitura,
atualização, catálogo de bairros e endpoints de fotografias existem no backend. O formulário
compartilhado e a ficha técnica do frontend usam as chamadas de service; a galeria de fotos
está integrada ao backend. A configuração atual permite acesso às rotas para testes locais,
mas não há autenticação/autorização funcional integrada nem perfis aprovados para produção.
O mapa cartográfico é funcional com OSM. O estilo satélite está implementado, mas ainda não
foi verificado com uma chave MapTiler real. Testes e limitações de armazenamento são
registrados nas seções 15 e 17.

### Backend

O backend utiliza:

- Java 21.
- Spring Boot.
- Spring Web MVC.
- Spring Data JPA.
- PostgreSQL.
- DTOs de requisição e resposta.
- Mappers manuais.
- MinIO para armazenamento de arquivos.

O novo domínio deverá respeitar a organização atual:

`Controller → Service → Repository`

As regras de negócio devem permanecer principalmente na camada de serviço, com validações adequadas nas entradas da API.

### Limitações e pendências conhecidas

- A entidade `Bairro` e `GET /api/bairros` existem para compatibilidade com os registros atuais.
  O fluxo urbano atual usa esse catálogo e o filtro consulta nomes textuais. Os 24 bairros de
  referência estão carregados no ambiente local; sua fonte e abrangência ainda dependem de
  validação da AMMA.
- A classificação fitossanitária e os métodos de medição/estimativa são provisórios.
- As rotas do Inventário estão liberadas para testes locais por `permitAll()`. Não há
  autenticação nem autorização funcional: essa configuração não atende RN11/CA12 e não pode
  ser considerada pronta para produção.
- A tela principal, o cadastro, a edição e a ficha usam os services reais. O formulário
  compartilhado só envia após ação explícita e preserva os dados em caso de falha; fotografias
  são gerenciadas pelos endpoints próprios, sem serem enviadas pelo PUT geral. A ficha limpa
  o registro anterior ao trocar o ID e diferencia carregamento, erro, não encontrado e acesso
  negado.
- A chave MapTiler é ausente no ambiente observado. A base OSM foi verificada no navegador;
  o carregamento de imagens satélite reais permanece sem teste.
- Compensações entre PostgreSQL e MinIO são síncronas e de melhor esforço. Interrupção
  abrupta pode deixar objetos sem metadados ou metadados sem objeto; não existe protocolo
  durável de reconciliação. Essa melhoria não impede o desenvolvimento independente das
  telas, mas permanece uma pendência antes de afirmar recuperação após crash.

## 3. Identidade visual

Todas as páginas devem utilizar o layout administrativo existente.

### Cores

- Verde principal: `#6B8B3F`.
- Verde escuro: `#4D6E1F`.
- Verde claro: `#E1E8D9`.
- Verde muito claro: `#F5F8F2`.
- Fundo geral: `#EEF2EA`.
- Texto secundário: `#6F6F6F`.

Os componentes devem priorizar os tokens existentes no CSS do projeto, em vez de repetir valores fixos sem necessidade.

### Características visuais

Preservar:

- Fonte League Spartan.
- Sidebar verde expansível e recolhível.
- Cabeçalho administrativo atual.
- Cards brancos com bordas suaves.
- Sombras discretas.
- Bordas arredondadas assimétricas.
- Botões principais verdes.
- Campos de formulário com foco destacado.
- Ícones Material Symbols Outlined.
- Layout responsivo com Tailwind.

Não criar um segundo layout administrativo nem introduzir outro sistema visual.

## 4. Tela principal do Inventário

**Rota:** `/sistema/inventario`

### Organização da página

A área de conteúdo apresentará:

1. Título "Inventário Arbóreo".
2. Breve descrição da funcionalidade.
3. Botão principal "Nova árvore".
4. Campo de pesquisa e filtros.
5. Mapa interativo.
6. Listagem das árvores.

O mapa ficará acima da listagem nas telas maiores, utilizando a largura disponível do conteúdo administrativo.

### Pesquisa

A pesquisa deverá aceitar correspondência parcial por:

- Código institucional da árvore; manter o ID numérico disponível como referência técnica quando necessário.
- Nome popular da espécie.
- Nome científico da espécie.

A interface deverá apresentar um campo de pesquisa com indicação clara dos critérios aceitos.

### Filtros

Disponibilizar:

- Bairro, por seletor nativo de escolha única alimentado pelos bairros ativos de
  `GET /api/bairros`, em ordem alfabética, com a opção inicial “Todos os bairros”. A opção
  selecionada envia seu nome no parâmetro textual `bairro`; “Todos os bairros” omite o
  parâmetro. Não enviar o ID ao filtro textual.
- Espécie.
- Condição fitossanitária.
- Ação "Limpar filtros".

Pesquisa e filtros funcionarão por interseção.

Mapa e listagem deverão representar o mesmo conjunto de registros filtrados. Caso a listagem tenha paginação, ela poderá mostrar uma página desse conjunto, enquanto o mapa apresentará os marcadores correspondentes ao conjunto filtrado.

### Mapa

O mapa deverá permitir:

- Aproximar e afastar.
- Arrastar a visualização.
- Visualizar marcadores das árvores.
- Selecionar um marcador.
- Consultar informações resumidas.
- Abrir a ficha técnica.
- Destacar a árvore selecionada.

O resumo de um marcador deverá conter:

- Código institucional legível, idêntico ao exibido na listagem e na ficha.
- Nome popular da espécie.
- Condição fitossanitária.
- Miniatura da fotografia de capa ou ilustração padrão "Sem fotografia".
- Ação "Ver detalhes".

#### Bases cartográficas

A base Mapa usa tiles OpenStreetMap com atribuição visível e link para copyright. A base
Satélite usa o estilo MapTiler Cloud `satellite-v4`, quando configurado por
`VITE_MAPTILER_API_KEY`. A chave é pública no bundle; usar uma chave de navegador dedicada
e restringir suas origens permitidas aos domínios implantados. Nunca incluir valor real no
repositório. Sem chave, a opção fica indisponível com explicação e OSM permanece operável.
A atribuição gerada pelo estilo e o logotipo MapTiler vinculado são mantidos na base
satélite. Trocar a base deve preservar câmera, marcadores, seleção e eventos da instância.
A configuração está descrita em `frontend/README.md`; a imagem real de satélite ainda
aguarda verificação com chave válida.

As condições poderão possuir representações visuais distintas, mas a informação nunca deverá depender exclusivamente da cor.

### Listagem

Priorizar a reutilização do `AppDataTable`.

Colunas propostas:

| Coluna      | Conteúdo                                                                         |
| ----------- | -------------------------------------------------------------------------------- |
| Código      | Código institucional `ARV-QXD-000001`, derivado do ID permanente                 |
| Fotografia  | Miniatura da fotografia de capa ou ilustração identificada como "Sem fotografia" |
| Espécie     | Nome popular e, quando adequado, científico                                      |
| Localização | Bairro ou referência                                                             |
| Condição    | Classificação fitossanitária                                                     |
| Ações       | Ver ficha e localizar no mapa; edição depende da integração futura e autorização |

A ação "Localizar no mapa" deverá centralizar e destacar o marcador correspondente.

A ação "Editar" somente estará disponível para usuários com permissão.

Não adicionar ação de exclusão definitiva.

### Estados da tela

Devem existir tratamentos distintos para:

- Carregamento.
- Inventário sem registros.
- Pesquisa sem resultados.
- Falha na API.
- Falha ao carregar o mapa.

#### Estado de erro da API

O frontend deve interpretar respostas `application/problem+json` conforme `ProblemDetail`
(RFC 7807) retornado pelo backend. Deve apresentar `detail` como mensagem principal e os
itens de `errors`, quando existentes, associados aos campos correspondentes. Erros de
requisição devem alimentar o feedback global por `AppToastContainer`, sem expor stack traces
nem converter falha em estado vazio. Respostas 401/403 devem ser apresentadas como acesso
indisponível.

Caso o mapa falhe, a listagem deverá continuar utilizável se os dados tiverem sido carregados corretamente.

## 5. Tela de cadastro

**Rota:** `/sistema/inventario/nova`

O formulário deverá utilizar breadcrumb, título, subtítulo e cards, seguindo as telas de cadastro existentes.

Em desktop amplo, o formulário usa uma composição dividida: o mapa ocupa aproximadamente dois
terços da área útil; o painel de dados ocupa o restante e rola independentemente. A divisão começa
somente quando há espaço suficiente após o menu lateral. Em tablets e celulares, o mapa vem antes
dos dados, em largura total, e a página rola normalmente.

### 5.1. Identificação

Campo obrigatório:

- Espécie botânica.

A espécie será selecionada do catálogo existente.

Cada opção deverá identificar adequadamente o nome popular e o nome científico.

O identificador individual será gerado pelo sistema após o cadastro.

Não permitir o cadastro de uma nova espécie dentro deste formulário.

### 5.2. Localização

Campos:

- Busca textual de endereço, usada para localizar e enquadrar um ponto aproximado no mapa.
- Latitude, obrigatória, exibida como somente leitura.
- Longitude, obrigatória, exibida como somente leitura.
- Modo territorial explícito: urbano por padrão ou zona rural por checkbox; o geocoder e as
  coordenadas não definem esse modo.
- Bairro urbano, obrigatório, em um único seletor nativo chamado “Bairro *”, abastecido com
  bairros ativos de `GET /api/bairros`, em ordem alfabética e com “Selecione o bairro” como
  opção inicial. Não existe campo de busca separado. A seleção e o payload usam o ID.
- Localidade rural, obrigatória em modo rural, como texto editável; o seletor urbano fica oculto.
- CEP, opcional e editável, somente quando disponível.
- Logradouro ou referência, opcional e editável.

O fluxo de localização será:

1. O usuário informa um endereço no campo "Buscar endereço".
2. O geocodificador pesquisa o endereço, centraliza o mapa com animação e posiciona um marcador
   provisório na coordenada retornada, preenchendo os valores coordenados no formulário.
3. O usuário confere a área e clica no ponto exato da árvore no mapa para ajustar a seleção.
4. O mapa move o marcador e atualiza latitude e longitude no estado do formulário.

Latitude e longitude não poderão ser digitadas manualmente; serão atualizadas pela busca ou pelo
clique no mapa e continuarão compondo o payload reativo do cadastro ou da edição. O ponto retornado
pela busca é uma referência aproximada que o usuário deve conferir e ajustar no mapa.

O geocodificador MapTiler utiliza a chave de ambiente `VITE_MAPTILER_API_KEY`, limita resultados
ao Brasil, prioriza português e aplica uma preferência de proximidade ao enquadramento aproximado
de Quixadá. Esse viés não representa nem impõe limites municipais oficiais. Sem a chave, a busca
de endereço fica indisponível; a base cartográfica OpenStreetMap continua disponível quando
carregada. A chave de navegador deve ter os domínios autorizados configurados em Allowed HTTP
origins no MapTiler Cloud e não deve ser versionada no repositório.

Quando a chave MapTiler estiver configurada, o mapa auxiliar inicia no estilo híbrido
`https://api.maptiler.com/maps/hybrid/style.json`. O controle permite alternar entre essa base e
o mapa de ruas. Sem a chave, o mapa de ruas permanece disponível e a opção híbrida é desativada.

Ao clicar no mapa, latitude e longitude são emitidas e armazenadas imediatamente, mesmo se a
geocodificação reversa falhar ou estiver pendente. O geocoder pode sugerir logradouro, bairro,
CEP e localidade rural quando disponíveis. No modo urbano, somente uma correspondência única
com bairro ativo pode preencher a seleção; no modo rural, a localidade sugerida é texto editável.
Respostas tardias não substituem alterações manuais. Resposta ausente ou incompleta não bloqueia
o cadastro. A interface cartográfica aprovada não muda nesta evolução.

Latitude e longitude serão expressas em graus decimais.

Valores fora dos intervalos globais permitidos deverão ser rejeitados.

Enquanto não existir uma delimitação municipal confiável, não bloquear coordenadas com base em limites presumidos de Quixadá.

No contrato atual, modo urbano exige um bairro ativo, enviado como `bairroId`; o backend deriva
`BAIRRO_IDENTIFICADO` e o nome canônico. Modo rural exige `localidadeRural`, não associa bairro e
é persistido como `FORA_DA_AREA_URBANA`. `BAIRRO_NAO_IDENTIFICADO` permanece representável em
registros legados e não é inferido nem reclassificado automaticamente. CEP e logradouro continuam
opcionais. O cadastro urbano depende do catálogo ativo; nenhum dos modos usa delimitação
geográfica presumida.

### 5.3. Características técnicas

Campos:

- Data do plantio.
- DAP em centímetros.
- Altura estimada em metros.
- Condição fitossanitária.
- Observações.

Data, DAP, altura e observações serão opcionais.

DAP e altura, quando informados, deverão ser positivos.

A condição inicial será "Não avaliada".

A lista provisória será:

- Não avaliada.
- Aparentemente saudável.
- Com sinais de comprometimento.
- Morta.

A terminologia final depende de validação técnica com a AMMA.

### 5.4. Fotografias

O formulário deverá permitir selecionar até três fotografias. Durante o cadastro, os
arquivos selecionados ficam pendentes no estado do formulário e não são enviados antes da
criação confirmada da árvore. A seleção não pode limpar os demais campos, desmontar ou
reinicializar o mapa, nem remover a localização marcada.

Formatos:

- JPG.
- PNG.
- WebP.

Limite: 2 MB por imagem.

Exibir prévias e permitir remover uma imagem da seleção antes do envio.
Permitir substituir um arquivo selecionado. As prévias devem permanecer associadas ao
formulário e não podem cobrir ou ocultar a área principal após uma alteração do seletor.

Informar claramente erros de tamanho, formato e quantidade sem congelar, apagar ou quebrar
a interface. Após falha de seleção, o operador deve continuar podendo revisar os campos,
usar o mapa e corrigir a seleção.

No cadastro, após a API confirmar a criação e devolver o ID da árvore, enviar os arquivos
pendentes, um por vez, pelo endpoint existente `POST /api/arvores/{id}/fotos`. Não incluir
arquivos no `POST /api/arvores` nem duplicar o cadastro para tentar novamente um upload.
Se a criação for confirmada e algum upload falhar, manter o ID e a referência da árvore
criada, informar sucesso parcial e permitir tentar novamente somente os arquivos que
falharam. As imagens já enviadas não devem ser reenviadas nessa tentativa.

**Defeito relatado pelo usuário:** ao selecionar uma fotografia em
`/sistema/inventario/nova`, a área principal fica quase toda branca, enquanto a sidebar
continua visível. O usuário relata que o problema foi reproduzido; a causa técnica ainda
não está identificada. A T28 deve investigar com evidência, considerando exceções
JavaScript, manipulação de arquivos e URLs Blob, eventos reativos, overlays, posicionamento
CSS e rolagem, sem presumir que qualquer uma dessas possibilidades seja a causa.

### 5.5. Ações

Botões:

- Cancelar.
- Cadastrar árvore.

Durante o salvamento:

- Exibir indicador de processamento.
- Evitar envios duplicados.
- Preservar os dados em caso de falha.
- Confirmar sucesso somente depois das operações efetivamente concluídas.

Após o cadastro bem-sucedido, disponibilizar a navegação para a ficha técnica.

Se o registro da árvore for salvo, mas houver falha posterior no envio de imagens, a interface deverá informar precisamente o sucesso parcial e permitir tratar os anexos pendentes. Não apresentar a operação completa como bem-sucedida.

### 5.6. Ajuste de composição visual

O formulário mantém o mapa como primeira etapa visual. Em desktop amplo, reserva aproximadamente
dois terços da largura útil ao mapa e mantém o painel de dados com rolagem própria. Em larguras
menores, inclusive desktops cujo menu lateral limite o conteúdo, apresenta mapa e dados empilhados,
com o mapa primeiro. A altura do mapa deve preservar uma área confortável de seleção.

A busca de endereço, a alternância compacta “Mapa de ruas / Satélite” e a ação “Ampliar mapa” ficam
agrupadas sobre o mapa em uma barra responsiva. A busca funciona por botão ou Enter, sem submeter
o cadastro. Uma única linha de estado apresenta a orientação essencial, andamento ou falha atual,
sem repetir instruções extensas. As atribuições cartográficas permanecem visíveis. A
identificação da árvore, as instruções e a leitura das coordenadas devem usar blocos compactos,
sem cartões informativos redundantes. Latitude e longitude permanecem obrigatórias, somente leitura
e visíveis em um único bloco compacto; campos obrigatórios e opcionais devem ser distinguíveis pelos
rótulos. As coordenadas são apresentadas com seis casas decimais, mantendo sua precisão original
no estado e no payload. As ações de cancelar e cadastrar/salvar permanecem claras e acessíveis em
telas estreitas e ficam fora da área rolável do painel no desktop amplo.

Esse ajuste refina a apresentação e as interações de seleção e correção de erros. Mantém os campos,
as validações de negócio, os payloads e os requisitos funcionais de cadastro e edição.

#### Seleção ampliada e validação orientada ao erro

“Ampliar mapa” abre um diálogo modal de localização com a mesma instância MapLibre, câmera,
marcador e controles. Alterações de tamanho chamam `resize`; não reinicializam o mapa. Cliques
continuam atualizando imediatamente as coordenadas no formulário. “Usar localização” retorna ao
formulário com o ponto escolhido. Fechar ou usar Escape também retorna sem desfazer a seleção,
pois ela já foi aplicada. O diálogo tem nome acessível, semântica modal nativa, contenção de
Tab/Shift+Tab entre seus controles e retorno do foco ao controle que o abriu; a rolagem de fundo
fica bloqueada enquanto aberto.

Ao salvar com campos inválidos, o formulário destaca os controles e, após atualizar as mensagens,
rola até o primeiro erro na ordem dos campos. Campos editáveis recebem foco; erros de coordenadas
levam ao canvas do mapa, com mensagem para marcar o ponto. Erros de campos retornados pelo backend
seguem a mesma orientação. Falhas gerais sem campo associado levam ao alerta do formulário.
O resumo e os erros locais não criam toasts duplicados. Dados preenchidos são preservados.

## 6. Ficha técnica

**Rota:** `/sistema/inventario/:id`

A página deverá seguir o padrão das telas de detalhes existentes.

### Cabeçalho

Exibir:

- Breadcrumb.
- Código institucional e ID numérico permanente da árvore.
- Nome popular.
- Nome científico.
- Condição fitossanitária.
- Ação de edição, quando permitida.
- Navegação para o inventário.

### Cards de informações

**Identificação botânica**

Apresentar o código institucional da árvore e os dados da espécie vinculada.

**Localização**

Apresentar mapa com um marcador, latitude, longitude, bairro e ponto de referência.

**Características técnicas**

Apresentar data de plantio, DAP, altura estimada e condição fitossanitária.

**Observações**

Apresentar as observações registradas.

**Fotografias**

Exibir galeria das fotografias associadas, com possibilidade de ampliação.

Campos sem informação deverão mostrar "Não informado".

Se o registro não existir, apresentar uma mensagem adequada e opção de retorno.

## 7. Tela de edição

**Rota:** `/sistema/inventario/:id/editar`

Cadastro e edição utilizam o mesmo componente `ArvoreFormulario.vue`. A view deriva
`isEditing` da rota (incluindo o identificador da árvore), carrega o registro existente no
modo de edição e seleciona a operação de cadastro ou atualização sem duplicar o formulário.

Os campos deverão ser preenchidos com os dados existentes.

### Comportamento

- O identificador não poderá ser alterado.
- A localização poderá ser corrigida.
- Campos opcionais poderão ser limpos intencionalmente.
- Dados não alterados deverão permanecer preservados.
- Fotografias existentes deverão ser mantidas por padrão.
- Remoção ou substituição de fotografias dependerá de ação explícita.
- O total de fotografias não poderá ultrapassar três.

Após salvar, apresentar os dados atualizados.

Em caso de falha, preservar o formulário para nova tentativa.

## 8. Fluxos de navegação

### Consulta

Inventário → Pesquisa/filtros → Seleção na listagem ou no mapa → Ficha técnica.

### Cadastro

Inventário → Nova árvore → Preenchimento → Seleção da localização → Seleção local e prévia das fotografias → Criar árvore → Enviar fotografias pendentes → Ficha técnica.

Se a criação da árvore for confirmada e o envio de fotografias falhar parcialmente, manter
a identificação da árvore criada, exibir o resultado parcial e oferecer nova tentativa
somente dos arquivos pendentes, sem repetir a operação de criação.

### Edição

Inventário ou ficha → Editar → Alterar campos → Salvar → Ficha atualizada.

## 9. Responsividade e acessibilidade

O módulo deverá funcionar em computadores e dispositivos móveis.

Em telas menores:

- Os filtros serão organizados verticalmente.
- O mapa utilizará a largura disponível.
- Os campos serão apresentados em uma coluna.
- A listagem respeitará o comportamento responsivo existente.
- A sidebar utilizará o mecanismo móvel do layout administrativo.

Os campos deverão possuir labels visíveis, mensagens claras e estados de foco. As coordenadas
serão somente leitura, com orientação para selecionar o ponto no mapa.

As condições fitossanitárias não poderão ser identificadas somente por cores.

## 10. Arquitetura do frontend

### Organização proposta

Respeitar a estrutura existente em `frontend/src/`.

A implementação poderá conter:

- Views administrativas de inventário, cadastro e edição, e detalhes.
- Componentes específicos de mapa e formulário.
- Types para os contratos de inventário.
- Service de requisições HTTP.
- Store Pinia do inventário.

Os nomes e caminhos definitivos deverão seguir as convenções encontradas no repositório durante a implementação.

### Componentes candidatos

- `InventarioMapa`: renderização e interação com MapLibre.
- `InventarioFiltros`: pesquisa e filtros.
- `ArvoreFormulario`: campos compartilhados entre cadastro e edição.
- `ArvoreMarcador`: apresentação das informações do marcador, quando houver necessidade de componente próprio.
- `ArvoreGaleria`: visualização de fotografias.

Esses são componentes propostos, não existentes. Devem ser criados somente se a separação reduzir duplicações e mantiver coerência com o código atual.

### Store e service

A store deverá coordenar:

- Registros carregados.
- Pesquisa e filtros ativos.
- Estados de carregamento.
- Erros.
- Árvore selecionada.
- Operações de cadastro e atualização.

O service deverá concentrar as requisições à API.

As views deverão cuidar principalmente da apresentação, navegação e interação do usuário.

Evitar chamadas HTTP diretas espalhadas pelos componentes.

Na implementação atual, `inventarioStore` separa resultados paginados da tabela e resultados
completos do mapa. `useInventarioConsulta` aplica os mesmos filtros a ambos; mudança de
página recarrega somente a tabela. Conforme a evolução vigente de RN07/RN17, o formulário usa
um único seletor nativo de bairro ativo e a página principal filtra pelo nome textual do bairro
selecionado; a espécie continua vindo do catálogo existente. `AppDataTable` recebe paginação e
seleção pela view principal.

### Sincronização mapa e lista

A pesquisa e os filtros deverão ser compartilhados.

Ao alterar qualquer filtro:

1. Atualizar os critérios ativos.
2. Solicitar ou recalcular os resultados correspondentes.
3. Atualizar mapa e listagem com os dados filtrados.
4. Manter consistência na seleção da árvore.

Se um registro selecionado deixar de atender aos filtros, a interface deverá encerrar ou atualizar seu destaque de forma previsível.

## 11. Integração com MapLibre

`frontend/src/components/inventario/InventarioMapa.vue` usa a dependência MapLibre GL JS
instalada. Recebe a lista de `ArvoreResumoResponseDTO` pela propriedade `arvores`; não chama
API nem acessa a store. Atualizações da propriedade atualizam marcadores sem recriar a
instância do mapa. Coordenadas fora dos intervalos globais são omitidas.

### Fontes e atribuição

- **Mapa:** raster `https://tile.openstreetmap.org/{z}/{x}/{y}.png`, atribuição OSM com link
  visível. O uso deve observar a política de tiles da OSMF; não usar prefetch/offline.
- **Satélite:** estilo MapTiler Cloud `satellite-v4`, URL montada com
  `VITE_MAPTILER_API_KEY`. Atribuição declarada pelo estilo é apresentada pelo controle
  nativo do MapLibre e o logotipo MapTiler vinculado aparece nessa base. Não há chave
  versionada; `.env.example` documenta a variável. Restringir Allowed HTTP origins no
  provedor. Sem chave, Satélite fica indisponível e Mapa continua disponível.

A base padrão OSM e a centralização aproximada em Quixadá foram visualizadas. A URL,
controle e transformação para MapTiler são cobertos por testes locais, porém não se afirma
que a fonte satélite carregue até que seja verificada com chave real e origem autorizada.
Não existem polígonos municipais ou bairros desenhados.

### Interação e ciclo de vida

Controles de navegação permitem zoom e deslocamento. Marcadores DOM distinguem as quatro
condições provisórias por símbolo, cor e texto. Selecionar apresenta código institucional,
espécie, condição, miniatura e ação de ficha; sem foto, apresenta a ilustração acessível
"Sem fotografia". O conteúdo é solicitado somente ao abrir o popup selecionado, usando o
service Axios e `GET /api/arvores/{id}/fotos/{fotoId}`. Outros marcadores não iniciam
download de imagem. O evento `abrirFicha` envia o ID numérico à rota. A seleção também
sincroniza com a tabela.
A ação da tabela pode localizar e destacar um marcador. Na troca de estilo, a transformação
preserva centro, zoom, rotação e inclinação atuais; marcadores e estado de seleção não são
recriados nem apagados. Ao desmontar a view, listeners, marcadores e o mapa são removidos.
Falha de rede/WebGL exibe mensagem e opção de nova tentativa sem impedir a tela restante.

### Mapa auxiliar de formulário

`ArvoreLocalizacaoMapa.vue` permite pesquisar um endereço para centralizar o mapa e selecionar
as coordenadas definitivas por clique. O evento atualiza os campos somente leitura e o estado do
formulário; o geocoder usa proximidade aproximada de Quixadá sem desenhar bairros ou impor limites
municipais. O mapa recebe coordenadas atuais como propriedade para exibir o marcador.

## 12. Modelo de dados implementado e decisões pendentes

### 12.1. Entidade de árvore

A entidade JPA `Arvore` representa um indivíduo. O modelo atual combina a escolha urbana/rural
explícita com a associação de catálogo no modo urbano e preserva os campos territoriais legados:

| Campo                    | Tipo Java                   | Regra                                                                                |
| ------------------------ | --------------------------- | ------------------------------------------------------------------------------------ |
| id                       | `Long` com `IDENTITY`       | Chave gerada, sem setter                                                             |
| especie                  | `Especie`, `ManyToOne` lazy | Obrigatória, sem cascata de remoção                                                  |
| latitude / longitude     | `BigDecimal`                | Obrigatórias, graus decimais                                                         |
| zonaRural                 | `Boolean` no contrato       | Escolha explícita; `null` preserva situação legada não classificada                  |
| bairro                    | `Bairro`, `ManyToOne` lazy  | Obrigatório para novas árvores urbanas; nulo no modo rural; vínculo legado preservado sem cascata |
| bairroNome                | `String`                    | Nome canônico derivado do bairro ativo; fallback de leitura legado, preservado       |
| localidadeRural           | `String`                    | Obrigatória no modo rural; nullable para compatibilidade com árvores existentes      |
| cep                       | `String`                    | Opcional; texto que preserva zeros à esquerda                                       |
| classificacaoTerritorial  | Enum com `EnumType.STRING`  | Mapeada da escolha explícita; não inferida de coordenadas                            |
| referenciaLocalizacao    | `String`                    | Logradouro/referência opcional                                                       |
| dataPlantio              | `LocalDate`                 | Opcional                                                                             |
| dapCm / alturaEstimadaM  | `BigDecimal`                | Opcionais e positivos quando informados                                              |
| condicaoFitossanitaria   | Enum com `EnumType.STRING`  | Obrigatória, padrão `NAO_AVALIADA`                                                   |
| observacoes              | `String`                    | Opcional                                                                             |
| criadoEm / atualizadoEm  | `LocalDateTime`             | Criados/atualizados por callbacks JPA                                                |

Fotos são mantidas na entidade separada `FotoArvore`, não em cascata na árvore. Para novos
requests, urbano exige `bairroId` ativo e resulta em `BAIRRO_IDENTIFICADO`; rural exige
`localidadeRural`, não associa bairro e resulta em `FORA_DA_AREA_URBANA`. `BAIRRO_NAO_IDENTIFICADO`
continua válido para leitura de registros legados e não é inferido nem reclassificado
automaticamente. `bairro_id`, a entidade `Bairro` e suas associações históricas permanecem.

#### Código institucional derivado

O identificador numérico `Arvore.id` permanece a única identidade persistida e é a chave
usada nas rotas. `codigo` é uma representação calculada, nunca uma coluna, sequence ou
campo editável: concatenar `ARV-QXD-` ao ID decimal, completando com zeros à esquerda até
seis dígitos (`1` → `ARV-QXD-000001`). IDs acima de seis dígitos permanecem completos.
O valor deve ser incluído nos DTOs de resumo e ficha e exibido sem variação na listagem,
na ficha e no popup. `search` deve aceitar correspondências parciais, sem distinção de
caixa e com espaços externos removidos, contra o código canônico além dos critérios
existentes. O código nunca substitui o ID numérico em rotas ou payloads de alteração.

#### Referência de miniatura

`ArvoreResumoResponseDTO` inclui `fotoCapa` opcional com apenas `id` e `contentType` (nulo quando
não há fotografia), junto ao campo `codigo`. A capa é a foto associada com menor
`FotoArvore.id`; seus metadados são obtidos em lote/projeção, sem consulta N+1. O endpoint de
conteúdo permanece a única fonte dos bytes e nenhuma chave ou URL do MinIO integra os DTOs.
Não há sequência nem atributo persistido adicional para capa.

### 12.2. Relacionamento com espécie

A implementação tem uma associação `ManyToOne` de `Arvore` para `Especie` (muitas árvores para uma espécie). O service confirma que a espécie informada existe.

Relacionamento conceitual:

`Especie 1:N Arvore`

A exclusão ou alteração de uma espécie não deverá provocar remoção acidental de árvores do inventário.

Não duplicar na entidade de árvore os dados botânicos já mantidos no catálogo de espécies.

### 12.3. Bairros

`Bairro` e `GET /api/bairros` alimentam o fluxo urbano atual e permanecem para compatibilidade
com as associações legadas. O formulário recebe bairros ativos em ordem alfabética e envia o ID
selecionado. O backend valida a atividade, persiste a associação e deriva dela o nome canônico.
Na página inicial, o seletor de filtro usa o mesmo catálogo, mas envia o nome no parâmetro textual
`bairro`, que o backend aplica também a nomes legados.

#### Transição e preservação

A transição territorial adicionou `bairro_nome`, `cep` e `localidade_rural` de forma aditiva,
sem remover a tabela `bairros`, a coluna `bairro_id`, sua chave estrangeira ou registros. O
backfill só preenche nome ausente a partir de associação existente; não substitui conteúdo nem
inventa dados. Respostas e filtros mantêm fallback para `bairro.nome` legado. Novos requests
urbanos usam `bairroId` ativo; novos requests rurais usam `localidadeRural` e não enviam bairro
urbano. CEP permanece opcional. O filtro da página usa nome textual em `bairro`, sem enviar ID.

O CEP é persistido como texto para preservar zeros à esquerda e não é obrigatório. A configuração
atual usa `spring.jpa.hibernate.ddl-auto=update` e não foi localizado Flyway/Liquibase; essa
atualização de schema não substitui o backfill. T29 preparou o SQL manual aditivo e seu backfill,
testados em PostgreSQL descartável; não iniciar a aplicação contra banco compartilhado para
migrar dados.

O endpoint `GET /api/bairros` retorna bairros ativos em ordem alfabética e atende o formulário
urbano e o filtro. Sua disponibilidade e a carga local não significam que a lista foi homologada
como fonte institucional oficial pela AMMA.

O inventário deverá distinguir:

- Bairro identificado.
- Bairro não identificado.
- Localização fora da área urbana.

Essa classificação será indicada pelo usuário autorizado. O geocoder pode sugerir um nome, mas
nunca define a situação territorial. Sem delimitação confiável, nenhuma coordenada permite inferir
`FORA_DA_AREA_URBANA`.

Não criar lista fictícia de bairros nem considerar "Fora da área urbana" como nome de bairro.

### 12.4. Condição fitossanitária

Os enums atuais são persistidos por nome (`EnumType.STRING`), independentes dos rótulos exibidos na interface. Valores em uso:

- `NAO_AVALIADA`.
- `APARENTEMENTE_SAUDAVEL`.
- `SINAIS_COMPROMETIMENTO`.
- `MORTA`.

Os valores são a classificação provisória correspondente aos rótulos do `requirements.md`; devem ser revisados caso a AMMA aprove outra classificação.

### 12.5. Fotografias

A implementação utiliza a entidade `FotoArvore` e associação própria com árvore; o objeto
fica no MinIO e seus metadados/referência são persistidos no PostgreSQL. `FotoArvoreStorage`
reutiliza o `MinioClient` e bucket existentes sem publicar objetos ou expor chave/URL MinIO
no DTO. Endpoints e formato real estão na seção 14.4.

O service valida conteúdo JPG/PNG/WebP e máximo de 2 MiB, limita a três imagens e usa
bloqueio da linha da árvore nas inclusões concorrentes. Upload e remoção coordenam MinIO e
transação PostgreSQL por compensação síncrona. A compensação não é durável: crash e falha da
própria compensação podem deixar inconsistência. Ver seção 15; reconciliação durável não faz
parte das tarefas atuais de tela.

## 13. Arquitetura do backend

A implementação atual mantém as responsabilidades `Controller → Service → Repository`, com DTOs e mapper manual. Os caminhos relevantes são:

- `controllers/ArvoreController.java` e `FotoArvoreController.java`;
- `controllers/GlobalExceptionHandler.java`, `BairroController.java` e `FotoArvoreMultipartExceptionResolver.java`;
- `services/ArvoreService.java`, `BairroService.java` e `FotoArvoreService.java`;
- `repositories/ArvoreRepository.java`, `ArvoreSpecifications.java`, `BairroRepository.java` e `FotoArvoreRepository.java`;
- `entities/Arvore.java`, `Bairro.java` e `FotoArvore.java`;
- `dtos/request/ArvoreCadastroRequestDTO.java`, `ArvoreAtualizacaoRequestDTO.java` e `DadosArvoreRequest.java`;
- `dtos/response/ArvoreResponseDTO.java`, `ArvoreResumoResponseDTO.java`, `ArvorePaginaResponseDTO.java`, `BairroResumoResponseDTO.java`, `EspecieIdentificacaoResponseDTO.java`, `FotoArvoreResponseDTO.java` e `FotoArvoreConteudoDTO.java`;
- `mappers/ArvoreMapper.java`, `enums/ClassificacaoTerritorial.java` e `CondicaoFitossanitaria.java`;
- `storage/FotoArvoreStorage.java` e `validation/FotoArvoreValidator.java`.

Essas estruturas estão sob `backend/arborize/src/main/java/br/com/arborize/`. O endpoint
`GET /api/bairros` está implementado e permanece legado/compatível; a validação da fonte oficial
não bloqueia o fluxo territorial textual definido nesta evolução.

### Responsabilidades

**Controller**

Receber requisições, validar contratos HTTP e retornar códigos de resposta apropriados.

**Service**

Executar as regras de negócio, verificar a espécie, validar atualizações, coordenar persistência e fotografias.

**Repository**

Realizar consultas e persistência com Spring Data JPA.

**Mapper**

Converter entidades em DTOs e DTOs em dados utilizados pelo serviço.

Evitar expor entidades JPA diretamente como respostas públicas da API.

## 14. Contratos atuais da API

O prefixo implementado é `/api/arvores`; os endpoints existentes de Biblioteca, Eventos e Espécies não fazem parte desta spec.

| Método | Endpoint                           | Finalidade                                         |
| ------ | ---------------------------------- | -------------------------------------------------- |
| GET    | `/api/arvores`                     | Listar e filtrar árvores                           |
| GET    | `/api/arvores/mapa`                | Consultar dados leves para marcadores              |
| GET    | `/api/arvores/{id}`                | Consultar ficha técnica                            |
| POST   | `/api/arvores`                     | Cadastrar árvore                                   |
| PUT    | `/api/arvores/{id}`                | Atualizar informações                              |
| GET    | `/api/bairros`                      | Listar bairros ativos (compatibilidade legada)      |
| POST   | `/api/arvores/{id}/fotos`          | Adicionar uma fotografia multipart no campo `file` |
| GET    | `/api/arvores/{id}/fotos`          | Listar metadados das fotografias associadas        |
| GET    | `/api/arvores/{id}/fotos/{fotoId}` | Recuperar bytes da fotografia (inline, `no-store`) |
| DELETE | `/api/arvores/{id}/fotos/{fotoId}` | Remover associação de fotografia                   |

Não criar endpoint para exclusão definitiva de árvore.

### 14.1. Consulta com filtros

Parâmetros atuais em ambos endpoints: `search`, `bairro` (nome textual), `especieId` e `condicao`. O filtro `bairro` tem correspondência parcial sem distinção de caixa e considera `bairro_nome` e o fallback `bairro.nome` legado. O frontend obtém a opção no catálogo, envia seu nome e omite o parâmetro para “Todos os bairros”; não envia ID para esse filtro. `GET /api/arvores` também recebe `page` (inicia em zero) e `size` (1 a 100; padrão 20). `GET /api/arvores/mapa` não pagina o conjunto filtrado.

A API deverá aplicar os critérios de busca e filtros de maneira combinada.

A listagem poderá retornar resultados paginados.

O endpoint do mapa deverá receber os mesmos critérios de filtro e retornar apenas os dados necessários para posicionar e identificar os marcadores.

A paginação da tabela não deverá alterar indevidamente o conjunto de registros representado pelo mapa.

As respostas `ArvorePaginaResponseDTO.content` e `GET /api/arvores/mapa` usam
`ArvoreResumoResponseDTO`, que deve incluir `codigo` e `fotoCapa: { id, contentType } | null`
para sustentar as duas visualizações. O endpoint de ficha inclui `codigo` em
`ArvoreResponseDTO`. A inclusão é uma ampliação pequena dos DTOs existentes; não criar
endpoint público ou URL de arquivo. O conteúdo da fotografia continua vindo do endpoint
protegido existente.

Na listagem, carregar a miniatura somente quando sua linha estiver visível, por meio do
service Axios e do endpoint protegido de conteúdo. No mapa, buscar bytes somente para o
popup aberto. Usar URLs Blob temporárias quando necessário e revogá-las ao substituir,
fechar/remover o item ou desmontar a view. Não buscar conteúdo de foto para cada marcador
ao carregar o conjunto do mapa. O estado sem fotografia usa ilustração local acessível;
erro, 401/403 e 404 de conteúdo são estados de indisponibilidade, não o fallback de ausência.

### 14.2. Cadastro

O contrato novo de `ArvoreCadastroRequestDTO` recebe `especieId`, `latitude`, `longitude`,
`zonaRural`, `bairroId` para modo urbano ou `localidadeRural` para modo rural, além de CEP,
referência e campos técnicos opcionais. Não exige que o cliente envie `classificacaoTerritorial`
nem `bairroNome`: o backend deriva a classificação da escolha explícita e o nome urbano do bairro
ativo. Contratos legados permanecem para compatibilidade durante a transição. Espécie,
coordenadas e contexto territorial válido são obrigatórios; a condição ausente recebe
`NAO_AVALIADA`.

O backend deverá:

1. Validar os dados obrigatórios.
2. Confirmar que a espécie existe.
3. Validar as coordenadas.
4. Validar os dados técnicos informados.
5. Persistir a árvore.
6. Retornar o registro criado com seu identificador.

Fotografias poderão ser enviadas pelos endpoints específicos após a criação.

### 14.3. Atualização

`PUT /api/arvores/{id}` recebe `ArvoreAtualizacaoRequestDTO`, representação completa dos campos editáveis. `especieId`, coordenadas, classificação territorial e condição são obrigatórios; campos opcionais nulos significam limpeza.

O frontend deverá carregar previamente os dados existentes e preencher o formulário.

Campos opcionais enviados explicitamente com valor nulo deverão ser limpos.

Campos não alterados deverão ser reenviados com seus valores preservados.

O identificador não deverá integrar os campos editáveis.

As fotografias deverão ser tratadas separadamente, evitando que uma atualização geral exclua anexos por omissão.

### 14.4. Fotografias

O upload deverá utilizar `multipart/form-data`.

A API deverá validar:

- Quantidade máxima de fotografias por árvore.
- Tamanho máximo de 2 MB por imagem.
- Formato permitido.
- Integridade e associação do arquivo.

A validação deverá ocorrer no backend, independentemente das verificações do frontend.

As respostas deverão informar o identificador do arquivo e os metadados necessários à interface.

O acesso às fotografias deverá respeitar a política administrativa de autorização.

### 14.5. Respostas e erros

Códigos verificados no Controller, handlers e testes:

- `200 OK`: consulta, listagem e atualização; consulta de metadados ou conteúdo da foto.
- `201 Created`: cadastro de árvore e upload de foto.
- `204 No Content`: remoção da associação da foto.
- `400 Bad Request`: entrada, enum, conversão, JSON ou validação inválidos.
- `404 Not Found`: recurso associado inexistente.
- `413 Content Too Large`: fotografia acima do limite.
- `500 Internal Server Error`: falha inesperada, sanitizada.
- `401 Unauthorized`/`403 Forbidden`: respostas possíveis de uma camada de autenticação/autorização. No ambiente local atual as rotas estão configuradas com `permitAll()` para testes; isso não equivale a autorização e não é adequado para produção.

O `GlobalExceptionHandler`, anotado com `@RestControllerAdvice` sem restrição de pacote,
retorna `ProblemDetail` com `type`, `title`, `status`, `detail`, `code` e, para erros de
campo, `errors`. Controllers legados que capturam exceções localmente ainda podem manter
respostas próprias até uma migração revisada por compatibilidade. Não há regra que justifique
inventar status `409`.

## 15. Validações e integridade

O backend deverá validar:

- Espécie existente.
- Latitude entre -90 e 90.
- Longitude entre -180 e 180.
- DAP positivo, quando preenchido.
- Altura positiva, quando preenchida.
- Condição fitossanitária válida.
- Limites de fotografias.
- Referências territoriais válidas, quando preenchidas.

Campos desconhecidos deverão permanecer nulos.

O sistema não deverá preencher valores técnicos fictícios.

### Consistência de fotografias: comportamento e limite verificados

PostgreSQL e MinIO não participam da mesma transação distribuída. O código registra
compensações transacionais: remove objeto após rollback de inclusão e tenta restaurar bytes
após rollback de remoção. O storage real, PostgreSQL real e transporte HTTP foram cobertos
por testes isolados documentados em `backend/arborize/src/test/java/br/com/arborize/integration/README.md`.

Os testes confirmam casos normais e compensações síncronas; também reproduzem que uma falha
de compensação pode deixar objeto órfão/metadado sem objeto, e que a interrupção da JVM após
a operação no MinIO e antes da confirmação no banco não executa callback em memória. Não há
outbox, journal durável nem worker de reconciliação. A prevenção/recuperação durável após
crash permanece pendente. Esta limitação deve ser transparente, mas não bloqueia T14–T16 ou
T18 por si só.

## 16. Autenticação e autorização

`SecurityConfig.java` permite as rotas do Inventário para testes locais, sem autenticação ou
autorização efetivas. As regras atuais também permitem outras rotas da API; isso não
representa autenticação administrativa. CSRF está desativado na configuração existente. A
liberação local não satisfaz RN11/CA12 e não deve ser tratada como política de produção. A
interface deve continuar tratando 401/403 como acesso indisponível, sem conversão para
resultado vazio.

Este módulo exige proteção administrativa, mas os perfis específicos dependem de validação com a AMMA.

O design deverá separar conceitualmente as permissões de:

- Consultar inventário.
- Cadastrar árvore.
- Editar árvore.
- Gerenciar fotografias.

A autorização deverá ser verificada no backend.

Ocultar botões no frontend não será considerado mecanismo suficiente de segurança.

A implementação de autenticação poderá ser organizada como dependência técnica própria, mas a entrega do inventário não poderá ser considerada pronta para produção sem as proteções exigidas no `requirements.md`.

Não presumir usuários, credenciais, tokens ou roles já implementados.

## 17. Testes previstos

### Backend

Prever testes para:

- Cadastro válido e inválido.
- Espécie inexistente.
- Validação de coordenadas.
- Dados técnicos opcionais.
- Medidas inválidas.
- Consulta e filtros combinados.
- Atualização e preservação do identificador.
- Limpeza intencional de campos opcionais.
- Permanência de árvores mortas.
- Fotografias inválidas ou em excesso.
- Registro não encontrado.
- Tratamento de falhas.
- Negação de acesso, após integração da autenticação.

### Frontend

Prever verificações para:

- Renderização da listagem.
- Estados de carregamento, erro e ausência de resultados.
- Pesquisa e filtros.
- Sincronização mapa/lista.
- Seleção de marcadores.
- Busca de endereço, centralização do mapa e seleção do ponto exato por clique.
- Atualização dos campos de coordenadas somente leitura e validação dos limites globais.
- Validação de formulários.
- Preservação de dados após falha.
- Cadastro e edição.
- Prévia, remoção e limite de fotografias.
- Responsividade.

As ferramentas de teste deverão seguir as dependências e convenções existentes no repositório.

### Evidência disponível (não equivale ao aceite fullstack)

Os relatórios em `backend/arborize/target/surefire-reports/` registram 217 testes aprovados
(21 suites, sem falhas/erros/ignorados): 180 unitários/contratos/segurança, 16 com
PostgreSQL real descartável, 19 com PostgreSQL e MinIO reais e 2 de transporte Tomcat
multipart; esses grupos podem compartilhar suites conforme o executor. A fixture e os
limites são descritos no README de integração. Este resultado não testa autenticação
funcional nem todos os módulos legados.

A última execução registrada no frontend para T13 foi build/TypeScript/lint aprovados e
81 testes do inventário aprovados. A checagem visual confirmou OSM, estados de erro e
responsividade sem transbordamento; não havia API local disponível nem chave MapTiler,
portanto não comprova fullstack com dados nem imagens satélite reais.

## 18. Dependências e decisões pendentes

Antes da conclusão definitiva do módulo, confirmar:

1. A classificação fitossanitária aprovada pela AMMA.
2. Homologação da AMMA para fonte, nomenclatura e abrangência da lista de bairros. O catálogo de referência está carregado no ambiente local e é uma dependência operacional do fluxo urbano atual; a carga não constitui aprovação institucional nem define delimitação geográfica.
3. Os procedimentos técnicos de medição e estimativa.
4. Os perfis internos autorizados.
5. A solução de autenticação e autorização.
6. A validação da chave/origens e condições comerciais do MapTiler antes de habilitar a base satélite em produção (a escolha técnica está feita; teste real pendente).
7. O volume esperado de árvores para orientar otimizações.

Essas pendências não autorizam a invenção de dados institucionais.

As decisões que não impedirem o desenvolvimento de componentes independentes poderão ser tratadas durante o planejamento, desde que permaneçam explícitas e não sejam apresentadas como regras aprovadas.

## 19. Estado para continuidade SDD

O `tasks.md` é o checklist de implementação e verificação; seus estados devem ser lidos junto
às evidências de cada tarefa. A etapa atual de implementação funcional está concluída e o
escopo está congelado para integração; não iniciar novas funcionalidades do Inventário, salvo
correção comprovadamente necessária. O fluxo territorial vigente é o urbano/rural explícito da seção
21 e está implementado nas tarefas T29–T37 conforme a rastreabilidade registrada. A carga
dos 24 bairros foi aplicada ao PostgreSQL local de desenvolvimento após autorização operacional
do usuário; isso não é homologação da AMMA nem evidência de carga em outros ambientes.

O formulário compartilhado atende cadastro e edição, e as fotografias têm endpoints e interface
integrados. A seleção de fotografia no cadastro, porém, continua com defeito visual reportado e
sem causa/correção confirmada (T28). O aceite integrado geral permanece pendente: não há
autenticação/autorização funcional, há verificações fullstack e de provedor ainda por concluir,
e permanecem validações institucionais. As rotas seguem liberadas com `permitAll()` somente para
testes locais. A verificação do satélite com chave real também está pendente.

A reconciliação durável após crash é uma melhoria pendente separada da integração normal
das telas e não deve ser presumida como requisito bloqueador das próximas tarefas de UI.

## 20. Evolução de escopo: bairro opcional sem catálogo obrigatório

Esta decisão posterior à implementação inicial substitui, para o fluxo novo, o select obrigatório
de bairro por texto opcional confirmado pelo operador e remove a dependência do catálogo
administrativo. O comportamento antigo e sua implementação permanecem registrados em T04/T24;
seus checkboxes concluídos descrevem o trabalho realizado à época e não atestam conformidade com
RN07 revisada. A revisão funcional exige as tarefas T29–T31, listadas no `tasks.md`.

A estratégia prescrita é adicionar `bairroNome` e `cep` nullable em `Arvore`, preservar `bairro_id`
e `Bairro` como vínculo legado, retroalimentar o nome existente sem apagar dados, ajustar DTOs,
validação, consulta/filtros e o formulário. O filtro textual deve operar sobre o nome de bairro
registrado na árvore, incluindo fallback de leitura do vínculo legado durante a transição. O
frontend pode apresentar a sugestão de geocoding e permitir sua edição; lat/lng são atualizados
independentemente e primeiro. CEP e nome de bairro podem faltar. A situação `FORA_DA_AREA_URBANA`
permanece escolha explícita, nunca resultado inferido por proximidade, geocoder ou polígono
presumido. A interface e os controles cartográficos aprovados não são alterados por esta decisão.

## 21. Evolução de escopo: fluxo urbano/rural explícito e catálogo de bairros de referência

Esta seção é a definição vigente para novas operações e substitui a interação de texto
territorial livre descrita na seção 20. A seção 20 permanece como histórico da etapa T29–T32
e como referência para compatibilidade dos registros/clientes legados. Não redesenhar o mapa:
os componentes cartográficos conservam dimensões, modo expandido, controles, estilos,
responsividade, busca, marcadores e comportamentos aprovados. A integração pode apenas ampliar
o evento tipado de reverse geocoding e a sincronização com o formulário.

### 21.1. Escolha do contexto territorial

O formulário usa um checkbox acessível, inicialmente desmarcado, com o rótulo **“A árvore está
localizada em zona rural”**. Desmarcado, apresenta o fluxo urbano e um campo obrigatório de
bairro; marcado, oculta esse seletor e apresenta a localidade rural textual obrigatória. CEP e
referência permanecem opcionais. Latitude/longitude continuam obrigatórias e são atualizadas
imediatamente pelo mapa. O geocoder nunca marca/desmarca o checkbox.

Ao alternar os modos, os valores mantidos no estado local não são descartados silenciosamente.
Ao montar o payload, somente `bairroId` é enviado para urbano e somente `localidadeRural` para
rural; campos incompatíveis são enviados como nulos. A validação exige que o contexto ativo
esteja completo. Na edição, a resposta leva `zonaRural=true` para `FORA_DA_AREA_URBANA`, `false`
para `BAIRRO_IDENTIFICADO` e `null` para `BAIRRO_NAO_IDENTIFICADO` legado. Uma ficha legada
desconhecida aparece no padrão urbano da interface, mas não pode ser salva sem seleção de
bairro ativo, evitando reclassificação implícita.

### 21.2. Seletor nativo e proveniência do catálogo

O formulário consulta o `GET /api/bairros` já existente, que retorna apenas bairros ativos em
ordem alfabética. O fluxo urbano apresenta somente um seletor nativo com o rótulo “Bairro *”
e a opção inicial “Selecione o bairro”; não há um campo de busca separado. O controle conserva
as interações de teclado do navegador. A seleção e o payload de cadastro/edição guardam o ID
numérico, nunca texto livre. A validação compara o ID selecionado com o conjunto de IDs ativos;
após uma seleção válida, remove o erro anterior desse campo. Um ID ausente ou não ativo continua
inválido. Na edição, a associação ativa existente é carregada no seletor depois que o catálogo
estiver disponível. Erros de catálogo permanecem distintos de lista vazia e impedem cadastro
urbano enquanto não houver opção válida. O endpoint legado e as associações existentes continuam
disponíveis.

Uma sugestão do geocoder pode selecionar automaticamente o ID do catálogo somente quando a
correspondência normalizada for única. Seleção manual mantém prioridade sobre respostas atrasadas.
No modo rural, o seletor urbano fica oculto e a localidade rural obrigatória permanece em uso.

Na página inicial, o filtro de bairro usa um seletor nativo de escolha única abastecido pelo
mesmo catálogo ativo e ordenado. A primeira opção é “Todos os bairros”; as demais usam o nome do
bairro tanto como valor de seleção quanto como texto exibido. Ao aplicar, o frontend transmite
esse nome pelo parâmetro textual `bairro` existente, não o ID. Escolher “Todos os bairros” omite
o parâmetro territorial e mantém pesquisa, espécie, condição e tamanho de página. Os mesmos
critérios são enviados aos endpoints da tabela e do mapa. O filtro textual do backend continua
compatível com nomes recuperados de associações legadas.

A carga de referência usa os 24 nomes do Plano Municipal da Primeira Infância de Quixadá,
publicado pela Prefeitura e exibido na tabela municipal com fonte indicada como IBGE 2023.
A lista consta na subseção 7.6 dos requisitos e é apenas um conjunto inicial de referência,
sujeito à validação da AMMA. A referência histórica da Lei Municipal 2.319/2007 relaciona 22
bairros, com “Carrascal” não dividido e sem “Residencial”; essa divergência é mantida explícita.
Não inserir distritos/comunidades rurais no catálogo de bairros urbanos.

O script manual de carga compara nomes normalizados sem distinção de acentuação, caixa ou
espaços repetidos e insere somente nomes sem equivalente, com `ativo=true`. Ele não renomeia,
reativa, remove ou troca IDs e relações existentes; não cria polígonos, coordenadas ou geometria
territorial. Não roda automaticamente pelo Spring Boot. A carga foi aplicada ao PostgreSQL local
de desenvolvimento após autorização operacional explícita, com backup, restrição de acesso,
autenticação SCRAM e conferências pós-carga reportadas; o backfill territorial não foi executado.
Os testes de idempotência/backfill em PostgreSQL descartável permanecem evidência separada. Esta
operação local não valida a lista institucionalmente nem autoriza outras execuções.

### 21.3. Contrato HTTP e compatibilidade backend

Os requests de cadastro/PUT acrescentam `zonaRural` e `localidadeRural`; `zonaRural` não nulo
ativa o novo contrato. `bairroId` é obrigatório em modo urbano e deve referenciar `Bairro.ativo`
verdadeiro. `localidadeRural` é obrigatória e não vazia em modo rural; a coluna é `TEXT` e esta
spec não define limite de tamanho institucional. `bairroId` e `bairroNome` não podem acompanhar
um request rural novo. CEP e referência continuam opcionais. Coordenadas e espécie mantêm os
contratos existentes.

O backend define a classificação persistida sem consultar o geocoder: urbano →
`BAIRRO_IDENTIFICADO`, rural → `FORA_DA_AREA_URBANA`. O nome territorial urbano é derivado do
registro `Bairro` ativo selecionado e os IDs ficam persistidos tanto no novo cadastro quanto em
edições urbanas. `localidadeRural` fica numa coluna textual nullable. Nenhuma tabela, FK, coluna
ou associação legada é removida.

Requests legados sem `zonaRural` continuam no validador e mapper de compatibilidade anteriores.
Responses mantêm `classificacaoTerritorial`, `bairro`, `bairroNome` e CEP e acrescentam
`zonaRural` (tri-state nullable para preservar `BAIRRO_NAO_IDENTIFICADO`) e `localidadeRural`.
DTOs resumidos fornecem os campos à tabela e popup sem alterar o layout do mapa; o texto
territorial exibido usa bairro urbano ou localidade rural sem apresentar localidade rural como
bairro.

Filtros existentes permanecem. `bairro` continua parcial, case-insensitive e combinado com os
demais critérios nas mesmas Specifications compartilhadas pela tabela e pelo mapa. Para novos
registros, a associação canônica e o nome correspondente são consultados juntos; para árvores
legadas, `bairroNome` e `bairro.nome` permanecem como fallback. `localidadeRural` não participa
do filtro de bairro.

### 21.4. Persistência e transição

A entidade `Arvore` mantém `Bairro bairro`, `bairro_id`, `bairroNome`, `cep` e o enum legado e
acrescenta `localidadeRural` nullable (`localidade_rural`). O SQL manual aditivo de T29 passa a
incluir essa coluna sem backfill presumido. As associações antigas não são apagadas e o novo
contrato não transforma um vínculo antigo em referência válida por coincidência de nome.

Para editar registro com associação ativa, o formulário seleciona seu ID. Quando a associação
está ausente/inativa, a UI mostra o nome legado como informação e exige que o operador escolha
um bairro ativo antes de salvar como urbano. Registros `FORA_DA_AREA_URBANA` sem localidade antiga
podem ser consultados; edição rural exige que o operador informe uma localidade. Registros
`BAIRRO_NAO_IDENTIFICADO` permanecem inalterados até escolha explícita e preenchimento compatível.
A carga do catálogo nunca associa automaticamente árvores antigas por similaridade textual.

Para uma execução futura em ambiente diferente ou após reconstrução do volume, exige-se nova
autorização e revisão operacional. Antes de executar: confirmar `current_database()`/schema,
contar nomes equivalentes e vínculos, registrar backup. Depois: confirmar quantidade de bairros,
ausência de duplicidades normalizadas, IDs antigos e FKs inalterados, e revisar os nomes com a
AMMA. Não repetir a carga no ambiente local já abastecido. O rollback não apaga linhas ou
relações: deve parar a aplicação e restaurar backup se a lista incorreta tiver sido usada; não
remover bairros referenciados por árvores.

### 21.5. Fluxos visuais e respostas geográficas

No modo urbano, o formulário mostra um único seletor nativo com opções ativas; não há campo de
busca separado. Uma sugestão reverse-geocoded de bairro é normalizada com NFD, remoção de
diacríticos, normalização de espaços e comparação de caixa; somente uma única correspondência
exata pelo nome canônico pode preencher o ID. Zero ou múltiplos resultados não mudam a seleção.
O operador pode escolher manualmente no seletor, inclusive com os recursos de teclado nativos
do navegador. A página inicial usa o mesmo catálogo num seletor distinto: envia o nome no
parâmetro textual `bairro`, enquanto a opção “Todos os bairros” omite o filtro.

No modo rural, o mesmo evento de geocodificação oferece uma sugestão textual de localidade quando
o provedor disponibiliza locality/place/neighbourhood. O operador pode aceitar ou editar. Não se
classifica o ponto por proximidade, nome de distrito, CEP ou mapa. A escolha do modo e edições
manuais realizadas durante uma requisição invalidam somente as sugestões correspondentes; uma
resposta tardia não sobrescreve campo alterado. Coordenadas emitidas pelo MapLibre são imediatas,
mesmo que o geocoder falhe, esteja indisponível ou não retorne resultado.

### 21.6. Critérios e testes da evolução

CA33–CA41 cobrem catálogo ativo, seleção por ID, validação urbana/rural, sugestões sem inferência,
normalização unívoca, idempotência da carga, preservação de legados, edição, validação do seletor
e consistência dos filtros mapa/lista. Testes de serviço validam estado ativo e campos cruzados;
integração com PostgreSQL descartável exercita contratos e executa a carga mais de uma vez. Testes
frontend montam o formulário real e integram o evento real do mapa em host de teste, cobrindo
alternância, seleção, retorno atrasado, dados legados, validação, payload e filtros. O usuário
confirmou no navegador que as alterações visuais da T37 estavam corretas; isso é evidência de
aceitação visual somente das modificações verificadas, não substitui os fluxos fullstack ainda
pendentes, a verificação com MapTiler real ou a validação de teclado não especificamente
reportada.

### 21.7. Registro da fonte

- PMPI Quixadá 2022–2031, tabela de divisão territorial, “24 bairros”, fonte indicada “IBGE 2023”,
  publicado pela Prefeitura: `https://quixada.ce.gov.br/conselhoarquivo/1/302/planomunicipalpelaprimeirainfancia.pdf`,
  consultado em 2026-11-01.
- CREDE12/SEDUC-CE, página histórica com texto das Leis Municipais 1.863/1999 e 2.319/2007,
  publicada em 2016: `https://www.ce.gov.br/crede12-seduc/2016/02/25/quixada-historico/`,
  consultada em 2026-11-01. É referência histórica secundária, não homologação da AMMA.
