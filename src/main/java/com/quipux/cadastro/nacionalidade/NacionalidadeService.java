package com.quipux.cadastro.nacionalidade;

import com.quipux.cadastro.nacionalidade.dto.NacionalidadeResponse;
import com.quipux.cadastro.nacionalidade.dto.NacionalidadeResponse.Nacionalidade;
import com.quipux.cadastro.nacionalidade.dto.NationalizeResponse;
import com.quipux.cadastro.pessoa.Pessoa;
import com.quipux.cadastro.pessoa.PessoaService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class NacionalidadeService {

    private static final Locale IDIOMA_EXIBICAO = Locale.forLanguageTag("pt-BR");

    /** Codigos ISO 3166-1 alpha-2 conhecidos pelo JDK. */
    private static final Set<String> PAISES_ISO = Set.of(Locale.getISOCountries());

    private final PessoaService pessoaService;
    private final NationalizeClient client;

    public NacionalidadeService(PessoaService pessoaService, NationalizeClient client) {
        this.pessoaService = pessoaService;
        this.client = client;
    }

    public NacionalidadeResponse preverPorDocumento(String documento) {
        Pessoa pessoa = pessoaService.buscarEntidadePorDocumento(documento);

        NationalizeResponse previsao = client.preverPorNome(pessoa.getNome());

        List<Nacionalidade> nacionalidades = previsao.paisesOuVazio().stream()
                .filter(pais -> pais.countryId() != null)
                .sorted(Comparator.comparing(
                        NationalizeResponse.Pais::probability,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(pais -> new Nacionalidade(
                        pais.countryId(),
                        nomeDoPais(pais.countryId()),
                        pais.probability()))
                .toList();

        if (nacionalidades.isEmpty()) {
            return NacionalidadeResponse.semPrevisao(documento, pessoa.getNome());
        }

        return new NacionalidadeResponse(
                documento,
                pessoa.getNome(),
                nacionalidades.getFirst(),
                nacionalidades.subList(1, nacionalidades.size()),
                null);
    }

    /**
     * Converte o codigo ISO 3166-1 alpha-2 devolvido pela API publica no nome do
     * pais, usando a base do proprio JDK (sem tabela hardcoded).
     *
     * <p>Se o codigo nao for um ISO conhecido, devolve o proprio codigo: e mais
     * util para quem consome a API do que um "Regiao desconhecida" generico.
     */
    static String nomeDoPais(String codigoIso) {
        if (codigoIso == null || codigoIso.isBlank()) {
            return codigoIso;
        }
        String normalizado = codigoIso.toUpperCase(Locale.ROOT);
        if (!PAISES_ISO.contains(normalizado)) {
            return codigoIso;
        }
        String nome = new Locale.Builder()
                .setRegion(normalizado)
                .build()
                .getDisplayCountry(IDIOMA_EXIBICAO);
        return (nome == null || nome.isBlank()) ? codigoIso : nome;
    }
}
