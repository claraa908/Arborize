import type { QuizQuestion } from '@/types/assistentePlantio'

export const assistentePlantioQuestions: QuizQuestion[] = [
  {
    id: 'largura_calcada',
    prompt: 'Qual a largura da sua calçada?',
    helpText: 'Informe a largura livre aproximada da calçada.',
    options: [
      { label: 'Menor que 1,5m', description: 'Calçada estreita.', value: 'MENOR_1_5M' },
      { label: 'Entre 1,5m e 2,0m', description: 'Calçada com largura entre 1,5m e 2,0m.', value: 'ENTRE_1_5M_E_2M' },
      { label: 'Entre 2,0m e 2,4m', description: 'Calçada com largura entre 2,0m e 2,4m.', value: 'ENTRE_2M_E_2_4M' },
      { label: 'Maior que 2,4m', description: 'Calçada com mais de 2,4m de largura.', value: 'MAIOR_2_4M' }
    ]
  },
  {
    id: 'fiacao_eletrica',
    prompt: 'Existe fiação elétrica aérea diretamente sobre o local?',
    helpText: 'Observe se há fiação passando acima do ponto de plantio.',
    options: [
      { label: 'Sim, há fiação', description: 'Há fiação elétrica aérea sobre o local.', value: 'SIM' },
      { label: 'Não há fiação', description: 'Não há fiação elétrica aérea sobre o local.', value: 'NAO' }
    ]
  },
  {
    id: 'local_plantio',
    prompt: 'Onde a muda será plantada?',
    helpText: 'Escolha o ambiente principal de plantio.',
    options: [
      { label: 'Calçada / Via pública', description: 'Em frente a uma residência ou comércio.', value: 'CALCADA' },
      { label: 'Quintal / Área interna residencial', description: 'Em uma área particular com acesso ao solo.', value: 'QUINTAL' }
    ]
  },
  {
    id: 'recuo_construcao',
    prompt: 'Qual a distância até a construção/muro mais próximo?',
    helpText: 'Considere a distância entre o local de plantio e a construção ou muro.',
    options: [
      { label: 'Até 2 metros', description: 'O local fica a até 2 metros da construção ou muro.', value: 'ATE_2M' },
      { label: 'De 2 a 4 metros', description: 'O local fica entre 2 e 4 metros da construção ou muro.', value: 'ENTRE_2M_E_4M' },
      { label: 'Mais de 4 metros', description: 'O local fica a mais de 4 metros da construção ou muro.', value: 'MAIOR_4M' }
    ]
  },
  {
    id: 'tipo_solo',
    prompt: 'Como é a condição de solo e irrigação do local?',
    helpText: 'Escolha a opção que melhor descreve o solo e a água disponível.',
    options: [
      { label: 'Solo seco/raso (padrão Caatinga)', description: 'Solo seco, raso ou com pouca profundidade.', value: 'SECO_RASO' },
      { label: 'Local com disponibilidade diária de irrigação', description: 'É possível irrigar o local todos os dias.', value: 'COM_IRRIGACAO' }
    ]
  },
  {
    id: 'objetivo',
    prompt: 'Qual o objetivo principal do plantio?',
    helpText: 'Escolha o benefício mais importante para você.',
    options: [
      { label: 'Sombra e conforto térmico', description: 'Criar sombra e amenizar o calor.', value: 'SOMBRA' },
      { label: 'Produção de frutos', description: 'Ter frutos para consumo ou fauna.', value: 'FRUTAS' },
      { label: 'Atração de abelhas nativas e fauna', description: 'Favorecer polinizadores e outros animais.', value: 'POLINIZADORES' },
      { label: 'Restauração ambiental', description: 'Contribuir com a recuperação ambiental.', value: 'RESTAURACAO' }
    ]
  },
  {
    id: 'disponibilidade_rega',
    prompt: 'Tem disponibilidade para regar 2 a 3 vezes por semana nos primeiros 60 dias?',
    helpText: 'A rega inicial ajuda a muda a se estabelecer.',
    options: [
      { label: 'Sim', description: 'Posso regar a muda nesse período.', value: 'SIM' },
      { label: 'Não', description: 'Não tenho disponibilidade para regar nesse período.', value: 'NAO' }
    ]
  },
  {
    id: 'piso_drenante',
    prompt: 'Existe área permeável/terra exposta de pelo menos 1m²?',
    helpText: 'Considere o espaço de terra que permite a infiltração da água.',
    options: [
      { label: 'Sim', description: 'Há pelo menos 1m² de área permeável.', value: 'SIM' },
      { label: 'Não (precisará de abertura de berço/piso)', description: 'Será necessário abrir espaço no piso para o plantio.', value: 'NAO' }
    ]
  },
  {
    id: 'substituicao_nim',
    prompt: 'O plantio é para substituir uma árvore de Nim Indiano suprimida?',
    helpText: 'Nim Indiano é uma espécie exótica invasora.',
    options: [
      { label: 'Sim', description: 'A muda substituirá um Nim Indiano removido.', value: 'SIM' },
      { label: 'Não', description: 'O plantio não substitui um Nim Indiano.', value: 'NAO' }
    ]
  },
  {
    id: 'quantidade_mudas',
    prompt: 'Quantas mudas você deseja solicitar?',
    helpText: 'Escolha a quantidade desejada para este local.',
    options: [
      { label: '1 muda', description: 'Uma muda.', value: 'UMA' },
      { label: '2 mudas', description: 'Duas mudas.', value: 'DUAS' },
      { label: '3 mudas', description: 'Três mudas.', value: 'TRES' }
    ]
  }
]
