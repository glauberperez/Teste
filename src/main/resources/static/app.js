/*
 * Interface web que consome as APIs do projeto.
 * Sem framework nem build: apenas fetch + DOM.
 */

const CHAVE_TOKEN = 'cadastro-pessoas.token';
const CHAVE_USUARIO = 'cadastro-pessoas.usuario';

const el = (id) => document.getElementById(id);

const estado = {
    get token() {
        return sessionStorage.getItem(CHAVE_TOKEN);
    },
    set token(valor) {
        if (valor) {
            sessionStorage.setItem(CHAVE_TOKEN, valor);
        } else {
            sessionStorage.removeItem(CHAVE_TOKEN);
        }
    },
    get usuario() {
        return sessionStorage.getItem(CHAVE_USUARIO);
    },
    set usuario(valor) {
        if (valor) {
            sessionStorage.setItem(CHAVE_USUARIO, valor);
        } else {
            sessionStorage.removeItem(CHAVE_USUARIO);
        }
    }
};

/* ---------- utilidades ---------- */

let timerAlerta;

function avisar(mensagem, tipo = 'sucesso') {
    const alerta = el('alerta');
    alerta.textContent = mensagem;
    alerta.className = `alerta ${tipo}`;
    clearTimeout(timerAlerta);
    timerAlerta = setTimeout(() => alerta.classList.add('oculto'), 5000);
}

/** Transforma o corpo de erro padronizado da API em uma mensagem legivel. */
function mensagemDeErro(corpo, status) {
    if (!corpo) {
        return `Erro ${status}`;
    }
    if (corpo.campos) {
        return Object.entries(corpo.campos)
            .map(([campo, texto]) => `${campo}: ${texto}`)
            .join('\n');
    }
    return corpo.mensagem || corpo.erro || `Erro ${status}`;
}

async function chamarApi(caminho, opcoes = {}) {
    const cabecalhos = { ...(opcoes.headers || {}) };
    if (opcoes.body) {
        cabecalhos['Content-Type'] = 'application/json';
    }
    if (estado.token) {
        cabecalhos['Authorization'] = `Bearer ${estado.token}`;
    }

    const resposta = await fetch(caminho, { ...opcoes, headers: cabecalhos });

    if (resposta.status === 401 && estado.token) {
        encerrarSessao();
        throw new Error('Sessao expirada. Autentique-se novamente.');
    }

    const texto = await resposta.text();
    const corpo = texto ? JSON.parse(texto) : null;

    if (!resposta.ok) {
        throw new Error(mensagemDeErro(corpo, resposta.status));
    }
    return corpo;
}

/* ---------- sessao ---------- */

function mostrarAreaLogada(logado) {
    el('secaoLogin').classList.toggle('oculto', logado);
    el('secaoCadastro').classList.toggle('oculto', !logado);
    el('secaoLista').classList.toggle('oculto', !logado);
    el('sessao').classList.toggle('oculto', !logado);
    if (!logado) {
        el('secaoNacionalidade').classList.add('oculto');
    }
}

function encerrarSessao() {
    estado.token = null;
    estado.usuario = null;
    mostrarAreaLogada(false);
}

el('formLogin').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
        const dados = await chamarApi('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ usuario: el('usuario').value, senha: el('senha').value })
        });
        estado.token = dados.token;
        estado.usuario = el('usuario').value;
        el('usuarioLogado').textContent = `Conectado como ${estado.usuario}`;
        mostrarAreaLogada(true);
        avisar('Autenticado com sucesso.');
        await carregarPessoas();
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
});

el('btnSair').addEventListener('click', () => {
    encerrarSessao();
    avisar('Sessao encerrada.');
});

/* ---------- cadastro ---------- */

el('formPessoa').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const pessoa = {
        documento: el('documento').value.trim(),
        nome: el('nome').value.trim(),
        sobrenome: el('sobrenome').value.trim(),
        email: el('email').value.trim()
    };
    try {
        await chamarApi('/registrarName', { method: 'POST', body: JSON.stringify(pessoa) });
        el('formPessoa').reset();
        avisar(`${pessoa.nome} registrado(a) com sucesso.`);
        await carregarPessoas();
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
});

/* ---------- listagem ---------- */

el('btnAtualizar').addEventListener('click', () => carregarPessoas());

async function carregarPessoas() {
    try {
        const pessoas = await chamarApi('/list');
        const corpo = el('corpoTabela');
        corpo.replaceChildren();
        el('listaVazia').classList.toggle('oculto', pessoas.length > 0);

        pessoas.forEach((pessoa) => corpo.appendChild(criarLinha(pessoa)));
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

function criarLinha(pessoa) {
    const linha = document.createElement('tr');

    linha.appendChild(celula(pessoa.documento));
    linha.appendChild(celula(`${pessoa.nome} ${pessoa.sobrenome}`));
    linha.appendChild(celula(pessoa.email));

    const acoes = document.createElement('div');
    acoes.className = 'acoes';

    const botaoNacionalidade = document.createElement('button');
    botaoNacionalidade.className = 'secundario';
    botaoNacionalidade.textContent = 'Nacionalidade';
    botaoNacionalidade.addEventListener('click', () => consultarNacionalidade(pessoa.documento));

    const botaoExcluir = document.createElement('button');
    botaoExcluir.className = 'perigo';
    botaoExcluir.textContent = 'Excluir';
    botaoExcluir.addEventListener('click', () => excluir(pessoa));

    acoes.append(botaoNacionalidade, botaoExcluir);

    const celulaAcoes = document.createElement('td');
    celulaAcoes.appendChild(acoes);
    linha.appendChild(celulaAcoes);

    return linha;
}

function celula(texto) {
    const td = document.createElement('td');
    td.textContent = texto;
    return td;
}

async function excluir(pessoa) {
    try {
        await chamarApi(`/list/${pessoa.documento}`, { method: 'DELETE' });
        avisar(`${pessoa.nome} excluido(a).`);
        el('secaoNacionalidade').classList.add('oculto');
        await carregarPessoas();
    } catch (erro) {
        avisar(erro.message, 'erro');
    }
}

/* ---------- nacionalidade ---------- */

async function consultarNacionalidade(documento) {
    const secao = el('secaoNacionalidade');
    const destino = el('resultadoNacionalidade');
    destino.replaceChildren();
    secao.classList.remove('oculto');

    const carregando = document.createElement('p');
    carregando.className = 'ajuda';
    carregando.textContent = 'Consultando a API publica...';
    destino.appendChild(carregando);

    try {
        const previsao = await chamarApi(`/findNacionalityByPerson/${documento}`);
        destino.replaceChildren();

        const titulo = document.createElement('p');
        titulo.className = 'ajuda';
        titulo.textContent = `Previsao para o nome ${previsao.nome}`;
        destino.appendChild(titulo);

        if (!previsao.nacionalidadeProvavel) {
            const vazio = document.createElement('p');
            vazio.textContent = previsao.mensagem || 'Sem previsao disponivel.';
            destino.appendChild(vazio);
            return;
        }

        const todas = [previsao.nacionalidadeProvavel, ...(previsao.outrasPossibilidades || [])];
        todas.forEach((item, indice) => destino.appendChild(criarBarra(item, indice === 0)));
    } catch (erro) {
        destino.replaceChildren();
        avisar(erro.message, 'erro');
        secao.classList.add('oculto');
    }
}

function criarBarra(nacionalidade, destaque) {
    const bloco = document.createElement('div');

    const percentual = Math.round((nacionalidade.probabilidade || 0) * 100);
    const prefixo = destaque ? 'Mais provavel: ' : '';

    const rotulo = document.createElement('p');
    rotulo.className = 'bandeira';
    rotulo.textContent = `${prefixo}${nacionalidade.pais} (${nacionalidade.codigoIso}) - ${percentual}%`;
    if (destaque) {
        rotulo.style.fontWeight = '600';
    }

    const barra = document.createElement('div');
    barra.className = 'barra';
    const preenchimento = document.createElement('span');
    preenchimento.style.width = `${Math.max(percentual, 2)}%`;
    barra.appendChild(preenchimento);

    bloco.append(rotulo, barra);
    return bloco;
}

/* ---------- inicializacao ---------- */

if (estado.token) {
    el('usuarioLogado').textContent = `Conectado como ${estado.usuario}`;
    mostrarAreaLogada(true);
    carregarPessoas();
}
