package com.quipux.cadastro.nacionalidade;

import com.quipux.cadastro.nacionalidade.dto.NacionalidadeResponse;
import com.quipux.cadastro.validation.Cpf;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Nacionalidade", description = "Previsao de nacionalidade a partir do nome da pessoa")
@SecurityRequirement(name = "bearerAuth")
public class NacionalidadeController {

    private final NacionalidadeService service;

    public NacionalidadeController(NacionalidadeService service) {
        this.service = service;
    }

    @GetMapping("/findNacionalityByPerson/{documento}")
    @Operation(summary = "Retorna a provavel nacionalidade da pessoa, pelo nome dela")
    public NacionalidadeResponse prever(@PathVariable @NotBlank @Cpf String documento) {
        return service.preverPorDocumento(documento);
    }
}
