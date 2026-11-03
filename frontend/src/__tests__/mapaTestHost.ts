import { createRenderer, markRaw } from 'vue'

export class ElementoTeste {
  children: ElementoTeste[] = []
  parent: ElementoTeste | null = null
  props: Record<string, unknown> = {}
  style: Record<string, string> = {}
  atributos = new Map<string, string>()
  listeners = new Map<string, Array<(event: unknown) => void>>()
  classes = new Set<string>()
  classList = {
    add: (valor: string) => this.classes.add(valor),
    remove: (valor: string) => this.classes.delete(valor),
    contains: (valor: string) => this.classes.has(valor),
    toggle: (valor: string, ativo: boolean) =>
      ativo ? this.classes.add(valor) : this.classes.delete(valor),
  }
  value: unknown = ''
  selected = false
  multiple = false
  open = false
  focado = false
  rolamentos: unknown[] = []
  text = ''
  constructor(readonly tagName: string) {
    markRaw(this)
  }
  get type() {
    return this.props.type ?? ''
  }
  set type(valor: unknown) {
    this.props.type = valor
  }
  getRootNode() {
    return document
  }
  getClientRects() {
    return [{}]
  }
  querySelectorAll(seletor: string): ElementoTeste[] {
    if (!seletor.startsWith('button:not([disabled])')) return []
    const encontrados: ElementoTeste[] = []
    const visitar = (no: ElementoTeste) => {
      if (
        (['BUTTON', 'INPUT'].includes(no.tagName) && !no.props.disabled) ||
        (no.tagName === 'A' && no.props.href) ||
        no.props.tabindex === '0'
      )
        encontrados.push(no)
      no.children.forEach(visitar)
    }
    this.children.forEach(visitar)
    return encontrados
  }
  focus() {
    this.focado = true
    ;(document as unknown as { activeElement: ElementoTeste }).activeElement = this
  }
  scrollIntoView(opcoes: unknown) {
    this.rolamentos.push(opcoes)
  }
  showModal() {
    this.open = true
  }
  close() {
    this.open = false
  }
  querySelector(seletor: string): ElementoTeste | null {
    try {
      return encontrar(this, (no) => seletor.startsWith('#') && no.props.id === seletor.slice(1))
    } catch {
      return null
    }
  }
  get options() {
    return this.children.filter((filho) => filho.tagName === 'OPTION')
  }
  get textContent(): string {
    return this.text + this.children.map((filho) => filho.textContent).join('')
  }
  set textContent(valor: string) {
    this.text = valor
    this.children = []
  }
  addEventListener(nome: string, listener: (event: unknown) => void) {
    this.listeners.set(nome, [...(this.listeners.get(nome) ?? []), listener])
  }
  removeEventListener(nome: string, listener: (event: unknown) => void) {
    this.listeners.set(
      nome,
      (this.listeners.get(nome) ?? []).filter((item) => item !== listener),
    )
  }
  setAttribute(nome: string, valor: string) {
    this.atributos.set(nome, valor)
  }
  getAttribute(nome: string) {
    return this.atributos.get(nome) ?? null
  }
  hasAttribute(nome: string) {
    return this.atributos.has(nome)
  }
  removeAttribute(nome: string) {
    this.atributos.delete(nome)
  }
  appendChild(filho: ElementoTeste) {
    filho.remove()
    filho.parent = this
    this.children.push(filho)
  }
  replaceChildren(...filhos: ElementoTeste[]) {
    this.children.slice().forEach((filho) => filho.remove())
    filhos.forEach((filho) => this.appendChild(filho))
  }
  remove() {
    if (this.parent) this.parent.children = this.parent.children.filter((filho) => filho !== this)
    this.parent = null
  }
  dispatch(nome: string, extras: Record<string, unknown> = {}) {
    const event = { target: this, preventDefault() {}, stopPropagation() {}, ...extras }
    for (const listener of this.listeners.get(nome) ?? []) listener(event)
    const propriedade = `on${nome[0]!.toUpperCase()}${nome.slice(1)}`
    const captura = this.props[`${propriedade}Capture`] as ((event: unknown) => unknown) | undefined
    captura?.(event)
    const handler = this.props[propriedade] as ((event: unknown) => unknown) | undefined
    return handler?.(event)
  }
}

export const renderer = createRenderer<ElementoTeste, ElementoTeste>({
  createElement: (nome) => new ElementoTeste(nome.toUpperCase()),
  createText: (texto) => Object.assign(new ElementoTeste('#text'), { text: texto }),
  createComment: () => new ElementoTeste('#comment'),
  setText: (no, texto) => {
    no.text = texto
  },
  setElementText: (no, texto) => {
    no.text = texto
    no.children = []
  },
  patchProp(no, chave, _anterior, valor) {
    no.props[chave] = valor
    if (chave === 'value') no.value = valor
  },
  insert(no, pai, ancora) {
    no.remove()
    no.parent = pai
    const indice = ancora ? pai.children.indexOf(ancora) : -1
    if (indice < 0) pai.children.push(no)
    else pai.children.splice(indice, 0, no)
  },
  remove: (no) => no.remove(),
  parentNode: (no) => no.parent,
  nextSibling: (no) => no.parent?.children[no.parent.children.indexOf(no) + 1] ?? null,
  setScopeId: () => undefined,
  querySelector: (seletor) =>
    seletor === 'body' ? (document.body as unknown as ElementoTeste) : null,
})

export function encontrar(
  no: ElementoTeste,
  condicao: (no: ElementoTeste) => boolean,
): ElementoTeste {
  if (condicao(no)) return no
  for (const filho of no.children) {
    try {
      return encontrar(filho, condicao)
    } catch {
      continue
    }
  }
  throw new Error('Elemento não encontrado no host de teste')
}
