package com.quipux.cadastro.pessoa;

import com.quipux.cadastro.exception.RecursoNaoEncontradoException;
import com.quipux.cadastro.exception.RegraDeNegocioException;
import com.quipux.cadastro.pessoa.dto.PessoaRequest;
import com.quipux.cadastro.pessoa.dto.PessoaResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PessoaService {
    private final PessoaRepository repository;

    public PessoaService(PessoaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PessoaResponse registrar(PessoaRequest request) {
        if (repository.existsByDocumento(request.documento())) {
            throw new RegraDeNegocioException(
                    "Ja existe uma pessoa registrada com o documento " + request.documento());
        }
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new RegraDeNegocioException(
                    "Ja existe uma pessoa registrada com o e-mail " + request.email());
        }
        Pessoa pessoa = repository.save(new Pessoa(
                request.documento(),
                request.nome().trim(),
                request.sobrenome().trim(),
                request.email().toLowerCase()));
        return PessoaResponse.de(pessoa);
    }

    @Transactional(readOnly = true)
    public List<PessoaResponse> listar(int limite) {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Pessoa::getNome, String.CASE_INSENSITIVE_ORDER))
                .limit(limite)
                .map(PessoaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public PessoaResponse buscarPorDocumento(String documento) {
        return PessoaResponse.de(buscarEntidadePorDocumento(documento));
    }

    @Transactional(readOnly = true)
    public Pessoa buscarEntidadePorDocumento(String documento) {
        return repository.findByDocumento(documento)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nenhuma pessoa encontrada com o documento " + documento));
    }

    @Transactional
    public void excluirPorDocumento(String documento) {
        Pessoa pessoa = buscarEntidadePorDocumento(documento);
        repository.delete(pessoa);
    }
}
