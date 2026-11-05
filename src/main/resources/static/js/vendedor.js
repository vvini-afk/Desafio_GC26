const COD_REPRESENTANTE_PADRAO = 31906;

const estadoVendedor = {
    vendedor: null,
    clientes: [],
    clienteAtual: null,
    diagnosticoAtual: null,
    insightsAtuais: [],
    climaAtual: null
};

const dom = {
    btnMenuHamburguer: document.getElementById('btn-menu-hamburguer'),
    toast: document.getElementById('fv-toast'),
    sellerAvatarLetters: document.getElementById('seller-avatar-letters'),
    sellerName: document.getElementById('seller-name'),
    sellerRole: document.getElementById('seller-role'),
    sellerRegion: document.getElementById('seller-region'),
    kpiClientesCarteira: document.getElementById('kpi-clientes-carteira'),
    kpiClientesQuebra: document.getElementById('kpi-clientes-quebra'),
    kpiTotalCidades: document.getElementById('kpi-total-cidades'),
    totalClientesBadge: document.getElementById('total-clientes-badge'),
    selectRepresentante: document.getElementById('vendedor-select-representante'),
    selectCliente: document.getElementById('vendedor-select-cliente'),
    painelInsightsCliente: document.getElementById('painel-insights-cliente'),
    clienteNome: document.getElementById('fv-cliente-nome'),
    clienteCnpj: document.getElementById('fv-cliente-cnpj'),
    clientePerfil: document.getElementById('fv-cliente-perfil'),
    clienteCidadeBadge: document.getElementById('fv-cliente-cidade-badge'),
    quebraAlert: document.getElementById('fv-quebra-alert'),
    clienteUltimaCompra: document.getElementById('fv-cliente-ultima-compra'),
    clienteClima: document.getElementById('fv-cliente-clima'),
    diagnosticoBar: document.getElementById('fv-diagnostico-bar'),
    pitchText: document.getElementById('fv-pitch-text'),
    btnCopiarPitch: document.getElementById('btn-copiar-pitch'),
    btnAbrirWhatsappCliente: document.getElementById('btn-abrir-whatsapp-cliente'),
    totalInsights: document.getElementById('fv-total-insights'),
    listaInsights: document.getElementById('lista-insights'),
    estadoMensagem: document.getElementById('fv-estado-mensagem')
};

const formatadorMoeda = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
});

function formatarMoeda(valor) {
    if (valor === null || valor === undefined || Number.isNaN(Number(valor))) {
        return 'Sob consulta';
    }
    return formatadorMoeda.format(Number(valor));
}

function escaparHtml(texto) {
    const div = document.createElement('div');
    div.textContent = texto ?? '';
    return div.innerHTML;
}

function obterIniciais(nome) {
    if (!nome) return 'FV';
    const partes = String(nome).trim().split(/\s+/);
    if (partes.length === 1) return partes[0].slice(0, 2).toUpperCase();
    return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
}

let toastTimeout = null;

function exibirToast(mensagem) {
    if (toastTimeout) clearTimeout(toastTimeout);
    dom.toast.textContent = mensagem;
    dom.toast.classList.remove('hidden');
    toastTimeout = setTimeout(() => {
        dom.toast.classList.add('hidden');
    }, 2400);
}

async function carregarPainelVendedor(codRepresentante) {
    try {
        const resposta = await fetch(`/api/clientes/vendedor/${encodeURIComponent(codRepresentante)}`);
        if (!resposta.ok) {
            throw new Error(`Status ${resposta.status}`);
        }

        const painel = await resposta.json();
        estadoVendedor.vendedor = painel;
        estadoVendedor.clientes = Array.isArray(painel.clientes) ? painel.clientes : [];

        dom.sellerAvatarLetters.textContent = obterIniciais(painel.nomeVendedor);
        dom.sellerName.textContent = painel.nomeVendedor || 'Representante';
        dom.sellerRole.textContent = `Representante Comercial • Cód. ${painel.codRepresentante}`;
        dom.sellerRegion.textContent = `📍 Atuação: ${painel.ufsAtendidas || 'RS'} (${painel.totalCidades || 0} cidades)`;

        dom.kpiClientesCarteira.textContent = String(painel.totalClientes ?? estadoVendedor.clientes.length);
        dom.kpiClientesQuebra.textContent = String(painel.clientesEmQuebra ?? 0);
        dom.kpiTotalCidades.textContent = String(painel.totalCidades ?? 0);

        popularSeletorClientes(estadoVendedor.clientes);

    } catch (erro) {
        dom.totalClientesBadge.textContent = 'Erro ao carregar';
        dom.selectCliente.innerHTML = `<option value="">Falha ao consultar carteira (${escaparHtml(erro.message)})</option>`;
    }
}

async function carregarRepresentantes() {
    try {
        const resposta = await fetch('/api/clientes/vendedores');
        if (!resposta.ok) throw new Error(`Status ${resposta.status}`);
        const representantes = await resposta.json();
        dom.selectRepresentante.innerHTML = '<option value="">Selecione um representante...</option>';

        representantes.forEach(representante => {
            const opcao = document.createElement('option');
            opcao.value = String(representante.codRepresentante);
            opcao.textContent = `${representante.nomeVendedor} • Cód. ${representante.codRepresentante}`;
            dom.selectRepresentante.appendChild(opcao);
        });

        const params = new URLSearchParams(window.location.search);
        const solicitado = params.get('codRepresentante') || localStorage.getItem('codRepresentante');
        const existeSolicitado = representantes.some(rep => String(rep.codRepresentante) === String(solicitado));
        const existePadrao = representantes.some(rep => rep.codRepresentante === COD_REPRESENTANTE_PADRAO);
        const codigo = existeSolicitado ? solicitado
            : existePadrao ? String(COD_REPRESENTANTE_PADRAO)
                : representantes.length ? String(representantes[0].codRepresentante) : null;

        if (!codigo) throw new Error('Nenhum representante foi encontrado no banco.');
        dom.selectRepresentante.value = codigo;
        await carregarPainelVendedor(codigo);
    } catch (erro) {
        dom.totalClientesBadge.textContent = 'Erro ao carregar';
        dom.selectRepresentante.innerHTML = `<option value="">Falha ao consultar representantes (${escaparHtml(erro.message)})</option>`;
        dom.selectCliente.innerHTML = '<option value="">Não foi possível carregar a carteira</option>';
    }
}

function popularSeletorClientes(clientes) {
    dom.selectCliente.innerHTML = '<option value="">Selecione um cliente da carteira...</option>';
    dom.totalClientesBadge.textContent = `${clientes.length} clientes`;

    clientes.forEach(c => {
        const opt = document.createElement('option');
        opt.value = String(c.id);
        const cidadeUf = c.cidade ? ` (${c.cidade}${c.uf ? '/' + c.uf : ''})` : '';
        const flagQuebra = c.emQuebraConsumo ? `⚠️ [${c.gravidadeQuebra || 'QUEBRA'}] ` : '';
        opt.textContent = `${flagQuebra}${c.nome}${cidadeUf}`;
        dom.selectCliente.appendChild(opt);
    });

    if (clientes.length > 0) {
        const primeiroCliente = clientes[0];
        dom.selectCliente.value = String(primeiroCliente.id);
        carregarInsightsCliente(primeiroCliente.id);
    }
}

async function carregarContextoClima(cliente) {
    const cidade = cliente?.cidade;
    if (!cidade && !cliente?.cep) {
        dom.clienteClima.textContent = 'Cidade não informada';
        estadoVendedor.climaAtual = null;
        return;
    }

    dom.clienteClima.textContent = 'Consultando Open-Meteo...';
    try {
        const params = new URLSearchParams();
        if (cidade) params.set('cidade', cidade);
        if (cliente.uf) params.set('uf', cliente.uf);
        if (cliente.cep) params.set('cep', cliente.cep);
        const res = await fetch(`/api/contexto/clima?${params.toString()}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const clima = await res.json();
        estadoVendedor.climaAtual = clima;

        const temp = clima.temperaturaCelsius !== null && clima.temperaturaCelsius !== undefined
            ? `${Number(clima.temperaturaCelsius).toFixed(1)}°C`
            : '—';
        const umid = clima.umidadeRelativaPercentual !== null && clima.umidadeRelativaPercentual !== undefined
            ? `💧 ${clima.umidadeRelativaPercentual}%`
            : '';
        const chuva = Number(clima.precipitacaoMilimetros) > 0
            ? ` • 🌧️ ${Number(clima.precipitacaoMilimetros).toFixed(1)}mm`
            : '';

        dom.clienteClima.textContent = `🌡️ ${temp} ${umid}${chuva}`.trim();
    } catch {
        dom.clienteClima.textContent = `📍 ${cidade}`;
        estadoVendedor.climaAtual = null;
    }
}

async function carregarInsightsCliente(clienteId) {
    if (!clienteId) {
        dom.painelInsightsCliente.classList.add('hidden');
        dom.estadoMensagem.classList.remove('hidden');
        return;
    }

    const cliente = estadoVendedor.clientes.find(c => String(c.id) === String(clienteId));
    if (!cliente) return;

    estadoVendedor.clienteAtual = cliente;

    dom.clienteNome.textContent = cliente.nome;
    dom.clienteCnpj.textContent = `CNPJ: ${cliente.cnpj || 'Não informado'}`;
    const subseg = cliente.subsegmento ? ` • ${cliente.subsegmento}` : '';
    dom.clientePerfil.textContent = `${cliente.perfil || 'Comércio'}${subseg}`;
    const ufTexto = cliente.uf ? `/${cliente.uf}` : '';
    dom.clienteCidadeBadge.textContent = cliente.cidade ? `📍 ${cliente.cidade}${ufTexto}` : '📍 Sul';
    dom.clienteUltimaCompra.textContent = cliente.ultimaCompra ? cliente.ultimaCompra : 'Sem registro';

    if (dom.btnAbrirWhatsappCliente) {
        dom.btnAbrirWhatsappCliente.href = cliente.cnpj
            ? `/index.html?cnpj=${encodeURIComponent(cliente.cnpj)}`
            : '/index.html';
    }

    if (cliente.emQuebraConsumo) {
        const dias = cliente.diasSemComprar !== null && cliente.diasSemComprar !== undefined
            ? Math.round(Number(cliente.diasSemComprar))
            : '—';
        const intervalo = cliente.intervaloMedioDias !== null && cliente.intervaloMedioDias !== undefined
            ? Math.round(Number(cliente.intervaloMedioDias))
            : '—';
        const pedidos = cliente.totalPedidos ?? '—';
        dom.quebraAlert.innerHTML = `
            ⚠️ <strong>Alerta de Quebra de Consumo (${escaparHtml(cliente.gravidadeQuebra || 'ATIVA')}):</strong>
            Cliente está há <strong>${dias} dias sem comprar</strong> (intervalo médio histórico: ${intervalo} dias em ${pedidos} pedidos).
        `;
        dom.quebraAlert.classList.remove('hidden');
    } else {
        dom.quebraAlert.classList.add('hidden');
        dom.quebraAlert.innerHTML = '';
    }

    dom.painelInsightsCliente.classList.remove('hidden');
    dom.estadoMensagem.classList.add('hidden');

    dom.listaInsights.innerHTML = '<div style="font-size: 0.82rem; color: var(--fv-text-muted); padding: 12px 0;">Consultando motor de recomendação e contexto...</div>';
    dom.totalInsights.textContent = 'Calculando...';
    dom.diagnosticoBar.classList.add('hidden');

    const promessaClima = carregarContextoClima(cliente);

    try {
        const params = new URLSearchParams();
        if (cliente.cidade) params.set('cidade', cliente.cidade);
        if (cliente.uf) params.set('uf', cliente.uf);
        if (cliente.cep) params.set('cep', cliente.cep);
        params.set('limite', '30');

        const resposta = await fetch(`/api/recomendacoes/${encodeURIComponent(cliente.id)}/diagnostico?${params.toString()}`);
        if (!resposta.ok) throw new Error(`Status ${resposta.status}`);

        const diagnostico = await resposta.json();
        await promessaClima;

        estadoVendedor.diagnosticoAtual = diagnostico;
        const todasRecomendacoes = Array.isArray(diagnostico.recomendacoes) ? diagnostico.recomendacoes : [];
        estadoVendedor.insightsAtuais = todasRecomendacoes.slice(0, 15);

        renderizarBarraDiagnostico(diagnostico);
        renderizarInsights(estadoVendedor.insightsAtuais, cliente, diagnostico);

    } catch (erro) {
        await promessaClima;
        dom.listaInsights.innerHTML = `
            <div style="font-size: 0.82rem; color: #dc2626; padding: 12px 0;">
                Erro ao calcular insights: ${escaparHtml(erro.message)}
            </div>
        `;
        dom.totalInsights.textContent = '0 produtos';
    }
}

function renderizarBarraDiagnostico(diagnostico) {
    if (!diagnostico || !dom.diagnosticoBar) return;

    const regiao = diagnostico.regiao || 'Sul';
    const mult = diagnostico.multiplicadorAplicado !== null && diagnostico.multiplicadorAplicado !== undefined
        ? `x${Number(diagnostico.multiplicadorAplicado).toFixed(2)}`
        : 'x1.00';

    dom.diagnosticoBar.innerHTML = `
        <span class="fv-diag-chip highlight">Região: ${escaparHtml(regiao)} (${escaparHtml(mult)})</span>
        <span class="fv-diag-chip">🔄 Recompras: ${diagnostico.recomprasDiretas ?? 0}</span>
        <span class="fv-diag-chip">✨ Cross-Sell: ${diagnostico.crossSellsIneditos ?? 0}</span>
    `;
    dom.diagnosticoBar.classList.remove('hidden');
}

function renderizarInsights(produtos, cliente, diagnostico) {
    dom.totalInsights.textContent = `${produtos.length} ${produtos.length === 1 ? 'produto' : 'produtos'}`;
    dom.listaInsights.innerHTML = '';

    if (produtos.length === 0) {
        dom.listaInsights.innerHTML = `
            <div style="font-size: 0.85rem; color: var(--fv-text-secondary); padding: 16px 0; text-align: center;">
                Nenhuma recomendação gerada para este cliente no momento.
            </div>
        `;
        dom.pitchText.textContent = `Cliente ${cliente.nome} sem recomendações pendentes no momento.`;
        return;
    }

    const principalItem = produtos[0];
    const motivoDestaque = principalItem.motivos && principalItem.motivos.length > 0
        ? principalItem.motivos[0]
        : 'alta aderência ao perfil';
    const cidadeStr = cliente.cidade ? ` em ${cliente.cidade}` : '';
    const contextoQuebra = cliente.emQuebraConsumo && cliente.diasSemComprar
        ? ` Notei que faz <strong>${Math.round(Number(cliente.diasSemComprar))} dias</strong> desde o seu último pedido (ciclo médio de ${Math.round(Number(cliente.intervaloMedioDias || 0))} dias).`
        : '';

    dom.pitchText.innerHTML = `
        "Olá, pessoal da <strong>${escaparHtml(cliente.nome)}</strong>! Estou acompanhando a sua rota${escaparHtml(cidadeStr)}.${contextoQuebra} Preparei uma sugestão prioritária de <strong>${escaparHtml(principalItem.nomeProduto)}</strong> (<em>${escaparHtml(motivoDestaque)}</em>) por <strong>${formatarMoeda(principalItem.precoBase)}</strong> para reposição hoje."
    `;

    produtos.forEach(item => {
        const card = document.createElement('div');
        card.className = 'fv-insight-card';

        const motivosHtml = Array.isArray(item.motivos) && item.motivos.length > 0
            ? item.motivos.map(m => `<span class="fv-motivo-item">${escaparHtml(m)}</span>`).join('')
            : '<span class="fv-motivo-item">Recomendação calculada pelo motor</span>';

        const precoFormatado = formatarMoeda(item.precoBase);
        const precoMinimoStr = item.precoMinimo ? ` • Mín. negociação: ${formatarMoeda(item.precoMinimo)}` : '';

        card.innerHTML = `
            <div class="fv-card-top-row">
                <div class="fv-prod-info">
                    <h5 class="fv-prod-name">${escaparHtml(item.nomeProduto)}</h5>
                    <span class="fv-prod-cat">${escaparHtml(item.categoria || 'Linha Fruki')}</span>
                </div>
                <span class="fv-score-pill">Score ${Number(item.score).toFixed(1)}</span>
            </div>

            <div class="fv-price-line">
                <span>Preço Base: ${precoFormatado}${precoMinimoStr}</span>
            </div>

            <div class="fv-motivos-list">
                ${motivosHtml}
            </div>
        `;

        dom.listaInsights.appendChild(card);
    });
}

function configurarEventos() {
    dom.btnMenuHamburguer.addEventListener('click', () => {
        exibirToast('Menu lateral (demonstração)');
    });

    dom.selectCliente.addEventListener('change', (e) => {
        carregarInsightsCliente(e.target.value);
    });

    dom.selectRepresentante.addEventListener('change', (e) => {
        const codigo = e.target.value;
        if (!codigo) return;
        localStorage.setItem('codRepresentante', codigo);
        carregarPainelVendedor(codigo);
    });

    dom.btnCopiarPitch.addEventListener('click', () => {
        const texto = dom.pitchText.innerText || dom.pitchText.textContent;
        navigator.clipboard.writeText(texto).then(() => {
            exibirToast('Argumento copiado para a área de transferência!');
        }).catch(() => {
            exibirToast('Argumento pronto para uso!');
        });
    });
}

document.addEventListener('DOMContentLoaded', () => {
    configurarEventos();
    carregarRepresentantes();
});
