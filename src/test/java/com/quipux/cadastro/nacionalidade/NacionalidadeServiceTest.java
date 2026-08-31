package com.quipux.cadastro.nacionalidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.quipux.cadastro.nacionalidade.dto.NacionalidadeResponse;
import com.quipux.cadastro.nacionalidade.dto.NationalizeResponse;
import com.quipux.cadastro.pessoa.Pessoa;
import com.quipux.cadastro.pessoa.PessoaService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NacionalidadeServiceTest {
    @Mock
    private PessoaService pessoaService;

    @Mock
    private NationalizeClient client;

    @InjectMocks
    private NacionalidadeService service;

    @Test
    @DisplayName("traduz o codigo ISO devolvido pela API publica para o nome do pais")
    void traduzCodigoIsoParaNomeDoPais() {
        assertThat(NacionalidadeService.nomeDoPais("US")).isEqualTo("Estados Unidos");
        assertThat(NacionalidadeService.nomeDoPais("BR")).isEqualTo("Brasil");
        assertThat(NacionalidadeService.nomeDoPais("IT")).isEqualTo("Itália");
    }

    @Test
    @DisplayName("devolve o proprio codigo quando o ISO e desconhecido")
    void devolveCodigoQuandoIsoDesconhecido() {
        assertThat(NacionalidadeService.nomeDoPais("ZZ")).isEqualTo("ZZ");
        assertThat(NacionalidadeService.nomeDoPais("codigo-invalido")).isEqualTo("codigo-invalido");
    }

    @Test
    @DisplayName("ordena as nacionalidades pela probabilidade, da maior para a menor")
    void ordenaPelaMaiorProbabilidade() {
        when(pessoaService.buscarEntidadePorDocumento("52998224725"))
                .thenReturn(new Pessoa("52998224725", "Nathaniel", "Silva", "n@exemplo.com"));
        when(client.preverPorNome(anyString())).thenReturn(new NationalizeResponse(
                "nathaniel", 5749,
                List.of(
                        new NationalizeResponse.Pais("IE", 0.05),
                        new NationalizeResponse.Pais("US", 0.09),
                        new NationalizeResponse.Pais("GB", 0.07))));

        NacionalidadeResponse resposta = service.preverPorDocumento("52998224725");

        assertThat(resposta.nacionalidadeProvavel().codigoIso()).isEqualTo("US");
        assertThat(resposta.nacionalidadeProvavel().pais()).isEqualTo("Estados Unidos");
        assertThat(resposta.outrasPossibilidades())
                .extracting(NacionalidadeResponse.Nacionalidade::codigoIso)
                .containsExactly("GB", "IE");
        assertThat(resposta.mensagem()).isNull();
    }

    @Test
    @DisplayName("informa quando a API publica nao retorna nenhuma previsao")
    void informaAusenciaDePrevisao() {
        when(pessoaService.buscarEntidadePorDocumento("52998224725"))
                .thenReturn(new Pessoa("52998224725", "Xyzabc", "Silva", "x@exemplo.com"));
        when(client.preverPorNome(anyString()))
                .thenReturn(new NationalizeResponse("xyzabc", 0, List.of()));

        NacionalidadeResponse resposta = service.preverPorDocumento("52998224725");

        assertThat(resposta.nacionalidadeProvavel()).isNull();
        assertThat(resposta.outrasPossibilidades()).isEmpty();
        assertThat(resposta.mensagem()).contains("nao retornou previsao");
    }
}
