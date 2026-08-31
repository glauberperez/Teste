package com.quipux.cadastro.pessoa;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quipux.cadastro.nacionalidade.NationalizeClient;
import com.quipux.cadastro.nacionalidade.dto.NationalizeResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de ponta a ponta dos endpoints exigidos pela prova, passando pela
 * cadeia real do Spring Security.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PessoaApiIntegrationTest {

    private static final String CPF = "52998224725";
    private static final String CPF_OUTRO = "11144477735";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PessoaRepository repository;

    /** A API externa nao e chamada de verdade nos testes. */
    @MockitoBean
    private NationalizeClient nationalizeClient;

    private String token;

    @BeforeEach
    void preparar() throws Exception {
        repository.deleteAll();
        token = autenticar("admin", "admin123");
    }

    private String autenticar(String usuario, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("usuario", usuario, "senha", senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("token").asText();
    }

    private String json(String... paresChaveValor) throws Exception {
        var mapa = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i < paresChaveValor.length; i += 2) {
            mapa.put(paresChaveValor[i], paresChaveValor[i + 1]);
        }
        return objectMapper.writeValueAsString(mapa);
    }

    private String pessoaJson(String documento, String nome, String sobrenome, String email) throws Exception {
        return json("documento", documento, "nome", nome, "sobrenome", sobrenome, "email", email);
    }

    private void registrar(String documento, String nome, String sobrenome, String email) throws Exception {
        mockMvc.perform(post("/registrarName")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pessoaJson(documento, nome, sobrenome, email)))
                .andExpect(status().isCreated());
    }

    /* ---------- autenticacao ---------- */

    @Test
    @DisplayName("sem token, as APIs respondem 401")
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Nao autenticado"));

        mockMvc.perform(post("/registrarName")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pessoaJson(CPF, "Nathaniel", "Silva", "n@exemplo.com")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token invalido tambem responde 401")
    void tokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/list").header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login com senha errada responde 401")
    void loginComSenhaErradaRetorna401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("usuario", "admin", "senha", "senha-errada")))
                .andExpect(status().isUnauthorized());
    }

    /* ---------- POST /registrarName ---------- */

    @Test
    @DisplayName("POST /registrarName cria a pessoa e devolve 201")
    void registrarPessoaValida() throws Exception {
        mockMvc.perform(post("/registrarName")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pessoaJson(CPF, "Nathaniel", "Silva", "Nathaniel.Silva@Exemplo.com")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/list/" + CPF))
                .andExpect(jsonPath("$.documento").value(CPF))
                .andExpect(jsonPath("$.nome").value("Nathaniel"))
                .andExpect(jsonPath("$.email").value("nathaniel.silva@exemplo.com"));
    }

    @Test
    @DisplayName("POST /registrarName recusa CPF com digito verificador errado")
    void registrarComCpfInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/registrarName")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pessoaJson("52998224726", "Nathaniel", "Silva", "n@exemplo.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.documento").exists());
    }

    @Test
    @DisplayName("POST /registrarName recusa e-mail fora do formato e campos vazios")
    void registrarComCamposInvalidosRetorna400() throws Exception {
        mockMvc.perform(post("/registrarName")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pessoaJson(CPF, "", "Silva", "isso-nao-e-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.nome").exists());
    }

    @Test
    @DisplayName("POST /registrarName recusa documento ja cadastrado com 409")
    void registrarDuplicadoRetorna409() throws Exception {
        registrar(CPF, "Nathaniel", "Silva", "n@exemplo.com");

        mockMvc.perform(post("/registrarName")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pessoaJson(CPF, "Outro", "Nome", "outro@exemplo.com")))
                .andExpect(status().isConflict());
    }

    /* ---------- GET /list ---------- */

    @Test
    @DisplayName("GET /list devolve as pessoas registradas")
    void listarPessoas() throws Exception {
        registrar(CPF, "Nathaniel", "Silva", "n@exemplo.com");
        registrar(CPF_OUTRO, "Giovanni", "Rossi", "g@exemplo.com");

        mockMvc.perform(get("/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nome").value("Giovanni"));
    }

    @Test
    @DisplayName("GET /list valida o parametro limite")
    void listarComLimiteInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/list").param("limite", "0").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.limite").exists());
    }

    /* ---------- GET e DELETE /list/{documento} ---------- */

    @Test
    @DisplayName("GET /list/{documento} devolve a pessoa")
    void buscarPorDocumento() throws Exception {
        registrar(CPF, "Nathaniel", "Silva", "n@exemplo.com");

        mockMvc.perform(get("/list/" + CPF).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sobrenome").value("Silva"));
    }

    @Test
    @DisplayName("GET /list/{documento} valida o formato do documento")
    void buscarComDocumentoInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/list/123").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.documento").exists());
    }

    @Test
    @DisplayName("GET /list/{documento} devolve 404 quando nao existe")
    void buscarInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/list/" + CPF).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /list/{documento} remove a pessoa")
    void excluirPessoa() throws Exception {
        registrar(CPF, "Nathaniel", "Silva", "n@exemplo.com");

        mockMvc.perform(delete("/list/" + CPF).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/list/" + CPF).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /list/{documento} devolve 404 quando nao existe")
    void excluirInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/list/" + CPF).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    /* ---------- GET /findNacionalityByPerson/{documento} ---------- */

    @Test
    @DisplayName("GET /findNacionalityByPerson devolve o nome do pais, nao o codigo ISO")
    void preverNacionalidade() throws Exception {
        registrar(CPF, "Nathaniel", "Silva", "n@exemplo.com");

        Mockito.when(nationalizeClient.preverPorNome(ArgumentMatchers.anyString()))
                .thenReturn(new NationalizeResponse("nathaniel", 5749,
                        List.of(new NationalizeResponse.Pais("US", 0.09),
                                new NationalizeResponse.Pais("IE", 0.05))));

        mockMvc.perform(get("/findNacionalityByPerson/" + CPF)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nathaniel"))
                .andExpect(jsonPath("$.nacionalidadeProvavel.codigoIso").value("US"))
                .andExpect(jsonPath("$.nacionalidadeProvavel.pais").value("Estados Unidos"))
                .andExpect(jsonPath("$.outrasPossibilidades", hasSize(1)));
    }

    @Test
    @DisplayName("GET /findNacionalityByPerson devolve 404 para pessoa nao registrada")
    void preverNacionalidadeDePessoaInexistente() throws Exception {
        mockMvc.perform(get("/findNacionalityByPerson/" + CPF)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
