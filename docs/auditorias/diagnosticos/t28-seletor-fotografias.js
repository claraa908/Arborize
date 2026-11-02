(() => {
  window.arborizeT28?.parar()
  const input = document.getElementById('arvore-fotografias-cadastro-seletor')
  if (location.pathname !== '/sistema/inventario/nova' || !(input instanceof HTMLInputElement)) {
    throw new Error('Abra o cadastro de árvore com o seletor de fotografias disponível.')
  }
  const label = input.labels?.[0] ?? input.closest('label')
  const elementos = new Set([input, label])
  for (let elemento = input.parentElement; elemento; elemento = elemento.parentElement) {
    elementos.add(elemento)
  }
  elementos.add(input.closest('form')?.querySelector('section[aria-label="Localização da árvore no mapa"]'))
  const identificar = (elemento) => elemento instanceof Element
    ? `${elemento.localName}${elemento.id ? `#${elemento.id}` : ''}`
    : elemento === window ? 'window' : elemento === document ? 'document' : 'nenhum'
  const arredondar = (numero) => Math.round(numero * 10) / 10
  const controle = new AbortController()
  const dados = []
  const anteriores = new Map()
  const inicio = performance.now()
  let interagindo = false
  let encerrado = false
  let scrollAgendado = false
  let temporizador

  function capturar(etapa, alvo = null) {
    if (encerrado) return
    const alteracoes = [...elementos].filter(Boolean).flatMap((elemento, indice) => {
      const r = elemento.getBoundingClientRect()
      const css = getComputedStyle(elemento)
      const leitura = {
        elemento: `${indice}:${identificar(elemento)}`, conectado: elemento.isConnected,
        classe: elemento.className, x: arredondar(r.x), y: arredondar(r.y),
        largura: arredondar(r.width), altura: arredondar(r.height),
        scrollTop: elemento.scrollTop, scrollLeft: elemento.scrollLeft,
        scrollHeight: elemento.scrollHeight, clientHeight: elemento.clientHeight,
        position: css.position, overflowY: css.overflowY, display: css.display,
        clip: css.clip, clipPath: css.clipPath, visibility: css.visibility,
        height: css.height, minHeight: css.minHeight, flex: css.flex,
        transform: css.transform, overflowAnchor: css.overflowAnchor,
        offsetParent: identificar(elemento.offsetParent),
      }
      const serializado = JSON.stringify(leitura)
      if (anteriores.get(elemento) === serializado) return []
      anteriores.set(elemento, serializado)
      return [leitura]
    })
    const registro = {
      etapa, ms: Math.round(performance.now() - inicio), alvo: identificar(alvo),
      foco: identificar(document.activeElement), arquivos: input.files?.length ?? 0,
      inputsComMesmoId: document.querySelectorAll('[id="arvore-fotografias-cadastro-seletor"]').length,
      janela: { x: scrollX, y: scrollY, largura: innerWidth, altura: innerHeight,
        visualTop: visualViewport?.offsetTop, visualHeight: visualViewport?.height },
      alteracoes,
    }
    dados.push(registro)
    console.log('[Arborize T28]', JSON.stringify(registro))
    if (dados.length >= 80) parar()
  }

  function parar() {
    encerrado = true
    controle.abort()
    clearTimeout(temporizador)
  }

  function observar(evento) {
    const alvo = evento.target
    if (evento.type === 'pointerdown' || evento.type === 'click') {
      if (alvo !== input && !(alvo instanceof Node && label?.contains(alvo))) return
      interagindo = true
    }
    if (evento.type === 'focusin' && alvo === input) interagindo = true
    if (!interagindo) return
    if (evento.type === 'scroll' || evento.type === 'wheel') {
      if (scrollAgendado) return
      scrollAgendado = true
      requestAnimationFrame(() => {
        scrollAgendado = false
        capturar(evento.type, alvo)
      })
      return
    }
    capturar(evento.type, alvo)
    requestAnimationFrame(() => capturar(`${evento.type}/quadro`, alvo))
    setTimeout(() => capturar(`${evento.type}/250ms`, alvo), 250)
  }

  const opcoes = { capture: true, passive: true, signal: controle.signal }
  for (const evento of ['pointerdown', 'click', 'focusin', 'focusout', 'change', 'cancel', 'scroll', 'wheel']) {
    document.addEventListener(evento, observar, opcoes)
  }
  window.addEventListener('focus', observar, opcoes)
  window.addEventListener('blur', observar, opcoes)
  window.arborizeT28 = { dados, capturar, parar }
  capturar('antes')
  temporizador = setTimeout(parar, 120000)
  console.info('[Arborize T28] Pronto. Clique em Selecionar imagens e cancele a janela primeiro.')
})()
