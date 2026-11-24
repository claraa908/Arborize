import {
  ClipboardList,
  CloudSun,
  Droplet,
  Eye,
  HeartHandshake,
  MapPin,
  Mountain,
  Recycle,
  ShieldCheck,
  Shovel,
  Sprout,
  Trees,
} from 'lucide-vue-next'
import type { Component } from 'vue'

interface HomeTopic {
  id: string
  icon: Component
  title: string
  text: string
}

export const requestSteps: readonly HomeTopic[] = [
  {
    id: 'descubra',
    icon: Trees,
    title: 'Descubra',
    text: 'Responda ao quiz para encontrar uma árvore adequada ao seu espaço.',
  },
  {
    id: 'peca',
    icon: ClipboardList,
    title: 'Peça',
    text: 'Preencha o formulário e confira a conversão em recicláveis antes de enviar.',
  },
  {
    id: 'entregue',
    icon: Recycle,
    title: 'Entregue os recicláveis',
    text: 'Confira no seu pedido a indicação do Eco Ponto ou local de entrega.',
  },
  {
    id: 'retire',
    icon: Sprout,
    title: 'Retire sua muda e plante',
    text: 'Acompanhe o pedido para saber quando retirar a muda.',
  },
]

export const plantingSteps: readonly HomeTopic[] = [
  {
    id: 'local',
    icon: MapPin,
    title: 'Escolha o lugar certo',
    text: 'Reserve espaço para a árvore adulta, longe de fios, muros e canos.',
  },
  {
    id: 'cova',
    icon: Shovel,
    title: 'Prepare a cova',
    text: 'Abra uma cova larga e funda, solte a terra e misture composto orgânico se tiver.',
  },
  {
    id: 'plantio',
    icon: Sprout,
    title: 'Plante com cuidado',
    text: 'Retire a embalagem sem desfazer o torrão. Deixe a base do tronco no nível do chão.',
  },
  {
    id: 'rega',
    icon: Droplet,
    title: 'Regue bem',
    text: 'Regue bem no plantio. Depois, faça regas profundas e espaçadas, conforme a necessidade da muda.',
  },
  {
    id: 'protecao',
    icon: ShieldCheck,
    title: 'Proteja',
    text: 'Use tutor e garajau. Cubra o solo com folhas secas, deixando espaço junto ao tronco.',
  },
  {
    id: 'cuidados',
    icon: Eye,
    title: 'Acompanhe',
    text: 'Nos primeiros 12 a 24 meses, observe as regas, ervas daninhas e pragas. Avise a AMMA se a muda murchar ou sofrer danos.',
  },
]

export const tipCategories = {
  cuidados: { label: 'Cuidados com a muda', icon: HeartHandshake },
  solo: { label: 'Conheça seu solo', icon: Mountain },
  clima: { label: 'Clima e época', icon: CloudSun },
  nativas: { label: 'Árvores nativas', icon: Trees },
} as const

export type TipCategory = keyof typeof tipCategories

interface CaatingaTip extends Omit<HomeTopic, 'icon'> {
  category: TipCategory
  link?: {
    label: string
    route: 'biblioteca-catalogo' | 'arborizacao-substituicao-nim'
  }
}

export const caatingaTips: readonly CaatingaTip[] = [
  {
    id: 'rega',
    category: 'cuidados',
    title: 'Regue em profundidade',
    text: 'Faça regas profundas e espaçadas, conforme a necessidade da muda.',
  },
  {
    id: 'cobertura',
    category: 'cuidados',
    title: 'Cubra o solo',
    text: 'Folhas secas ajudam a manter a umidade. Deixe espaço junto ao tronco.',
  },
  {
    id: 'garajau',
    category: 'cuidados',
    title: 'Evite o pisoteio',
    text: 'O garajau protege a muda do pisoteio e de danos causados por animais.',
  },
  {
    id: 'danos',
    category: 'cuidados',
    title: 'Avise a AMMA em caso de dano',
    text: 'Se houver galhos quebrados ou outros danos, procure a AMMA antes de podar.',
  },
  {
    id: 'arenoso',
    category: 'solo',
    title: 'Solo arenoso e solto',
    text: 'A água escoa rápido. Observe a umidade, cubra o solo e ajuste o intervalo das regas se necessário.',
  },
  {
    id: 'fofo',
    category: 'solo',
    title: 'Solo escuro e fofo',
    text: 'A terra solta favorece as raízes. Regue com cuidado para evitar encharcamento.',
  },
  {
    id: 'compactado',
    category: 'solo',
    title: 'Solo duro ou compactado',
    text: 'Afrouxe a terra e abra uma cova mais larga.',
  },
  {
    id: 'chuvas',
    category: 'clima',
    title: 'Aproveite o início das chuvas',
    text: 'No semiárido, plantar no início das chuvas ajuda a garantir água enquanto a muda se estabelece.',
  },
  {
    id: 'horarios',
    category: 'clima',
    title: 'Prefira horas mais frescas',
    text: 'Em dias de sol forte, cuide da muda no início da manhã ou no fim da tarde.',
  },
  {
    id: 'fauna',
    category: 'nativas',
    title: 'Dê preferência às nativas',
    text: 'Espécies da Caatinga são adaptadas ao clima da região e favorecem a fauna local.',
    link: { label: 'Acessar Biblioteca Digital', route: 'biblioteca-catalogo' },
  },
  {
    id: 'fiacao',
    category: 'nativas',
    title: 'Observe a fiação',
    text: 'Se houver fiação por perto e o local for adequado, escolha uma espécie de pequeno porte indicada pelo assistente.',
  },
  {
    id: 'nim',
    category: 'nativas',
    title: 'Substituição do Nim Indiano',
    text: 'O Nim Indiano é uma espécie exótica invasora. A AMMA pode avaliar a substituição.',
    link: { label: 'Solicitar avaliação do Nim', route: 'arborizacao-substituicao-nim' },
  },
]
