/**
 * Fruki - Canal WhatsApp Mobile com Catálogo Estilo WhatsApp Business (JioMart)
 * Fluxo de Checkout em 2 Etapas:
 *   1. Opções de Up-Sell (Volume) & Cross-Sell
 *   2. Confirmação do Carrinho (Revisão & Envio POST /api/pedidos)
 *
 * Endpoints consumidos exclusivamente:
 * - GET /api/clientes/busca?cnpj={cnpj}
 * - GET /api/recomendacoes/{clienteId}?cidade={cidade}
 * - POST /api/pedidos
 */

const estado = {
    cliente: null,
    recomendacoes: [],
    catalogo: [],
    categoriaAtiva: 'PARA_VOCE', // Primeira aba/categoria ativa por padrão
    carrinho: new Map(), // produtoId -> { produto: RecomendacaoDTO, quantidade: number }
    termoBusca: '',
    etapaCheckout: 'UPSELL', // 'UPSELL' | 'CONFIRMACAO'
    sugestoesCheckout: [] // 4 produtos recomendados para a etapa de sugestões
};

const dom = {
    chatMessages: document.getElementById('chat-messages'),
    dynamicMessages: document.getElementById('dynamic-messages'),
    timeSaudacao: document.getElementById('time-saudacao'),
    inputCnpj: document.getElementById('input-cnpj'),
    btnEnviarCnpj: document.getElementById('btn-enviar-cnpj'),
    btnAtalhoCatalogo: document.getElementById('btn-atalho-catalogo'),
    modalCatalogoTop: document.getElementById('modal-catalogo-top'),
    btnFecharCatalogoTopo: document.getElementById('btn-fechar-catalogo-topo'),
    btnNavBusca: document.getElementById('btn-nav-busca'),
    waCatalogSearchBar: document.getElementById('wa-catalog-search-bar'),
    inputBuscaProdutos: document.getElementById('input-busca-produtos'),
    btnLimparBusca: document.getElementById('btn-limpar-busca'),
    btnNavCarrinho: document.getElementById('btn-nav-carrinho'),
    navCartBadge: document.getElementById('nav-cart-badge'),
    btnNavMenu: document.getElementById('btn-nav-menu'),
    catalogDeliveryClient: document.getElementById('catalog-delivery-client'),
    catalogDeliveryCity: document.getElementById('catalog-delivery-city'),
    btnVerTodasCat: document.getElementById('btn-ver-todas-cat'),
    catCards: document.querySelectorAll('.wa-cat-card'),
    activeCategoryTitle: document.getElementById('active-category-title'),
    activeCategoryCount: document.getElementById('active-category-count'),
    productsList: document.getElementById('wa-products-list'),
    cartFloat: document.getElementById('wa-cart-float'),
    cartFloatQty: document.getElementById('cart-float-qty'),
    cartFloatTotal: document.getElementById('cart-float-total'),
    btnAbrirCheckout: document.getElementById('btn-abrir-checkout'),

    // Elementos do Modal de Checkout (2 Etapas)
    modalCheckout: document.getElementById('modal-checkout'),
    btnFecharCheckout: document.getElementById('btn-fechar-checkout'),
    indicatorStep1: document.getElementById('indicator-step-1'),
    indicatorStep2: document.getElementById('indicator-step-2'),
    checkoutViewUpsell: document.getElementById('checkout-view-upsell'),
    checkoutViewConfirmacao: document.getElementById('checkout-view-confirmacao'),
    upsellPreviewTotal: document.getElementById('upsell-preview-total'),
    btnAvancarConfirmacao: document.getElementById('btn-avancar-confirmacao'),
    checkoutSugestoesList: document.getElementById('checkout-sugestoes-list'),
    confirmClienteNome: document.getElementById('confirm-cliente-nome'),
    confirmClienteCnpj: document.getElementById('confirm-cliente-cnpj'),
    confirmClienteCidade: document.getElementById('confirm-cliente-cidade'),
    checkoutItensList: document.getElementById('checkout-itens-list'),
    checkoutSubtotal: document.getElementById('checkout-subtotal'),
    checkoutFinalTotal: document.getElementById('checkout-final-total'),
    btnVoltarUpsell: document.getElementById('btn-voltar-upsell'),
    btnConcluirPedido: document.getElementById('btn-concluir-pedido'),
    checkoutFeedback: document.getElementById('checkout-feedback')
};

const formatadorMoeda = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
});

function formatarPreco(valor) {
    if (valor === null || valor === undefined || Number.isNaN(Number(valor))) {
        return 'Sob consulta';
    }
    return formatadorMoeda.format(Number(valor));
}

function obterHoraAtual() {
    const agora = new Date();
    return agora.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

function escaparHtml(texto) {
    const div = document.createElement('div');
    div.textContent = texto ?? '';
    return div.innerHTML;
}

function scrollParaFinalDoChat() {
    setTimeout(() => {
        dom.chatMessages.scrollTop = dom.chatMessages.scrollHeight;
    }, 60);
}

/**
 * 1. Balões do Chat estilo WhatsApp
 */
function adicionarMensagem(remetente, conteudoHtml) {
    const row = document.createElement('div');
    row.className = `wa-message-row ${remetente}`;

    const bubble = document.createElement('div');
    bubble.className = 'wa-bubble';

    const ticks = remetente === 'user' ? '<span class="wa-msg-ticks">✔✔</span>' : '';
    bubble.innerHTML = `
        ${conteudoHtml}
        <span class="wa-msg-time">${obterHoraAtual()} ${ticks}</span>
    `;

    row.appendChild(bubble);
    dom.dynamicMessages.appendChild(row);
    scrollParaFinalDoChat();
    return row;
}

function adicionarAvisoDigitando() {
    const row = document.createElement('div');
    row.className = 'wa-message-row bot';
    row.id = 'typing-indicator';
    row.innerHTML = `
        <div class="wa-bubble">
            <span class="wa-typing-pill">Fruki digitando...</span>
        </div>
    `;
    dom.dynamicMessages.appendChild(row);
    scrollParaFinalDoChat();
    return row;
}

function removerAvisoDigitando() {
    const el = document.getElementById('typing-indicator');
    if (el) el.remove();
}

/**
 * 2. Processa o CNPJ digitado
 */
async function processarEnvioCnpj() {
    const valorDigitado = dom.inputCnpj.value.trim();
    if (!valorDigitado) return;

    dom.inputCnpj.value = '';
    dom.inputCnpj.disabled = true;
    dom.btnEnviarCnpj.disabled = true;

    adicionarMensagem('user', `<p>CNPJ: <strong>${escaparHtml(valorDigitado)}</strong></p>`);
    adicionarAvisoDigitando();

    try {
        const url = `/api/clientes/busca?cnpj=${encodeURIComponent(valorDigitado)}`;
        const resposta = await fetch(url);

        removerAvisoDigitando();

        if (!resposta.ok) {
            adicionarMensagem('bot', `
                <p>❌ <strong>CNPJ não encontrado</strong> em nossa base de clientes parceiros.</p>
                <p style="margin-top: 6px;">
                    Por favor, verifique os números digitados e tente novamente. Digite o seu <strong>CNPJ</strong>:
                </p>
            `);
            reiniciarProcessoIdentificacao();
            return;
        }

        const cliente = await resposta.json();
        estado.cliente = cliente;
        estado.carrinho.clear();
        atualizarResumoCarrinho();

        // Pré-carrega o catálogo da API com limite amplo (até 100 itens)
        await carregarCatalogoRecomendado(cliente);

        // Balão Interativo com o Botão "Ver Catálogo"
        const cidadeTexto = cliente.cidade ? ` em <strong>${escaparHtml(cliente.cidade)}</strong>` : '';
        const totalItens = estado.recomendacoes.length;

        const rowMensagem = adicionarMensagem('bot', `
            <div class="wa-interactive-card">
                <p>Que bom ter você por aqui, <strong>${escaparHtml(cliente.nome)}</strong>! 🥤</p>
                <p style="margin-top: 3px;">Identificamos o seu cadastro comercial${cidadeTexto}.</p>
                <p style="margin-top: 5px;">
                    Seu catálogo de hoje foi preparado com ofertas de reposição, fatores climáticos e promoções vigentes.
                </p>
                <div class="wa-interactive-btn-wrap">
                    <button type="button" class="wa-btn-view-catalog">
                        🛍️ <strong>Ver Catálogo</strong> (${totalItens} ${totalItens === 1 ? 'item' : 'itens'})
                    </button>
                </div>
            </div>
        `);

        const btnChat = rowMensagem.querySelector('.wa-btn-view-catalog');
        if (btnChat) {
            btnChat.addEventListener('click', abrirCatalogoHover);
        }

        dom.btnAtalhoCatalogo.classList.remove('hidden');
        dom.inputCnpj.placeholder = `Atendimento ativo para ${cliente.nome}`;

    } catch (erro) {
        removerAvisoDigitando();
        adicionarMensagem('bot', `
            <p>⚠️ Ocorreu uma instabilidade na consulta: ${escaparHtml(erro.message)}.</p>
            <p style="margin-top: 4px;">Por favor, digite seu CNPJ novamente:</p>
        `);
        reiniciarProcessoIdentificacao();
    } finally {
        dom.inputCnpj.disabled = false;
        dom.btnEnviarCnpj.disabled = false;
    }
}

function reiniciarProcessoIdentificacao() {
    estado.cliente = null;
    estado.recomendacoes = [];
    estado.catalogo = [];
    estado.carrinho.clear();
    atualizarResumoCarrinho();
    dom.btnAtalhoCatalogo.classList.add('hidden');
    fecharCatalogoHover();
    fecharCheckout();
    dom.inputCnpj.placeholder = 'Digite seu CNPJ (apenas números ou formatado)...';
    dom.inputCnpj.focus();
}

/**
 * 3. Consulta de Recomendações na API
 */
async function carregarCatalogoRecomendado(cliente) {
    dom.catalogDeliveryClient.textContent = `Entregar para ${cliente.nome}`;
    dom.catalogDeliveryCity.textContent = cliente.cidade ? `📍 ${cliente.cidade} • Entrega programada` : '📍 Região Sul • Entrega programada';

    try {
        const params = new URLSearchParams();
        if (cliente.cidade) {
            params.set('cidade', cliente.cidade);
        }
        params.set('limite', '100');
        const query = `?${params.toString()}`;
        const resposta = await fetch(`/api/recomendacoes/${encodeURIComponent(cliente.id)}${query}`);

        if (!resposta.ok) {
            throw new Error(`Status ${resposta.status}`);
        }

        const dados = await resposta.json();
        estado.recomendacoes = Array.isArray(dados) ? dados : [];

        // A aba "Mais Produtos" representa o catálogo disponível, não apenas o ranking recomendado.
        try {
            const respostaCatalogo = await fetch('/api/produtos/catalogo');
            if (respostaCatalogo.ok) {
                const catalogo = await respostaCatalogo.json();
                estado.catalogo = Array.isArray(catalogo) ? catalogo : estado.recomendacoes;
            } else {
                estado.catalogo = estado.recomendacoes;
            }
        } catch {
            estado.catalogo = estado.recomendacoes;
        }
        renderizarProdutosDaCategoriaAtiva();
    } catch (erro) {
        dom.productsList.innerHTML = `
            <div class="wa-product-row" style="text-align: center; color: var(--wa-text-secondary); padding: 20px;">
                Não foi possível carregar o catálogo (${escaparHtml(erro.message)}).
            </div>
        `;
    }
}

/**
 * 4. Controle do Catálogo Slide-Down
 */
function abrirCatalogoHover() {
    dom.modalCatalogoTop.classList.remove('hidden');
}

function fecharCatalogoHover() {
    dom.modalCatalogoTop.classList.add('hidden');
}

/**
 * 5. Categorização Exata: "Para Você", "Promoções", "Histórico", "Mais"
 * Primeira aba/categoria sempre "Para Você", seguida de "Promoções", "Histórico", "Mais".
 */
function obterItensDaCategoria(categoriaKey) {
    let filtrados = [];

    if (categoriaKey === 'PROMOCOES') {
        filtrados = estado.catalogo.filter(item =>
            Boolean(item.emPromocao) ||
            (item.promocaoId !== null && item.promocaoId !== undefined) ||
            (Array.isArray(item.pilares) && item.pilares.includes('PROMOCAO'))
        );
    } else if (categoriaKey === 'HISTORICO') {
        const compradosDiretos = estado.recomendacoes.filter(item => Boolean(item.jaComprado));
        if (compradosDiretos.length > 0) {
            filtrados = compradosDiretos;
        } else {
            filtrados = estado.recomendacoes.filter(item =>
                Array.isArray(item.pilares) && item.pilares.includes('HISTORICO')
            );
        }
    } else if (categoriaKey === 'PARA_VOCE') {
        // Primeira aba/categoria: Cross-sell, promoções e top recomendações personalizadas
        const destaques = estado.recomendacoes.filter(item => Boolean(item.isCrossSell) || Boolean(item.emPromocao));
        filtrados = (destaques.length > 0 ? destaques : estado.recomendacoes).slice(0, 15);
    } else if (categoriaKey === 'MAIS') {
        filtrados = estado.catalogo;
    } else {
        filtrados = estado.recomendacoes;
    }

    // Aplica filtro de pesquisa se digitado
    if (estado.termoBusca) {
        const termo = estado.termoBusca.toLowerCase();
        filtrados = filtrados.filter(item => 
            (item.nomeProduto && item.nomeProduto.toLowerCase().includes(termo)) ||
            (item.categoria && item.categoria.toLowerCase().includes(termo))
        );
    }

    return filtrados;
}

function selecionarCategoria(catKey) {
    estado.categoriaAtiva = catKey;
    dom.catCards.forEach(card => {
        if (card.dataset.categoria === catKey) {
            card.classList.add('active');
        } else {
            card.classList.remove('active');
        }
    });

    const titulos = {
        'PARA_VOCE': 'Para Você',
        'PROMOCOES': 'Promoções',
        'HISTORICO': 'Histórico',
        'MAIS': 'Mais Produtos'
    };
    dom.activeCategoryTitle.textContent = titulos[catKey] || 'Produtos';
    renderizarProdutosDaCategoriaAtiva();
}

/**
 * 6. Renderização dos Produtos do Catálogo
 */
function renderizarProdutosDaCategoriaAtiva() {
    dom.productsList.innerHTML = '';
    const produtos = obterItensDaCategoria(estado.categoriaAtiva);

    dom.activeCategoryCount.textContent = `${produtos.length} ${produtos.length === 1 ? 'produto' : 'produtos'}`;

    if (produtos.length === 0) {
        dom.productsList.innerHTML = `
            <div class="wa-product-row" style="color: var(--wa-text-secondary); font-size: 0.88rem; justify-content: center; padding: 24px 0; text-align: center;">
                ${estado.termoBusca ? 'Nenhum produto encontrado para sua busca.' : 'Nenhum produto nesta categoria no momento.'}
            </div>
        `;
        return;
    }

    produtos.forEach(item => {
        dom.productsList.appendChild(criarCardProdutoHorizontal(item));
    });
}

function obterCorSolida(categoria) {
    const cat = String(categoria || '').toUpperCase();
    if (cat.includes('REFRIGERANTE')) return '#fee2e2'; // Vermelho/guaraná suave
    if (cat.includes('AGUA') || cat.includes('ÁGUA')) return '#e0f2fe'; // Ciano/água suave
    if (cat.includes('SUCO') || cat.includes('CITRUS')) return '#fef3c7'; // Amarelo/laranja suave
    if (cat.includes('ENERGETICO')) return '#f3e8ff'; // Roxo/energético suave
    if (cat.includes('CERVEJA') || cat.includes('VINHO')) return '#fce7f3'; // Rosa suave
    return '#f1f5f9'; // Neutro
}

function obterEmojiCategoria(categoria) {
    const cat = String(categoria || '').toUpperCase();
    if (cat.includes('REFRIGERANTE')) return '🥤';
    if (cat.includes('AGUA') || cat.includes('ÁGUA')) return '💧';
    if (cat.includes('SUCO') || cat.includes('CITRUS')) return '🍊';
    if (cat.includes('ENERGETICO')) return '⚡';
    if (cat.includes('VINHO') || cat.includes('CERVEJA')) return '🍷';
    return '🍾';
}

function calcularPrecoFinal(item) {
    const base = Number(item.precoBase) || 0;
    if (!item.emPromocao || !item.valorDesconto) {
        return { precoFinal: base, precoAntigo: null, tagDesconto: '' };
    }

    let final = base;
    let tag = '';
    const desc = Number(item.valorDesconto) || 0;

    if (item.tipoDesconto === 'PERCENTUAL') {
        final = Math.max(0, base * (1 - desc / 100));
        tag = `-${desc}% OFF`;
    } else {
        final = Math.max(0, base - desc);
        tag = `-${formatarPreco(desc)}`;
    }

    return { precoFinal: final, precoAntigo: base, tagDesconto: tag };
}

function criarCardProdutoHorizontal(item) {
    const row = document.createElement('div');
    row.className = 'wa-product-row';
    row.dataset.id = String(item.produtoId);

    const motivoDestaque = Array.isArray(item.motivos) && item.motivos.length > 0 
        ? item.motivos[0] 
        : '';

    const corFundo = obterCorSolida(item.categoria);
    const emoji = obterEmojiCategoria(item.categoria);

    const noCarrinho = estado.carrinho.get(item.produtoId);
    const qtdAtual = noCarrinho ? noCarrinho.quantidade : 0;

    const { precoFinal, precoAntigo, tagDesconto } = calcularPrecoFinal(item);

    row.innerHTML = `
        <div class="wa-prod-img-box" style="background-color: ${corFundo};">
            <!-- Caminho pronto para imagem do produto: /images/produtos/${item.produtoId}.png -->
            <img src="/images/produtos/${item.produtoId}.png" 
                 alt="${escaparHtml(item.nomeProduto)}" 
                 class="wa-prod-img" 
                 onload="this.style.display='block'; if(this.nextElementSibling) this.nextElementSibling.style.display='none';"
                 onerror="this.style.display='none'; if(this.nextElementSibling) this.nextElementSibling.style.display='flex';"
                 style="display: none;">
            <div class="wa-prod-fallback" style="background-color: ${corFundo};">
                <span class="wa-prod-emoji">${emoji}</span>
            </div>
        </div>

        <div class="wa-prod-details">
            <h4 class="wa-prod-name">${escaparHtml(item.nomeProduto)}</h4>
            <span class="wa-prod-sub">${escaparHtml(item.categoria || 'Fruki')}</span>
            ${motivoDestaque ? `<span class="wa-prod-reason-badge">💡 ${escaparHtml(motivoDestaque)}</span>` : ''}
            <div class="wa-prod-price-line">
                ${precoAntigo ? `<span class="wa-price-strike">${formatarPreco(precoAntigo)}</span>` : ''}
                <span class="wa-price-bold">${formatarPreco(precoFinal)}</span>
                ${tagDesconto ? `<span class="wa-promo-tag">${tagDesconto}</span>` : ''}
            </div>
        </div>

        <div class="wa-prod-action" data-action-id="${item.produtoId}"></div>
    `;

    const actionArea = row.querySelector(`[data-action-id="${item.produtoId}"]`);
    renderizarControleQuantidade(actionArea, item, qtdAtual);

    return row;
}

function renderizarControleQuantidade(container, item, quantidade) {
    container.innerHTML = '';

    if (quantidade <= 0) {
        const btnAdd = document.createElement('button');
        btnAdd.type = 'button';
        btnAdd.className = 'wa-btn-square-add';
        btnAdd.setAttribute('aria-label', `Adicionar ${item.nomeProduto}`);
        btnAdd.textContent = '+';
        btnAdd.addEventListener('click', () => alterarQuantidade(item, 1));
        container.appendChild(btnAdd);
        return;
    }

    const stepper = document.createElement('div');
    stepper.className = 'wa-qty-stepper-compact';

    const btnMenos = document.createElement('button');
    btnMenos.type = 'button';
    btnMenos.className = 'wa-step-btn-mini';
    btnMenos.textContent = '−';
    btnMenos.setAttribute('aria-label', 'Diminuir quantidade');
    btnMenos.addEventListener('click', () => alterarQuantidade(item, quantidade - 1));

    const spanNum = document.createElement('span');
    spanNum.className = 'wa-step-num-mini';
    spanNum.textContent = String(quantidade);

    const btnMais = document.createElement('button');
    btnMais.type = 'button';
    btnMais.className = 'wa-step-btn-mini';
    btnMais.textContent = '+';
    btnMais.setAttribute('aria-label', 'Aumentar quantidade');
    btnMais.addEventListener('click', () => alterarQuantidade(item, quantidade + 1));

    stepper.append(btnMenos, spanNum, btnMais);
    container.appendChild(stepper);
}

/**
 * 7. Gerenciamento do Carrinho
 */
function alterarQuantidade(item, novaQtd) {
    if (novaQtd <= 0) {
        estado.carrinho.delete(item.produtoId);
    } else {
        estado.carrinho.set(item.produtoId, { produto: item, quantidade: novaQtd });
    }

    sincronizarTodosOsCards(item.produtoId);
    atualizarResumoCarrinho();

    // Se o modal de checkout estiver aberto, atualiza o conteúdo da etapa ativa
    if (!dom.modalCheckout.classList.contains('hidden')) {
        renderizarConteudoCheckout();
    }
}

function sincronizarTodosOsCards(produtoId) {
    const containers = document.querySelectorAll(`[data-action-id="${produtoId}"]`);
    const reg = estado.carrinho.get(produtoId);
    const qtd = reg ? reg.quantidade : 0;
    const item = reg ? reg.produto : estado.recomendacoes.find(r => r.produtoId === produtoId);

    if (!item) return;

    containers.forEach(c => renderizarControleQuantidade(c, item, qtd));
}

function calcularTotais() {
    let qtdTotal = 0;
    let valorTotal = 0;
    estado.carrinho.forEach(({ produto, quantidade }) => {
        const { precoFinal } = calcularPrecoFinal(produto);
        qtdTotal += quantidade;
        valorTotal += precoFinal * quantidade;
    });
    return { qtdTotal, valorTotal };
}

function atualizarResumoCarrinho() {
    const { qtdTotal, valorTotal } = calcularTotais();

    if (qtdTotal === 0) {
        dom.cartFloat.classList.add('hidden');
        dom.navCartBadge.classList.add('hidden');
        fecharCheckout();
        return;
    }

    dom.cartFloat.classList.remove('hidden');
    dom.navCartBadge.classList.remove('hidden');
    dom.navCartBadge.textContent = String(qtdTotal);
    dom.cartFloatQty.textContent = String(qtdTotal);
    dom.cartFloatTotal.textContent = formatarPreco(valorTotal);
}

/**
 * 8. FLUXO PÓS-COMPRA:
 *    ETAPA 1: SUGESTÕES DE VENDA ("Aproveite para levar também: - 4 produtos em cards do catálogo")
 *    ETAPA 2: CONFIRMAÇÃO DO CARRINHO (REVISÃO & ENVIAR POST /api/pedidos)
 */
function abrirCheckout(etapaInicial = 'UPSELL') {
    if (estado.carrinho.size === 0) return;
    
    // Seleciona e força a criação dos 4 cards de sugestões de venda
    estado.sugestoesCheckout = obterQuatroSugestoesDeVenda();
    if (dom.checkoutSugestoesList) {
        dom.checkoutSugestoesList.innerHTML = '';
    }
    
    navegarEtapaCheckout(etapaInicial);
    dom.modalCheckout.classList.remove('hidden');
}

function fecharCheckout() {
    dom.modalCheckout.classList.add('hidden');
}

function navegarEtapaCheckout(novaEtapa) {
    estado.etapaCheckout = novaEtapa;

    if (novaEtapa === 'UPSELL') {
        dom.indicatorStep1.className = 'wa-step-pill active';
        dom.indicatorStep1.textContent = '1. Sugestões';
        dom.indicatorStep2.className = 'wa-step-pill';
        dom.indicatorStep2.textContent = '2. Confirmação';
        dom.checkoutViewUpsell.classList.remove('hidden');
        dom.checkoutViewConfirmacao.classList.add('hidden');
        renderizarEtapaUpsell(false);
    } else {
        dom.indicatorStep1.className = 'wa-step-pill done';
        dom.indicatorStep1.textContent = '✓ 1. Sugestões';
        dom.indicatorStep2.className = 'wa-step-pill active';
        dom.indicatorStep2.textContent = '2. Confirmação';
        dom.checkoutViewUpsell.classList.add('hidden');
        dom.checkoutViewConfirmacao.classList.remove('hidden');
        renderizarEtapaConfirmacao();
    }

    const { valorTotal } = calcularTotais();
    dom.upsellPreviewTotal.textContent = formatarPreco(valorTotal);
    dom.checkoutSubtotal.textContent = formatarPreco(valorTotal);
    dom.checkoutFinalTotal.textContent = formatarPreco(valorTotal);
    dom.checkoutFeedback.classList.add('hidden');
}

function renderizarConteudoCheckout() {
    const { valorTotal } = calcularTotais();
    dom.upsellPreviewTotal.textContent = formatarPreco(valorTotal);
    dom.checkoutSubtotal.textContent = formatarPreco(valorTotal);
    dom.checkoutFinalTotal.textContent = formatarPreco(valorTotal);
    dom.checkoutFeedback.classList.add('hidden');

    if (estado.etapaCheckout === 'CONFIRMACAO') {
        renderizarEtapaConfirmacao();
    }
}

/**
 * Seleciona exatamente 4 produtos recomendados para as sugestões de venda
 */
function obterQuatroSugestoesDeVenda() {
    // 1. Prioriza produtos recomendados que ainda não foram colocados no carrinho
    const foraDoCarrinho = estado.recomendacoes.filter(item => !estado.carrinho.has(item.produtoId));

    foraDoCarrinho.sort((a, b) => {
        if (a.isCrossSell && !b.isCrossSell) return -1;
        if (!a.isCrossSell && b.isCrossSell) return 1;
        return (b.score || 0) - (a.score || 0);
    });

    let sugestoes = foraDoCarrinho.slice(0, 4);

    // Se houver menos de 4 fora do carrinho, completa com outras recomendações
    if (sugestoes.length < 4) {
        const idsJaInclusos = new Set(sugestoes.map(s => s.produtoId));
        for (const item of estado.recomendacoes) {
            if (sugestoes.length >= 4) break;
            if (!idsJaInclusos.has(item.produtoId)) {
                sugestoes.push(item);
                idsJaInclusos.add(item.produtoId);
            }
        }
    }

    return sugestoes.slice(0, 4);
}

/**
 * Renderização da Etapa 1: Sugestões de Venda
 * Mensagem: "Aproveite para levar também: (Exibe 4 produtos em card, assim como no catalogo)"
 */
function renderizarEtapaUpsell(forcarRecriacao = false) {
    if (!dom.checkoutSugestoesList) return;

    if (!estado.sugestoesCheckout || estado.sugestoesCheckout.length === 0) {
        estado.sugestoesCheckout = obterQuatroSugestoesDeVenda();
    }

    if (dom.checkoutSugestoesList.children.length > 0 && !forcarRecriacao) {
        estado.sugestoesCheckout.forEach(item => sincronizarTodosOsCards(item.produtoId));
        return;
    }

    dom.checkoutSugestoesList.innerHTML = '';

    if (estado.sugestoesCheckout.length === 0) {
        dom.checkoutSugestoesList.innerHTML = `
            <div style="font-size: 0.85rem; color: var(--wa-text-secondary); padding: 20px 0; text-align: center;">
                Nenhuma sugestão adicional no momento.
            </div>
        `;
        return;
    }

    // Exibe os 4 produtos em cards com o MESMO design do catálogo
    estado.sugestoesCheckout.forEach(item => {
        dom.checkoutSugestoesList.appendChild(criarCardProdutoHorizontal(item));
    });
}

/**
 * Renderização da Etapa 2: Confirmação do Carrinho
 */
function renderizarEtapaConfirmacao() {
    // 1. Dados do cliente e entrega
    if (estado.cliente) {
        dom.confirmClienteNome.textContent = estado.cliente.nome;
        dom.confirmClienteCnpj.textContent = `CNPJ: ${estado.cliente.cnpj || 'Cadastrado'}`;
        dom.confirmClienteCidade.textContent = `📍 ${estado.cliente.cidade || 'Região Sul'} • Entrega direta da fábrica`;
    }

    // 2. Itens detalhados com controle fino de quantidade
    dom.checkoutItensList.innerHTML = '';
    
    estado.carrinho.forEach(({ produto, quantidade }) => {
        const row = document.createElement('div');
        row.className = 'wa-item-checkout-row';
        const { precoFinal } = calcularPrecoFinal(produto);
        const subtotal = precoFinal * quantidade;

        row.innerHTML = `
            <div class="wa-item-info">
                <strong>${escaparHtml(produto.nomeProduto)}</strong>
                <span>${formatarPreco(precoFinal)} un. • Subtotal: <strong>${formatarPreco(subtotal)}</strong></span>
            </div>
            <div data-action-id="${produto.produtoId}"></div>
        `;

        const actionArea = row.querySelector(`[data-action-id="${produto.produtoId}"]`);
        renderizarControleQuantidade(actionArea, produto, quantidade);
        dom.checkoutItensList.appendChild(row);
    });
}

/**
 * 9. Envio do Pedido Real via POST /api/pedidos
 */
async function concluirPedido() {
    if (!estado.cliente || estado.carrinho.size === 0) return;

    dom.btnConcluirPedido.disabled = true;
    dom.checkoutFeedback.className = 'wa-feedback-msg';
    dom.checkoutFeedback.textContent = 'Enviando pedido para o backend...';
    dom.checkoutFeedback.classList.remove('hidden');

    const hojeIso = new Date().toISOString().split('T')[0];
    const itens = [];
    const { valorTotal } = calcularTotais();

    estado.carrinho.forEach(({ produto, quantidade }) => {
        const { precoFinal } = calcularPrecoFinal(produto);
        itens.push({
            produtoId: produto.produtoId,
            quantidade: quantidade,
            valorUnitario: precoFinal
        });
    });

    const payload = {
        clienteId: estado.cliente.id,
        dataCompra: hojeIso,
        itens: itens
    };

    try {
        const resposta = await fetch('/api/pedidos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!resposta.ok) {
            throw new Error(`Erro HTTP ${resposta.status}`);
        }

        const res = await resposta.json();
        dom.checkoutFeedback.className = 'wa-feedback-msg sucesso';
        dom.checkoutFeedback.textContent = `Pedido #${res.id} registrado com sucesso!`;

        const qtdItensRegistrados = estado.carrinho.size;
        estado.carrinho.clear();
        atualizarResumoCarrinho();

        // Atualiza recomendações e histórico do cliente após gravar o pedido no Supabase
        carregarCatalogoRecomendado(estado.cliente);

        setTimeout(() => {
            fecharCheckout();
            fecharCatalogoHover();
            adicionarMensagem('bot', `
                <p>🎉 <strong>Pedido #${res.id} confirmado com sucesso!</strong></p>
                <p style="margin-top: 4px;">Registramos <strong>${qtdItensRegistrados} produto(s)</strong> no valor total de <strong>${formatarPreco(valorTotal)}</strong> para <strong>${escaparHtml(estado.cliente.nome)}</strong>.</p>
                <p style="margin-top: 4px;">Nossa equipe de faturamento e logística já recebeu a sua solicitação. Obrigado pela preferência!</p>
            `);
        }, 1200);

    } catch (erro) {
        dom.checkoutFeedback.className = 'wa-feedback-msg erro';
        dom.checkoutFeedback.textContent = `Falha ao registrar pedido: ${erro.message}`;
    } finally {
        dom.btnConcluirPedido.disabled = false;
    }
}

/**
 * 10. Inicialização e Eventos
 */
function configurarEventos() {
    dom.timeSaudacao.textContent = obterHoraAtual();

    dom.btnEnviarCnpj.addEventListener('click', processarEnvioCnpj);
    dom.inputCnpj.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            processarEnvioCnpj();
        }
    });

    dom.btnAtalhoCatalogo.addEventListener('click', abrirCatalogoHover);
    dom.btnFecharCatalogoTopo.addEventListener('click', fecharCatalogoHover);

    // Barra de Pesquisa Retrátil
    dom.btnNavBusca.addEventListener('click', () => {
        dom.waCatalogSearchBar.classList.toggle('hidden');
        if (!dom.waCatalogSearchBar.classList.contains('hidden')) {
            dom.inputBuscaProdutos.focus();
        } else {
            estado.termoBusca = '';
            dom.inputBuscaProdutos.value = '';
            renderizarProdutosDaCategoriaAtiva();
        }
    });

    dom.inputBuscaProdutos.addEventListener('input', (e) => {
        estado.termoBusca = e.target.value.trim();
        renderizarProdutosDaCategoriaAtiva();
    });

    dom.btnLimparBusca.addEventListener('click', () => {
        dom.inputBuscaProdutos.value = '';
        estado.termoBusca = '';
        dom.waCatalogSearchBar.classList.add('hidden');
        renderizarProdutosDaCategoriaAtiva();
    });

    // Abrir carrinho pelo ícone do topo ou pelo botão flutuante abre a Etapa 1: Up-Sell
    dom.btnNavCarrinho.addEventListener('click', () => {
        if (estado.carrinho.size > 0) {
            abrirCheckout('UPSELL');
        }
    });

    dom.btnAbrirCheckout.addEventListener('click', () => {
        abrirCheckout('UPSELL');
    });

    // Navegação entre as etapas do Checkout:
    // Etapa 1 (Up-Sell) -> Etapa 2 (Confirmação)
    dom.btnAvancarConfirmacao.addEventListener('click', () => {
        navegarEtapaCheckout('CONFIRMACAO');
    });

    // Etapa 2 (Confirmação) -> Voltar para Etapa 1 (Up-Sell)
    dom.btnVoltarUpsell.addEventListener('click', () => {
        navegarEtapaCheckout('UPSELL');
    });

    // Steppers no cabeçalho do checkout clicáveis para alternância rápida
    dom.indicatorStep1.addEventListener('click', () => {
        navegarEtapaCheckout('UPSELL');
    });

    dom.indicatorStep2.addEventListener('click', () => {
        if (estado.carrinho.size > 0) {
            navegarEtapaCheckout('CONFIRMACAO');
        }
    });

    dom.btnFecharCheckout.addEventListener('click', fecharCheckout);
    dom.btnConcluirPedido.addEventListener('click', concluirPedido);

    dom.btnNavMenu.addEventListener('click', () => {
        alert(`Fruki Bebidas Oficial\nCliente: ${estado.cliente ? estado.cliente.nome : 'Não identificado'}\nCanal Digital de Autoatendimento B2B`);
    });

    // "Ver todas" seleciona a categoria MAIS
    dom.btnVerTodasCat.addEventListener('click', () => {
        selecionarCategoria('MAIS');
    });

    // Clique nas 4 categorias exatas: "Para Você", "Promoções", "Histórico", "Mais"
    dom.catCards.forEach(card => {
        card.addEventListener('click', () => {
            selecionarCategoria(card.dataset.categoria);
        });
    });
}

document.addEventListener('DOMContentLoaded', () => {
    configurarEventos();
    const params = new URLSearchParams(window.location.search);
    const cnpjParam = params.get('cnpj');
    if (cnpjParam) {
        dom.inputCnpj.value = cnpjParam.trim();
        processarEnvioCnpj();
    } else {
        dom.inputCnpj.focus();
    }
});
