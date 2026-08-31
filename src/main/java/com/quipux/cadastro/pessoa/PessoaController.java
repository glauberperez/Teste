package com.quipux.cadastro.pessoa;

import com.quipux.cadastro.pessoa.dto.PessoaRequest;
import com.quipux.cadastro.pessoa.dto.PessoaResponse;
import com.quipux.cadastro.validation.Cpf;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de cadastro de pessoas.
 *
 * <p>O parametro escolhido para identificar uma pessoa nas rotas /list/{documento}
 * e o <b>documento (CPF)</b>, por ser o identificador natural e nao sequencial.
 *
 * <p>{@code @Validated} habilita a validacao dos parametros de rota e de query.
 */
@RestController
@Validated
@Tag(name = "Pessoas", description = "Cadastro, consulta e exclusao de pessoas")
@SecurityRequirement(name = "bearerAuth")
public class PessoaController {

    private final PessoaService service;

    public PessoaController(PessoaService service) {
        this.service = service;
    }

    @PostMapping("/registrarName")
    @Operation(summary = "Registra uma pessoa (documento, nome, sobrenome e e-mail)")
    public ResponseEntity<PessoaResponse> registrar(@Valid @RequestBody PessoaRequest request) {
        PessoaResponse criada = service.registrar(request);
        return ResponseEntity.created(URI.create("/list/" + criada.documento())).body(criada);
    }

    @GetMapping("/list")
    @Operation(summary = "Lista as pessoas registradas")
    public List<PessoaResponse> listar(
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "limite deve ser no minimo 1")
            @Max(value = 200, message = "limite deve ser no maximo 200")
            int limite) {
        return service.listar(limite);
    }

    @GetMapping("/list/{documento}")
    @Operation(summary = "Obtem os dados de uma pessoa pelo documento")
    public PessoaResponse buscar(
            @PathVariable @NotBlank @Cpf String documento) {
        return service.buscarPorDocumento(documento);
    }

    @DeleteMapping("/list/{documento}")
    @Operation(summary = "Exclui uma pessoa pelo documento")
    public ResponseEntity<Void> excluir(
            @PathVariable @NotBlank @Cpf String documento) {
        service.excluirPorDocumento(documento);
        return ResponseEntity.noContent().build();
    }
}
